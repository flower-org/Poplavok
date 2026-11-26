package com.poplavok.bots.selldown;

import com.flower.eventbus.Pollable;
import com.poplavok.data.bot.BotExchange;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The status of the bot's orders as a stream of snapshots, whatever the source: periodic polling
 * ({@link OrderStatusPoller}) or, later, an exchange push stream. The reader names the orders it wants with
 * {@link #watch}. An empty batch means the stream is closed.
 */
interface OrderStatuses extends Pollable<OrderStatuses.Snapshot> {
    /**
     * A fresh view of the watched orders: {@code orders} has the latest status of each one fetched, {@code failures}
     * the ones whose fetch failed, by exchange order ID ({@link BotExchange.Order#orderId()}), with why.
     * May be empty: it still tells that nothing changed.
     */
    record Snapshot(List<BotExchange.Order> orders, Map<String, Exception> failures) {
    }

    /** The orders to report on from now on, replacing the previous set; an order is dropped once it's done. */
    void watch(Set<String> orderIds);
}
