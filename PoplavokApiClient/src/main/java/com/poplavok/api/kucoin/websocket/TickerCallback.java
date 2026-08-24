package com.poplavok.api.kucoin.websocket;

import com.poplavok.api.kucoin.websocket.event.KucoinEvent;
import com.poplavok.api.kucoin.websocket.event.TickerChangeEvent;

public interface TickerCallback {
    /**
     * ------
     * Symbol Ticker
     * Topic: /market/ticker:{symbol},{symbol}...
     * <p>
     * Push frequency: once every 100ms
     * Subscribe to this topic to get the push of BBO changes.
     * <p>
     * Please note that more information may be added to messages from this channel in the near future.
     * <p>
     * ------
     * All Symbols Ticker
     * Topic: /market/ticker:all
     * <p>
     * Push frequency: once every 2s
     * Subscribe to this topic to get the push of all market symbols BBO change.
     */
    void tickerCallback(KucoinEvent<TickerChangeEvent> event);
}
