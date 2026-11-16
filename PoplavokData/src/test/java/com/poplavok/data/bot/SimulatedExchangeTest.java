package com.poplavok.data.bot;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimulatedExchangeTest {
    final SimulatedExchange exchange = new SimulatedExchange(new BigDecimal("0.001"));

    @Test
    void partialThenFullFill() throws Exception {
        BotExchange.Order order = exchange.placeLimitOrder("LTC-USDT", BotExchange.Side.SELL, new BigDecimal("50"),
                new BigDecimal("10"));
        exchange.fill(order.orderId(), new BigDecimal("4"));
        BotExchange.Order partial = exchange.getOrder("LTC-USDT", order.orderId());
        assertEquals(BotExchange.OrderStatus.OPEN, partial.status());
        assertEquals(0, new BigDecimal("200").compareTo(partial.filledFunds()));
        assertEquals(0, new BigDecimal("0.2").compareTo(partial.fee()));

        exchange.fill(order.orderId(), new BigDecimal("100"));
        BotExchange.Order filled = exchange.getOrder("LTC-USDT", order.orderId());
        assertEquals(BotExchange.OrderStatus.FILLED, filled.status());
        assertEquals(0, new BigDecimal("10").compareTo(filled.filledSize()));
        assertThrows(IllegalStateException.class, () -> exchange.fill(order.orderId(), BigDecimal.ONE));
    }

    @Test
    void cancelKeepsFilledPart() throws Exception {
        BotExchange.Order order = exchange.placeLimitOrder("LTC-USDT", BotExchange.Side.BUY, new BigDecimal("50"),
                new BigDecimal("10"));
        exchange.fill(order.orderId(), new BigDecimal("3"));
        exchange.cancelOrder("LTC-USDT", order.orderId());
        BotExchange.Order cancelled = exchange.getOrder("LTC-USDT", order.orderId());
        assertEquals(BotExchange.OrderStatus.CANCELLED, cancelled.status());
        assertEquals(0, new BigDecimal("3").compareTo(cancelled.filledSize()));
    }

    @Test
    void priceGoesToSubscribersUntilClosed() throws Exception {
        List<BigDecimal> prices = new ArrayList<>();
        BotExchange.Subscription subscription = exchange.subscribePrice("LTC-USDT", prices::add);
        exchange.setPrice(new BigDecimal("51"));
        subscription.close();
        exchange.setPrice(new BigDecimal("52"));
        assertEquals(List.of(new BigDecimal("51")), prices);
        assertEquals(new BigDecimal("52"), exchange.price());
    }

    @Test
    void logsLatestPollsNewestFirst() throws Exception {
        BotExchange.Order order = exchange.placeLimitOrder("LTC-USDT", BotExchange.Side.SELL, new BigDecimal("50"),
                new BigDecimal("10"));
        exchange.getOrder("LTC-USDT", order.orderId());
        exchange.fill(order.orderId(), new BigDecimal("4"));
        for (int i = 0; i < SimulatedExchange.MAX_POLLS; i++) {
            exchange.getOrder("LTC-USDT", order.orderId());
        }
        List<SimulatedExchange.Poll> polls = exchange.polls();
        assertEquals(SimulatedExchange.MAX_POLLS, polls.size());
        assertEquals(0, new BigDecimal("4").compareTo(polls.get(0).order().filledSize()));

        exchange.cancelOrder("LTC-USDT", order.orderId());
        assertEquals(SimulatedExchange.MAX_POLLS, exchange.polls().size());
        assertEquals(BotExchange.OrderStatus.OPEN, exchange.polls().get(0).order().status(), "cancel isn't a poll");
    }

    @Test
    void fillsAtLimitOrBetterOnly() throws Exception {
        BotExchange.Order sell = exchange.placeLimitOrder("LTC-USDT", BotExchange.Side.SELL, new BigDecimal("50"),
                new BigDecimal("10"));
        assertThrows(IllegalArgumentException.class,
                () -> exchange.fill(sell.orderId(), BigDecimal.ONE, new BigDecimal("49.9")));
        exchange.fill(sell.orderId(), new BigDecimal("5"), new BigDecimal("52"));
        exchange.fill(sell.orderId(), new BigDecimal("5"));
        BotExchange.Order filled = exchange.getOrder("LTC-USDT", sell.orderId());
        assertEquals(0, new BigDecimal("510").compareTo(filled.filledFunds()));
        assertEquals(0, new BigDecimal("51").compareTo(filled.averageFillPrice()));

        BotExchange.Order buy = exchange.placeLimitOrder("LTC-USDT", BotExchange.Side.BUY, new BigDecimal("50"),
                new BigDecimal("10"));
        assertThrows(IllegalArgumentException.class,
                () -> exchange.fill(buy.orderId(), BigDecimal.ONE, new BigDecimal("50.1")));
        exchange.fill(buy.orderId(), BigDecimal.ONE, new BigDecimal("48"));
        assertEquals(0, new BigDecimal("48").compareTo(exchange.getOrder("LTC-USDT", buy.orderId()).filledFunds()));
    }

    @Test
    void injectsFailuresWhileSet() throws Exception {
        BotExchange.Order order = exchange.placeLimitOrder("LTC-USDT", BotExchange.Side.SELL, new BigDecimal("50"),
                new BigDecimal("10"));
        exchange.setFailing(SimulatedExchange.Failure.PLACE, true);
        exchange.setFailing(SimulatedExchange.Failure.POLL, true);
        exchange.setFailing(SimulatedExchange.Failure.CANCEL, true);
        assertThrows(java.io.IOException.class, () -> exchange.placeLimitOrder("LTC-USDT", BotExchange.Side.SELL,
                new BigDecimal("50"), new BigDecimal("1")));
        assertThrows(java.io.IOException.class, () -> exchange.getOrder("LTC-USDT", order.orderId()));
        assertThrows(java.io.IOException.class, () -> exchange.cancelOrder("LTC-USDT", order.orderId()));
        assertEquals(1, exchange.orders().size());
        assertEquals(BotExchange.OrderStatus.OPEN, exchange.orders().get(0).status());
        assertEquals(true, exchange.polls().get(0).failed());

        exchange.cancel(order.orderId());
        assertEquals(BotExchange.OrderStatus.CANCELLED, exchange.orders().get(0).status());

        exchange.setFailing(SimulatedExchange.Failure.POLL, false);
        assertEquals(BotExchange.OrderStatus.CANCELLED, exchange.getOrder("LTC-USDT", order.orderId()).status());
        assertEquals(false, exchange.polls().get(0).failed());
    }
}
