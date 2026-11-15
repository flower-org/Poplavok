package com.poplavok.data.bot;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * What the sell-down bot needs from an exchange: place and cancel limit orders on the symbol's trading account
 * (on KuCoin: its isolated margin account), poll their status and receive price updates. Implemented by KuCoin and by a
 * simulated exchange. Listeners may be called on any thread.
 */
public interface BotExchange {
    enum Side { BUY, SELL }

    /** OPEN: resting (possibly partially filled); FILLED: fully filled; CANCELLED: cancelled (possibly partially filled). */
    enum OrderStatus { OPEN, FILLED, CANCELLED }

    /** Order snapshot. {@code filledFunds}: QUOTE value of the filled part; {@code fee} in QUOTE. */
    record Order(String orderId, Side side, BigDecimal price, BigDecimal size, BigDecimal filledSize,
                 BigDecimal filledFunds, BigDecimal fee, OrderStatus status) {
        /** Size still waiting to fill; zero once the order is done. */
        public BigDecimal remainingSize() {
            return status == OrderStatus.OPEN ? size.subtract(filledSize) : BigDecimal.ZERO;
        }

        public boolean isDone() {
            return status != OrderStatus.OPEN;
        }
    }

    interface PriceListener {
        void onPrice(BigDecimal price);
    }

    /** Stops a subscription; idempotent. */
    interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    /** Places a limit order without borrowing. */
    Order placeLimitOrder(String symbol, Side side, BigDecimal price, BigDecimal size) throws IOException;

    /** Current state of an order: fills so far and status. */
    Order getOrder(String symbol, String orderId) throws IOException;

    /** Requests cancellation; the final state is seen via {@link #getOrder}. */
    void cancelOrder(String symbol, String orderId) throws IOException;

    /** Last trade price updates for {@code symbol}. */
    Subscription subscribePrice(String symbol, PriceListener listener) throws IOException;
}
