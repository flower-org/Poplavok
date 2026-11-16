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

    static SellDownBotState.LevelPlan level(int n, String sell, String trigger, boolean immediate,
                                            String beTrigger) {
        return new SellDownBotState.LevelPlan(n, "80", sell, trigger, immediate, "10", "100",
                beTrigger == null ? null : "6" + n, beTrigger);
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
        return state(level(1, "50", "50.5", false, null), level(2, "45", "45.5", false, "55"),
                level(3, "40", "40.5", false, "50"));
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

        bot.onPrice(new BigDecimal("54.9"));
        assertEquals(3, exchange.orders.size(), "below level 2 break-even trigger");

        bot.onPrice(new BigDecimal("55"));
        bot.onPrice(new BigDecimal("56"));
        assertEquals(List.of("o3"), exchange.cancelled);
        assertEquals(4, exchange.orders.size());
        BotExchange.Order buy = exchange.orders.get("o4");
        assertEquals(BotExchange.Side.BUY, buy.side());
        assertEquals(new BigDecimal("62"), buy.price());
        assertEquals(new BigDecimal("20"), buy.size());
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
    void immediateLevelSellsWithoutPrice() {
        SellDownBot bot = bot(state(level(1, "59", "70", true, null), level(2, "45", "45.5", false, "65")));
        bot.poll();
        assertEquals(new BigDecimal("59"), exchange.orders.get("o1").price());
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
        SellDownBot bot = bot(threeLevels());
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
