package com.poplavok.forms;

import com.poplavok.data.bot.BotExchange;
import com.poplavok.forms.SellDownBotState.EventType;
import com.poplavok.forms.SellDownBotState.Level;
import com.poplavok.forms.SellDownBotState.Status;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellDownBotTest {
    static class FakeExchange implements BotExchange {
        final Map<String, Order> orders = new LinkedHashMap<>();

        @Override
        public String exchangeName() {
            return "FAKE";
        }

        final List<String> cancelled = new ArrayList<>();
        boolean failNext;

        @Override
        public Order placeLimitOrder(String symbol, Side side, BigDecimal price, BigDecimal size) throws IOException {
            if (failNext) {
                failNext = false;
                throw new IOException("boom");
            }
            String id = "o" + (orders.size() + 1);
            Order order = new Order(id, side, price, size, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    OrderStatus.OPEN);
            orders.put(id, order);
            return order;
        }

        @Override
        public Order getOrder(String symbol, String orderId) {
            return orders.get(orderId);
        }

        @Override
        public void cancelOrder(String symbol, String orderId) {
            cancelled.add(orderId);
        }

        @Override
        public Subscription subscribePrice(String symbol, PriceListener listener) {
            return () -> { };
        }

        void fill(String id) {
            Order o = orders.get(id);
            orders.put(id, new Order(id, o.side(), o.price(), o.size(), o.size(), o.size().multiply(o.price()),
                    BigDecimal.ZERO, OrderStatus.FILLED));
        }

        void partialFill(String id, String size) {
            Order o = orders.get(id);
            BigDecimal filled = new BigDecimal(size);
            orders.put(id, new Order(id, o.side(), o.price(), o.size(), filled, filled.multiply(o.price()),
                    BigDecimal.ZERO, OrderStatus.OPEN));
        }

        void cancel(String id) {
            Order o = orders.get(id);
            orders.put(id, new Order(id, o.side(), o.price(), o.size(), o.filledSize(), o.filledFunds(), o.fee(),
                    OrderStatus.CANCELLED));
        }
    }

    final FakeExchange exchange = new FakeExchange();
    final List<SellDownBotState> saved = new ArrayList<>();

    static SellDownBotState.LevelPlan level(int n, String sell, String trigger) {
        return new SellDownBotState.LevelPlan(n, "80", sell, trigger, "10", "100", null, null);
    }

    static SellDownBotState.LevelPlan level(int n, String sell, String trigger, String bePrice, String beTrigger) {
        return new SellDownBotState.LevelPlan(n, "80", sell, trigger, "10", "100", bePrice, beTrigger);
    }

    static SellDownBotState state(SellDownBotState.LevelPlan... plans) {
        return new SellDownBotState(new SellDownBotState.Definition("LTC-USDT", "2026-11-15T21:00:00Z", "60", "1000",
                "30", "0", "80", "90", "95", "1", "0.1", "0.1", "0.1", List.of(plans)),
                java.util.Arrays.stream(plans).map(plan -> new Level(plan.number(), Status.PENDING, null)).toList(),
                List.of(), List.of(), null);
    }

    SellDownBot bot(SellDownBotState state) {
        return new SellDownBot(state, exchange, Clock.fixed(Instant.parse("2026-11-15T22:00:00Z"), ZoneOffset.UTC),
                saved::add);
    }

    static SellDownBotState threeLevels() {
        // Levels 1-2 sell 20 BASE for 950 QUOTE; break-even at 47.4 costs 20 * 47.4 * 1.001 = 948.948: no loss
        return state(level(1, "50", "50.5"), level(2, "45", "45.5", "47.4", "47"),
                level(3, "40", "40.5", "42.4", "42"));
    }

    List<EventType> events(SellDownBot bot) {
        return bot.state().history().stream().map(SellDownBotState.Event::type).toList();
    }

    @Test
    void sellsEachLevelAtItsTrigger() {
        SellDownBot bot = bot(threeLevels());
        bot.onPrice(new BigDecimal("51"));
        assertTrue(exchange.orders.isEmpty());

        bot.onPrice(new BigDecimal("50.5"));
        bot.onPrice(new BigDecimal("50"));
        assertEquals(1, exchange.orders.size(), "no second order for the same level");
        BotExchange.Order sell = exchange.orders.get("o1");
        assertEquals(BotExchange.Side.SELL, sell.side());
        assertEquals(new BigDecimal("50"), sell.price());
        assertEquals(new BigDecimal("10"), sell.size());
        assertEquals(Status.SELLING, bot.state().levels().get(0).status());

        exchange.partialFill("o1", "4");
        bot.poll();
        assertEquals("4", bot.state().lastOrder(1).filled());
        assertEquals(new BigDecimal("6"), bot.state().lastOrder(1).remaining());
        assertEquals(Status.SELLING, bot.state().levels().get(0).status());

        exchange.fill("o1");
        bot.poll();
        assertEquals(Status.SOLD, bot.state().levels().get(0).status());
        assertEquals("10", bot.state().lastOrder(1).filled());
        assertEquals(0, bot.state().lastOrder(1).remaining().signum());
        assertEquals(Status.PENDING, bot.state().levels().get(1).status());

        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o2");
        bot.poll();
        bot.onPrice(new BigDecimal("40.5"));
        exchange.fill("o3");
        bot.poll();

        assertTrue(bot.state().isFinished());
        bot.state().levels().forEach(l -> assertEquals(Status.SOLD, l.status()));
        assertEquals(List.of(EventType.ORDER_PLACED, EventType.ORDER_FILLED, EventType.SOLD,
                EventType.ORDER_PLACED, EventType.ORDER_FILLED, EventType.SOLD,
                EventType.ORDER_PLACED, EventType.ORDER_FILLED, EventType.SOLD, EventType.FINISHED), events(bot));
        assertEquals(bot.state(), saved.get(saved.size() - 1));
    }

    @Test
    void breakEvenCancelsOpenSellAndBuysBackTwoLevels() {
        SellDownBot bot = bot(threeLevels());
        bot.onPrice(new BigDecimal("50"));
        exchange.fill("o1");
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o2");
        bot.poll();
        bot.onPrice(new BigDecimal("40"));
        assertEquals(Status.SELLING, bot.state().levels().get(2).status());

        bot.onPrice(new BigDecimal("46.99"));
        assertEquals(3, exchange.orders.size(), "below level 2 break-even trigger");

        bot.onPrice(new BigDecimal("47"));
        bot.onPrice(new BigDecimal("48"));
        assertEquals(List.of("o3"), exchange.cancelled);
        assertEquals(4, exchange.orders.size());
        BotExchange.Order buy = exchange.orders.get("o4");
        assertEquals(BotExchange.Side.BUY, buy.side());
        assertEquals(new BigDecimal("47.4"), buy.price());
        assertEquals(new BigDecimal("20.0221719209"), buy.size(), "all 950 QUOTE / (47.4 x 1.001)");
        assertEquals(Status.BUYING_BACK, bot.state().levels().get(1).status());

        exchange.cancel("o3");
        exchange.fill("o4");
        bot.poll();
        assertEquals(Status.PENDING, bot.state().levels().get(2).status());
        assertEquals(Status.BROKEN_EVEN, bot.state().levels().get(1).status());
        assertTrue(bot.state().isFinished());
        List<EventType> events = events(bot);
        assertEquals(EventType.FINISHED, events.get(events.size() - 1));
        assertTrue(events.contains(EventType.ORDER_CANCELLED));

        bot.onPrice(new BigDecimal("30"));
        assertEquals(4, exchange.orders.size(), "finished: no more orders");
    }

    @Test
    void suddenDropSellsAllTriggeredLevelsAndBreakEvenCancelsThem() {
        SellDownBot bot = bot(threeLevels());
        bot.onPrice(new BigDecimal("50"));
        exchange.fill("o1");
        bot.poll();
        bot.onPrice(new BigDecimal("39"));
        assertEquals(Status.SELLING, bot.state().levels().get(1).status());
        assertEquals(Status.SELLING, bot.state().levels().get(2).status());
        assertEquals(3, exchange.orders.size());

        exchange.fill("o2");
        bot.poll();
        bot.onPrice(new BigDecimal("55"));
        assertEquals(List.of("o3"), exchange.cancelled);
        assertEquals(BotExchange.Side.BUY, exchange.orders.get("o4").side());
    }

    @Test
    void breakEvenUsesActualFills() {
        SellDownBot bot = bot(threeLevels());
        bot.onPrice(new BigDecimal("50"));
        exchange.partialFill("o1", "4");
        exchange.cancel("o1");
        bot.poll();
        exchange.fill("o2");
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o3");
        bot.poll();

        SellDownBotState.BreakEven breakEven = bot.state().breakEven();
        assertEquals(2, breakEven.level());
        assertEquals(0, new BigDecimal("20").compareTo(breakEven.base()));
        assertEquals(0, new BigDecimal("950").compareTo(breakEven.quote()));
        assertEquals(new BigDecimal("47.4"), breakEven.price(), "planned price");
        assertEquals(new BigDecimal("0.0221719209"), bot.state().breakEvenGain(breakEven, breakEven.price()),
                "BASE back beyond the 20 sold");
    }

    @Test
    void plannedBreakEvenAtALossIsOff() {
        // 20 * 48 * 1.001 = 960.96 > the 950 QUOTE received
        SellDownBot bot = bot(state(level(1, "50", "50.5"), level(2, "45", "45.5", "48", "47.5"),
                level(3, "40", "40.5", "42.4", "42")));
        bot.onPrice(new BigDecimal("50"));
        exchange.fill("o1");
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o2");
        bot.poll();

        SellDownBotState.BreakEven breakEven = bot.state().breakEven();
        assertEquals("would buy back 0.228105 BASE less than sold at the planned price 48", breakEven.autoUnavailable());
        assertEquals(null, breakEven.price());
        assertEquals(SellDownBotTab.Outlook.OFF, SellDownBotTab.breakEvenOutlook(bot.state(), breakEven));
        assertTrue(SellDownBotTab.breakEvenText(bot.state(), breakEven).contains("OFF, WON'T TRIGGER"));
        bot.onPrice(new BigDecimal("60"));
        assertEquals(2, exchange.orders.size(), "no automatic break-even");
    }

    @Test
    void autoBreakEvenCollidingWithNextSellTriggerIsUnavailable() {
        // Level 3 triggers at 47.5, above level 2's break-even trigger (~46.98)
        SellDownBot bot = bot(state(level(1, "50", "50.5"), level(2, "45", "45.5", "47.4", "47"),
                level(3, "47", "47.5", "42.4", "42")));
        bot.onPrice(new BigDecimal("50"));
        exchange.fill("o1");
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o2");
        bot.poll();
        int placed = exchange.orders.size();

        SellDownBotState.BreakEven breakEven = bot.state().breakEven();
        assertTrue(breakEven.autoUnavailable().contains("level 3's sell trigger 47.5"), breakEven.autoUnavailable());
        assertEquals(null, breakEven.price());
        bot.onPrice(new BigDecimal("60"));
        assertEquals(placed, exchange.orders.size(), "no automatic break-even");
        assertTrue(SellDownBotTab.breakEvenText(bot.state(), breakEven).contains("AUTO BREAK-EVEN (levels 1-2): OFF"));
    }

    @Test
    void manualBreakEvenTriggerOverridesAutomatic() throws IOException {
        SellDownBot bot = bot(threeLevels());
        bot.onPrice(new BigDecimal("50"));
        exchange.fill("o1");
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o2");
        bot.poll();
        bot.onPrice(new BigDecimal("43"));
        bot.start(java.util.concurrent.Executors.newSingleThreadScheduledExecutor(), 3_600_000);
        try {
            // Buy price = trigger / (1 - 1% fill buffer) = 43.56 / 0.99 = 44
            bot.setManualBreakEvenTrigger(new BigDecimal("43.56"));
            assertEquals(EventType.BREAK_EVEN_SET, events(bot).get(events(bot).size() - 1));
            SellDownBotState.BreakEven breakEven = bot.state().breakEven();
            assertEquals(new BigDecimal("44"), breakEven.price());
            assertTrue(bot.state().breakEvenGain(breakEven, new BigDecimal("44")).signum() > 0);
            assertTrue(bot.state().breakEvenGain(breakEven, new BigDecimal("48")).signum() < 0);

            bot.onPrice(new BigDecimal("43.5"));
            assertTrue(exchange.orders.values().stream().noneMatch(o -> o.side() == BotExchange.Side.BUY),
                    "below the manual trigger 43.56");
            bot.onPrice(new BigDecimal("43.6"));
            BotExchange.Order buy = exchange.orders.values().stream().filter(o -> o.side() == BotExchange.Side.BUY)
                    .findFirst().orElseThrow();
            assertEquals(0, new BigDecimal("44").compareTo(buy.price()));
            assertEquals(new BigDecimal("21.5693397511"), buy.size(), "all 950 QUOTE / (44 x 1.001)");
        } finally {
            bot.stop();
        }
    }

    @Test
    void manualBreakEvenIsBotWide() throws IOException {
        SellDownBot bot = bot(threeLevels());
        bot.setManualBreakEvenTrigger(new BigDecimal("60"));
        assertTrue(SellDownBotTab.breakEvenText(bot.state(), bot.state().breakEven())
                .contains("MANUAL trigger 60"));
        bot.onPrice(new BigDecimal("50"));
        exchange.fill("o1");
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o2");
        bot.poll();
        assertEquals(new BigDecimal("60"), bot.state().breakEven().trigger(), "applies to level 2");
        bot.onPrice(new BigDecimal("40"));
        exchange.fill("o3");
        bot.poll();
        SellDownBotState.BreakEven breakEven = bot.state().breakEven();
        assertEquals(3, breakEven.level());
        assertEquals(new BigDecimal("60"), breakEven.trigger(), "and to level 3, replacing its planned 42");

        java.io.File file = java.io.File.createTempFile("bot", ".bot");
        file.deleteOnExit();
        bot.state().write(file);
        SellDownBotState saved = SellDownBotState.read(file);
        assertEquals("60", saved.manualBreakEvenTrigger());
        assertEquals("60.6060606061", saved.manualBreakEvenPrice(), "60 / 0.99, saved when set");
    }

    @Test
    void manualBreakEvenAtTheCurrentPriceBuysAboveIt() throws IOException {
        SellDownBot bot = bot(threeLevels());
        bot.onPrice(new BigDecimal("50"));
        exchange.fill("o1");
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o2");
        bot.poll();
        bot.onPrice(new BigDecimal("43"));
        bot.start(java.util.concurrent.Executors.newSingleThreadScheduledExecutor(), 3_600_000);
        try {
            bot.setManualBreakEvenTrigger(new BigDecimal("43"));
            BotExchange.Order buy = exchange.orders.values().stream().filter(o -> o.side() == BotExchange.Side.BUY)
                    .findFirst().orElseThrow();
            assertEquals(new BigDecimal("43.4343434343"), buy.price(), "43 / 0.99");

            // Still shown and changeable while the buy is open
            SellDownBotState.BreakEven breakEven = bot.state().breakEven();
            assertEquals(2, breakEven.level());
            assertEquals(new BigDecimal("43"), breakEven.manualTrigger());
            String text = SellDownBotTab.breakEvenText(bot.state(), breakEven);
            assertTrue(text.startsWith("MANUAL BREAK-EVEN (bot-wide), trigger 43"), text);
            assertTrue(text.contains("BUY ORDER OPEN"), text);

            // Half the buy fills, then the trigger is moved: the buy is cancelled and only the rest is bought again
            exchange.partialFill(buy.orderId(), "10");
            bot.setManualBreakEvenTrigger(new BigDecimal("42.57"));
            assertTrue(exchange.cancelled.contains(buy.orderId()));
            exchange.cancel(buy.orderId());
            bot.poll();
            BotExchange.Order rebuy = exchange.orders.values().stream()
                    .filter(o -> o.side() == BotExchange.Side.BUY && !o.orderId().equals(buy.orderId()))
                    .findFirst().orElseThrow();
            assertEquals(0, new BigDecimal("43").compareTo(rebuy.price()), "42.57 / 0.99");
            assertEquals(new BigDecimal("11.9800331216"), rebuy.size(), "the QUOTE left: (950 - 10 x 43.4343434343) / (43 x 1.001)");

            // Clearing it while buying: cancelled, and the automatic one (trigger 47) is not hit at 43
            bot.setManualBreakEvenTrigger(null);
            assertTrue(exchange.cancelled.contains(rebuy.orderId()));
            exchange.cancel(rebuy.orderId());
            bot.poll();
            assertEquals(Status.SOLD, bot.state().levels().get(1).status());
            assertTrue(SellDownBotTab.breakEvenText(bot.state(), bot.state().breakEven())
                    .startsWith("AUTO BREAK-EVEN (levels 1-2): ON"));
        } finally {
            bot.stop();
        }
    }

    @Test
    void nothingIsSoldWithoutPrice() {
        SellDownBot bot = bot(state(level(1, "59", "70"), level(2, "45", "45.5", "50", "49")));
        bot.poll();
        assertTrue(exchange.orders.isEmpty());
        bot.onPrice(new BigDecimal("60"));
        assertEquals(new BigDecimal("59"), exchange.orders.get("o1").price());
    }

    @Test
    void cancelledPartialSellOnlySellsTheRest() {
        SellDownBot bot = bot(threeLevels());
        bot.onPrice(new BigDecimal("50"));
        exchange.partialFill("o1", "4");
        exchange.cancel("o1");
        bot.poll();
        assertEquals(new BigDecimal("6"), exchange.orders.get("o2").size());

        exchange.partialFill("o2", "6");
        exchange.cancel("o2");
        bot.poll();
        assertEquals(Status.SOLD, bot.state().levels().get(0).status());
        assertEquals(2, exchange.orders.size(), "nothing left to sell");
    }

    @Test
    void placeErrorIsRecordedAndRetried() {
        SellDownBot bot = bot(threeLevels());
        exchange.failNext = true;
        bot.onPrice(new BigDecimal("50"));
        assertEquals(List.of(EventType.ERROR), events(bot));
        assertEquals(Status.PENDING, bot.state().levels().get(0).status());

        bot.onPrice(new BigDecimal("50"));
        assertEquals(Status.SELLING, bot.state().levels().get(0).status());
        assertFalse(bot.state().isFinished());
    }

    @Test
    void simulationTexts() {
        SellDownBot bot = bot(threeLevels());
        bot.onPrice(new BigDecimal("50"));
        assertEquals("1", SimulatedExchangeTab.levelOf(bot.state(), "o1"));
        assertEquals(SellDownApproximatorTab.NONE, SimulatedExchangeTab.levelOf(bot.state(), "o9"));
        assertEquals("Simulation: LTC-USDT", SimulatedExchangeTab.title(new java.io.File("/tmp/LTC-USDT.bot")));
    }

    @Test
    void ordersKeepTheirHistory() {
        // 4 + 10 BASE sold for 650 QUOTE; break-even at 46 costs 14 * 46 * 1.001 = 644.644: no loss
        SellDownBot bot = bot(state(level(1, "50", "50.5"), level(2, "45", "45.5", "46", "45.5"),
                level(3, "40", "40.5", "42.4", "42")));
        bot.onPrice(new BigDecimal("50"));
        bot.poll();
        exchange.partialFill("o1", "4");
        bot.poll();
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        exchange.fill("o2");
        bot.poll();
        bot.onPrice(new BigDecimal("55"));
        bot.onPrice(new BigDecimal("55"));
        exchange.cancel("o1");
        bot.poll();

        assertEquals(List.of("o1"), exchange.cancelled, "cancel is requested once");
        SellDownBotState.BotOrder sell = bot.state().order("o1");
        assertEquals(List.of(SellDownBotState.OrderEventType.PLACED, SellDownBotState.OrderEventType.UPDATED,
                SellDownBotState.OrderEventType.CANCEL_REQUESTED, SellDownBotState.OrderEventType.CLOSED),
                sell.history().stream().map(SellDownBotState.BotOrderEvent::type).toList());
        assertEquals(BotExchange.OrderStatus.CANCELLED, sell.status());
        assertEquals("4", sell.filled());
        assertEquals("2026-11-15T22:00:00Z", sell.closedAt());
        assertEquals(List.of("o1", "o2", "o3"), bot.state().orders().stream()
                .map(SellDownBotState.BotOrder::orderId).toList());
        assertEquals(BotExchange.Side.BUY, bot.state().lastOrder(2).side());
        assertEquals(null, bot.state().order("o3").closedAt());
    }

    @Test
    void levelTracksFilledQuoteAndFee() {
        com.poplavok.data.bot.SimulatedExchange sim = new com.poplavok.data.bot.SimulatedExchange(
                new BigDecimal("0.001"));
        SellDownBot bot = new SellDownBot(threeLevels(), sim, Clock.systemUTC(), saved::add);
        bot.onPrice(new BigDecimal("50"));
        sim.fill("sim-1", new BigDecimal("4"), new BigDecimal("52"));
        bot.poll();
        SellDownBotState.BotOrder order = bot.state().lastOrder(1);
        assertEquals("208", order.filledQuote());
        assertEquals("0.208", order.fee());

        sim.fill("sim-1", new BigDecimal("6"));
        bot.poll();
        assertEquals(Status.SOLD, bot.state().levels().get(0).status());
        order = bot.state().lastOrder(1);
        assertEquals("508", order.filledQuote());
        assertEquals("0.508", order.fee());
    }

    @Test
    void consecutiveErrorsAreCountedInOneEvent() throws Exception {
        com.poplavok.data.bot.SimulatedExchange sim = new com.poplavok.data.bot.SimulatedExchange(BigDecimal.ZERO);
        SellDownBot bot = new SellDownBot(threeLevels(), sim, Clock.systemUTC(), saved::add);
        sim.setFailing(com.poplavok.data.bot.SimulatedExchange.Failure.PLACE, true);
        for (int i = 0; i < 5; i++) {
            bot.onPrice(new BigDecimal("50"));
        }
        sim.setFailing(com.poplavok.data.bot.SimulatedExchange.Failure.PLACE, false);
        bot.onPrice(new BigDecimal("50"));
        sim.setFailing(com.poplavok.data.bot.SimulatedExchange.Failure.POLL, true);
        bot.poll();
        bot.poll();
        sim.setFailing(com.poplavok.data.bot.SimulatedExchange.Failure.POLL, false);
        sim.fill("sim-1", new BigDecimal("10"));
        bot.poll();
        SellDownBotState state = bot.state().withError("t1", 2, "place sell: IOException", "a")
                .withError("t2", 1, "getOrder: IOException", "b");

        assertEquals(List.of(EventType.ERROR, EventType.ORDER_PLACED, EventType.ERROR, EventType.ORDER_FILLED,
                EventType.SOLD), events(bot));
        assertEquals(EventType.ERROR, state.history().get(state.history().size() - 1).type());
        assertEquals(bot.state().history().size() + 1, state.history().size());
        List<SellDownBotState.Event> errors = new java.util.ArrayList<>(bot.state().history().stream()
                .filter(e -> e.type() == EventType.ERROR).toList());
        errors.add(state.history().get(state.history().size() - 1));
        assertEquals(List.of(new SellDownBotState.ErrorCount("place sell: IOException", 5)), errors.get(0).errors());
        assertEquals(5, errors.get(0).errorCount());
        assertEquals(Integer.valueOf(1), errors.get(0).level());
        assertEquals(List.of(new SellDownBotState.ErrorCount("getOrder: IOException", 2)), errors.get(1).errors());
        assertEquals("place sell: IOException x1, getOrder: IOException x1", errors.get(2).errorSummary(),
                "different types are counted apart in the same event");
        assertEquals(null, errors.get(2).level(), "levels 2 and 1");
        assertEquals("t1", errors.get(2).time());
        assertEquals("t2", errors.get(2).lastTime());
        assertEquals("b", errors.get(2).note(), "latest message");

        SellDownBotState loaded = SellDownBotState.MAPPER.readValue(
                SellDownBotState.MAPPER.writeValueAsString(state), SellDownBotState.class);
        assertEquals(state.history(), loaded.history());
    }

    @Test
    void recoversFromInjectedFailures() {
        com.poplavok.data.bot.SimulatedExchange sim = new com.poplavok.data.bot.SimulatedExchange(BigDecimal.ZERO);
        SellDownBot bot = new SellDownBot(threeLevels(), sim, Clock.systemUTC(), saved::add);
        sim.setFailing(com.poplavok.data.bot.SimulatedExchange.Failure.PLACE, true);
        bot.onPrice(new BigDecimal("50"));
        assertEquals(Status.PENDING, bot.state().levels().get(0).status());
        sim.setFailing(com.poplavok.data.bot.SimulatedExchange.Failure.PLACE, false);
        bot.onPrice(new BigDecimal("50"));
        assertEquals(Status.SELLING, bot.state().levels().get(0).status());

        sim.fill("sim-1", new BigDecimal("10"));
        sim.setFailing(com.poplavok.data.bot.SimulatedExchange.Failure.POLL, true);
        bot.poll();
        assertEquals(Status.SELLING, bot.state().levels().get(0).status());
        sim.setFailing(com.poplavok.data.bot.SimulatedExchange.Failure.POLL, false);
        bot.poll();
        assertEquals(Status.SOLD, bot.state().levels().get(0).status());
        assertEquals(List.of(EventType.ERROR, EventType.ORDER_PLACED, EventType.ERROR, EventType.ORDER_FILLED,
                EventType.SOLD), events(bot).subList(0, 5));
    }

    @Test
    void bindsToExchangeOnFirstStartAndRefusesOthers() throws Exception {
        java.util.concurrent.ScheduledExecutorService scheduler =
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        try {
            SellDownBot bot = bot(threeLevels());
            bot.start(scheduler, 60_000);
            bot.stop();
            assertEquals("FAKE", bot.state().exchangeName());
            assertEquals(EventType.BOUND, bot.state().history().get(0).type());
            bot.start(scheduler, 60_000);
            bot.stop();
            assertEquals(1, bot.state().history().stream().filter(e -> e.type() == EventType.BOUND).count());

            SellDownBot elsewhere = bot(threeLevels().withExchangeName("KUCOIN"));
            IOException e = org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                    () -> elsewhere.start(scheduler, 60_000));
            assertTrue(e.getMessage().contains("KUCOIN"), e.getMessage());
            assertFalse(elsewhere.isRunning());
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void reloadedBotContinuesWithRestoredOrders() {
        com.poplavok.data.bot.SimulatedExchange sim = new com.poplavok.data.bot.SimulatedExchange(
                new BigDecimal("0.001"));
        SellDownBot bot = new SellDownBot(threeLevels(), sim, Clock.systemUTC(), saved::add);
        bot.onPrice(new BigDecimal("50"));
        sim.fill("sim-1", new BigDecimal("4"));
        bot.poll();

        SellDownBotState reloaded = bot.state();
        com.poplavok.data.bot.SimulatedExchange restored = new com.poplavok.data.bot.SimulatedExchange(
                new BigDecimal("0.001"));
        reloaded.orders().forEach(order -> restored.restore(order.toExchangeOrder()));
        BotExchange.Order original = sim.orders().get(0);
        BotExchange.Order back = restored.orders().get(0);
        assertEquals(original.orderId(), back.orderId());
        assertEquals(original.status(), back.status());
        assertEquals(0, original.filledSize().compareTo(back.filledSize()));
        assertEquals(0, original.fee().compareTo(back.fee()));

        SellDownBot again = new SellDownBot(reloaded, restored, Clock.systemUTC(), saved::add);
        restored.fill("sim-1", new BigDecimal("6"));
        again.poll();
        assertEquals(Status.SOLD, again.state().levels().get(0).status());
        again.onPrice(new BigDecimal("45"));
        assertEquals("sim-2", again.state().levels().get(1).orderId());
    }

    @Test
    void levelHoldsItsBaseUntilSoldThenQuote() {
        com.poplavok.data.bot.SimulatedExchange sim = new com.poplavok.data.bot.SimulatedExchange(
                new BigDecimal("0.001"));
        SellDownBot bot = new SellDownBot(threeLevels(), sim, Clock.systemUTC(), saved::add);
        Level first = bot.state().levels().get(0);
        assertHolds(bot.state().holdings(first), "10", "0", "0");

        bot.onPrice(new BigDecimal("50"));
        sim.fill("sim-1", new BigDecimal("4"));
        bot.poll();
        assertHolds(bot.state().holdings(first), "6", "199.8", "0.2");

        sim.fill("sim-1", new BigDecimal("6"));
        bot.poll();
        bot.onPrice(new BigDecimal("45"));
        sim.fill("sim-2", new BigDecimal("10"));
        bot.poll();
        bot.onPrice(new BigDecimal("62"));
        // Break-even buy of levels 1-2 for 940 + 0.94 fee, paid from their 499.5 + 449.55 QUOTE by QUOTE share
        sim.fill("sim-3", new BigDecimal("20"), new BigDecimal("47"));
        bot.poll();
        Level second = bot.state().levels().get(1);
        SellDownBotState.Holdings one = bot.state().holdings(first);
        SellDownBotState.Holdings two = bot.state().holdings(second);
        BigDecimal left = new BigDecimal("8.11");
        assertHolds(one, "10.52631578947368", left.multiply(new BigDecimal("499.5"))
                .divide(new BigDecimal("949.05"), java.math.MathContext.DECIMAL64).toPlainString(), null);
        assertTrue(two.quote().signum() > 0, two.toString());
        assertEquals(0, new BigDecimal("20").compareTo(one.base().add(two.base()).setScale(10, java.math.RoundingMode.HALF_UP)));
        assertEquals(0, left.compareTo(one.quote().add(two.quote()).setScale(10, java.math.RoundingMode.HALF_UP)));
        assertEquals(0, new BigDecimal("1.89").compareTo(one.fee().add(two.fee()).setScale(10, java.math.RoundingMode.HALF_UP)));
        assertHolds(bot.state().holdings(bot.state().levels().get(2)), "10", "0", "0");
        // Definition holds 30 BASE, 0 QUOTE: sold 20, bought back 20
        assertHolds(bot.state().holdings(), "30", "8.11", "1.89");

        String[][] cells = SellDownBotTab.currentCells(bot.state(), new BigDecimal("60"));
        assertEquals(com.poplavok.data.utils.BigDecimalUtil.formatAmount(new BigDecimal("60")), cells[0][1]);
        assertEquals(com.poplavok.data.utils.BigDecimalUtil.formatAmount(new BigDecimal("30")), cells[0][3]);
        assertEquals("5.123457", SellDownBotTab.stat(new BigDecimal("5.123456789")));
        assertEquals("2.50", SellDownBotTab.stat(new BigDecimal("2.5")));
        assertEquals("55.31%", cells[0][5]);
        assertEquals(com.poplavok.data.utils.BigDecimalUtil.formatAmount(new BigDecimal("60")) + " (planned)", SellDownBotTab.currentCells(bot.state(), null)[0][1]);
    }

    static void assertHolds(SellDownBotState.Holdings holdings, String base, String quote, @Nullable String fee) {
        int scale = 10;
        assertEquals(0, new BigDecimal(base).setScale(scale, java.math.RoundingMode.HALF_UP)
                .compareTo(holdings.base().setScale(scale, java.math.RoundingMode.HALF_UP)), "base " + holdings);
        assertEquals(0, new BigDecimal(quote).setScale(scale, java.math.RoundingMode.HALF_UP)
                .compareTo(holdings.quote().setScale(scale, java.math.RoundingMode.HALF_UP)), "quote " + holdings);
        if (fee != null) {
            assertEquals(0, new BigDecimal(fee).compareTo(holdings.fee()), "fee " + holdings);
        }
    }
}
