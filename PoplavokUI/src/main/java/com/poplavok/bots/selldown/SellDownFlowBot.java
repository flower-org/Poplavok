package com.poplavok.bots.selldown;

import com.flower.engine.Flower;
import com.flower.eventbus.PushToPollAdapter;
import com.poplavok.data.bot.BotExchange;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Drop-in replacement for {@link SellDownBot}: each start runs a {@link SellDownFlow} on its own Flower engine over
 * the host's scheduler, and talks to it through push-to-poll adapters (prices, commands) and an
 * {@link OrderStatusPoller}.
 * <p>
 * {@link #state()} and {@link #isRunning()} take no lock, so {@code onChange} may call them. {@link #stop()} waits
 * for the Flow to end, so nothing changes after it returns; it must not be called from the scheduler's thread.
 */
public class SellDownFlowBot implements SellDownBotEngine {
    private static final int COMMAND_CAPACITY = 64;
    private static final long STOP_TIMEOUT_MILLIS = 10_000;

    private final BotExchange exchange;
    /** Times recorded on events, errors and orders. */
    private final Clock timeStamper;
    private final Consumer<SellDownBotState> onChange;
    /** Guards start, stop and changes made while not running. */
    private final Object lifecycle = new Object();
    private volatile SellDownBotState botState;
    /** False from {@link #stop()} on, even while the Flow is still ending. */
    private volatile boolean running;
    /** The current run, until its Flow has ended. */
    private volatile @Nullable Run run;

    @SuppressWarnings("UnusedVariable") // read via accessors
    private record Run(PushToPollAdapter<SellDownFlow.Command> commands, OrderStatusPoller orders,
                       CompletableFuture<Void> closed) {
    }

    public SellDownFlowBot(SellDownBotState botState, BotExchange exchange, Clock timeStamper,
                           Consumer<SellDownBotState> onChange) {
        this.botState = botState;
        this.exchange = exchange;
        this.timeStamper = timeStamper;
        this.onChange = onChange;
    }

    @Override
    public SellDownBotState state() {
        return botState;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void start(ScheduledExecutorService scheduler, long pollMillis) throws IOException {
        synchronized (lifecycle) {
            if (running) {
                return;
            }
            if (run != null) {
                throw new IllegalStateException("The bot is still stopping");
            }
            bind();
            if (SellDownActions.isDone(botState)) {
                return;
            }
            PushToPollAdapter<BigDecimal> prices = new PushToPollAdapter<>(1);
            PushToPollAdapter<SellDownFlow.Command> commands = new PushToPollAdapter<>(COMMAND_CAPACITY);
            OrderStatusPoller orders = new OrderStatusPoller(exchange, botState.definition().symbol(), scheduler);
            // Watched before the first round, so the first snapshot has them
            orders.watch(SellDownActions.openOrderIds(botState));
            BotExchange.Subscription subscription = exchange.subscribePrice(botState.definition().symbol(),
                    prices::accept);
            CompletableFuture<SellDownFlow> flowFuture;
            try {
                Flower flower = new Flower(scheduler);
                flower.registerFlow(SellDownFlow.class);
                flower.initialize();
                SellDownFlow flow = new SellDownFlow(botState, exchange, timeStamper, prices, commands, orders,
                        this::changed);
                running = true;
                Run started = new Run(commands, orders, new CompletableFuture<>());
                run = started;
                flowFuture = flower.getFlowExec(SellDownFlow.class).runFlow(flow).getFuture();
                orders.start(pollMillis);
                CompletableFuture<?> unused = flowFuture.whenComplete((ended, error) -> {
                    subscription.close();
                    prices.close();
                    commands.close();
                    orders.close();
                    ended(ended, error);
                    started.closed().complete(null);
                });
            } catch (RuntimeException e) {
                running = false;
                run = null;
                subscription.close();
                prices.close();
                commands.close();
                orders.close();
                throw e;
            }
        }
    }

    /** Binds an unbound bot to this exchange; refuses one bound to another. */
    private void bind() throws IOException {
        String bound = botState.exchangeName();
        if (bound == null) {
            botState = SellDownActions.bind(botState, exchange.exchangeName(), timeStamper.instant());
            onChange.accept(botState);
        } else if (!bound.equals(exchange.exchangeName())) {
            throw new IOException("bot is bound to " + bound + ", not " + exchange.exchangeName());
        }
    }

    /** Stops the Flow and waits (bounded) until it has ended; open orders stay on the exchange. */
    @Override
    public void stop() {
        Run stopping;
        synchronized (lifecycle) {
            stopping = run;
            if (stopping == null) {
                return;
            }
            running = false;
            stopping.commands().accept(new SellDownFlow.Stop());
        }
        try {
            stopping.closed().get(STOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            // Still ending (e.g. in a slow exchange call); start refuses until it has
        }
    }

    @Override
    public void poll() {
        Run current = run;
        if (current != null && running) {
            current.orders().pollNow();
        }
    }

    /** Validated here; applied by the Flow if running, else right away. */
    @Override
    public void setManualBreakEvenTrigger(@Nullable BigDecimal trigger) {
        synchronized (lifecycle) {
            SellDownActions.checkManualBreakEven(botState, trigger);
            Run current = run;
            if (current != null) {
                if (!running) {
                    throw new IllegalStateException("The bot is still stopping");
                }
                current.commands().accept(new SellDownFlow.SetTrigger(trigger));
                return;
            }
            botState = SellDownActions.setManualBreakEven(botState, trigger, timeStamper.instant());
            onChange.accept(botState);
        }
    }

    /** Called by the Flow (on its thread) each time a Step changes the state. */
    private void changed(SellDownBotState changed) {
        botState = changed;
        onChange.accept(changed);
    }

    /** The Flow has ended: stopped, finished or failed. Not running from here on; then the last change. */
    private void ended(@Nullable SellDownFlow flow, @Nullable Throwable error) {
        if (flow != null) {
            botState = flow.botState;
        }
        if (error != null) {
            Throwable cause = error instanceof CompletionException && error.getCause() != null
                    ? error.getCause() : error;
            botState = SellDownActions.error(botState, timeStamper.instant(), null, "flow", "bot flow failed", cause);
        }
        running = false;
        run = null;
        onChange.accept(botState);
    }
}
