package com.poplavok.kucoin;

/** A source of streamed ticker prices that listeners can subscribe to per symbol. */
public interface PriceFeed {
    void subscribe(String symbol, TickerPriceService.PriceListener listener);

    void unsubscribe(String symbol, TickerPriceService.PriceListener listener);
}
