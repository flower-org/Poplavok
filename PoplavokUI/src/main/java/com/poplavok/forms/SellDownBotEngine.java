package com.poplavok.forms;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A sell-down bot as its hosts (the KuCoin and simulated exchange tabs) drive it. Implementations run one sell-down
 * cycle of a {@link SellDownBotState} on a {@link com.poplavok.data.bot.BotExchange} and report every state change to
 * an {@code onChange} callback given at construction.
 * <p>
 * Contract hosts rely on:
 * <ul>
 * <li>Methods are thread-safe; {@code onChange} may be called while the bot holds its lock, so it must not call back
 * into the bot.</li>
 * <li>The state is immutable; every change replaces it with a new {@link SellDownBotState}.</li>
 * <li>On finishing, the bot stops itself before its last {@code onChange}, so {@link #isRunning()} is then false.</li>
 * </ul>
 */
public interface SellDownBotEngine {
    /** The bot's current state. */
    SellDownBotState state();

    /** True between {@link #start} and {@link #stop} (or the bot finishing on its own). */
    boolean isRunning();

    /**
     * Binds an unbound bot to the exchange, subscribes to the price and polls open orders every {@code pollMillis} on
     * {@code scheduler}. Does nothing if already running.
     *
     * @throws IOException if the bot is bound to another exchange or the price subscription fails
     */
    void start(ScheduledExecutorService scheduler, long pollMillis) throws IOException;

    /** Stops reacting; open orders stay on the exchange and are picked up again on the next start. */
    void stop();

    /** Checks open orders and acts on the last price now; hosts call it to react to a change without waiting. */
    void poll();

    /**
     * Sets the bot-wide hand-set break-even trigger, overriding every level's planned break-even; null clears it.
     *
     * @throws IllegalStateException    if the bot is finished
     * @throws IllegalArgumentException if {@code trigger} isn't positive
     */
    void setManualBreakEvenTrigger(@Nullable BigDecimal trigger);
}
