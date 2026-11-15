package com.poplavok.data.bot;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotExchangeTest {
    static BotExchange.Order order(String filled, BotExchange.OrderStatus status) {
        return new BotExchange.Order("o1", BotExchange.Side.SELL, new BigDecimal("44.5"), new BigDecimal("10"),
                new BigDecimal(filled), BigDecimal.ZERO, BigDecimal.ZERO, status);
    }

    @Test
    void openOrderHasRemainingSize() {
        BotExchange.Order order = order("4", BotExchange.OrderStatus.OPEN);
        assertEquals(new BigDecimal("6"), order.remainingSize());
        assertFalse(order.isDone());
    }

    @Test
    void doneOrdersHaveNothingRemaining() {
        BotExchange.Order cancelled = order("4", BotExchange.OrderStatus.CANCELLED);
        assertEquals(BigDecimal.ZERO, cancelled.remainingSize());
        assertTrue(cancelled.isDone());
        assertTrue(order("10", BotExchange.OrderStatus.FILLED).isDone());
    }
}
