package com.poplavok.data.bot;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * What the sell-down bot needs from an exchange: place and cancel limit orders on the symbol's trading account
 * (on KuCoin: its isolated margin account), poll their status and receive price updates. Implemented by KuCoin and by a
 * simulated exchange. Listeners may be called on any thread.
 * <p>
 * Contract: orders never borrow and never repay, so the debt stays as it is and the QUOTE a sell brings in stays in
 * the account for the break-even buy. Fees are reported in QUOTE (0 if paid in another currency). The exchange may
 * round a limit order to its increments, only in the bot's favour: sizes down, sell prices up, buy prices down; the
 * returned {@link Order} shows what was placed.
 */
public interface BotExchange {
    enum Side { BUY, SELL }

    /** OPEN: resting (possibly partially filled); FILLED: fully filled; CANCELLED: cancelled (possibly partially filled). */
    enum OrderStatus { OPEN, FILLED, CANCELLED }

    /**
     * Order snapshot. {@code price}: limit price; fills happen at it or better. {@code filledFunds}: QUOTE value of the
     * filled part; {@code fee} in QUOTE.
     */
    record Order(String orderId, Side side, BigDecimal price, BigDecimal size, BigDecimal filledSize,
                 BigDecimal filledFunds, BigDecimal fee, OrderStatus status) {
        /** Size still waiting to fill; zero once the order is done. */
        public BigDecimal remainingSize() {
            return status == OrderStatus.OPEN ? size.subtract(filledSize) : BigDecimal.ZERO;
        }

        /** Average fill price, or null if nothing filled. */
        public @Nullable BigDecimal averageFillPrice() {
            return filledSize.signum() == 0 ? null
                    : filledFunds.divide(filledSize, MathContext.DECIMAL128);
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

    /** The order is below the exchange's minimum size or value, after rounding; it can't be placed. */
    class OrderTooSmallException extends IOException {
        private static final long serialVersionUID = 1L;

        public OrderTooSmallException(String message) {
            super(message);
        }
    }

    /**
     * Places a limit order without borrowing or repaying.
     *
     * @throws OrderTooSmallException if it's below the exchange's minimum size or value
     */
    Order placeLimitOrder(String symbol, Side side, BigDecimal price, BigDecimal size) throws IOException;

    /** Current state of an order: fills so far and status. */
    Order getOrder(String symbol, String orderId) throws IOException;

    /** Requests cancellation; the final state is seen via {@link #getOrder}. */
    void cancelOrder(String symbol, String orderId) throws IOException;

    /** Identifies the exchange (and account); a bot bound to one name won't run on another. */
    String exchangeName();

    /** Last trade price updates for {@code symbol}. */
    Subscription subscribePrice(String symbol, PriceListener listener) throws IOException;
}
