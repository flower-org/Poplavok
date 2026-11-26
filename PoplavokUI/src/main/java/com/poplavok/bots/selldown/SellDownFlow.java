package com.poplavok.bots.selldown;

import com.flower.anno.flow.FlowType;
import com.flower.anno.flow.State;
import com.flower.anno.functions.SimpleStepFunction;
import com.flower.anno.params.common.EventBus;
import com.flower.anno.params.common.In;
import com.flower.anno.params.common.InOut;
import com.flower.anno.params.common.Out;
import com.flower.anno.params.common.Output;
import com.flower.anno.params.transit.StepRef;
import com.flower.anno.params.transit.Terminal;
import com.flower.conf.InOutPrm;
import com.flower.conf.OutPrm;
import com.flower.conf.Transition;
import com.flower.eventbus.FlowEventBus;
import com.flower.eventbus.Pollable;
import com.flower.eventbus.Pollable.Update;
import com.poplavok.bots.selldown.SellDownActions.Outcome;
import com.poplavok.bots.selldown.SellDownBotState.Level;
import com.poplavok.bots.selldown.SellDownBotState.Status;
import com.poplavok.data.bot.BotExchange;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * One run (start to stop or finish) of a sell-down bot, as a Flower Flow. See {@link SellDownFlowBot} for how it's
 * driven, and {@code SellDownFlow.md} for the diagram.
 * <p>
 * Loop: {@code reconcile} (apply order statuses) → {@code decide} (pure: what to do with the state and the last
 * price) → an action Step ({@code placeSells}, {@code cancelSells}, {@code placeBuy}) → {@code awaitEvent}, which
 * waits on the Flow's {@link FlowEventBus} for whichever comes first: a command, a price, or an order status snapshot.
 * <p>
 * Order statuses come from {@link OrderStatuses}, which the Flow tells which orders to watch; how they're had (e.g.
 * polled) is up to it. Each wait future is registered on the bus once, when it's created, and replaced once it has
 * been consumed. Steps that change the state pass it to {@code botStatePersister} (see {@link #commit}). Order
 * actions are exchange calls made directly from the Steps (they block a Flower thread; deferred
 * for now).
 */
// TODO: exchange calls block the host's shared scheduler: the action Steps (placeSells, cancelSells, placeBuy,
//  applyCommands) here, and OrderStatusPoller's getOrder rounds. Make them async or move them to their own executor.
@FlowType(firstStep = "begin", note = "SellDown bot: sells levels as price falls, buys back at break-even")
public class SellDownFlow {
    /** Sent to the Flow by {@link SellDownFlowBot}. */
    sealed interface Command permits SetTrigger, Stop {
    }

    /** Set (or with null, clear) the manual break-even trigger; already validated by the sender. */
    record SetTrigger(@Nullable BigDecimal trigger) implements Command {
    }

    /** End the run; open orders stay on the exchange. */
    record Stop() implements Command {
    }

    /** Times recorded on events, errors and orders. */
    @State final Clock timeStamper;
    @State final BotExchange exchange;

    @State final Pollable<BigDecimal> prices;
    @State @Nullable CompletableFuture<List<Update<BigDecimal>>> priceFuture;
    @State @Nullable BigDecimal lastPrice;

    @State final Pollable<Command> commands;
    @State @Nullable CompletableFuture<List<Update<Command>>> commandFuture;
    @State(initialized = true) List<Command> pendingCommands = List.of();

    @State final OrderStatuses orders;
    @State @Nullable CompletableFuture<List<Update<OrderStatuses.Snapshot>>> orderFuture;
    @State(initialized = true) List<OrderStatuses.Snapshot> pendingOrderSnapshots = List.of();

    /**
     * While true, {@code decide} places no new sells or buys and just waits; it acts again once it knows which
     * orders are open. (Cancelling on finish, or for a new manual trigger, still goes ahead.)
     * <ul>
     *   <li>True at start: the orders left from a previous run aren't known yet.</li>
     *   <li>Set to true when a sell, cancel or buy fails: it may still have reached the exchange, so retrying
     *       right away could place a duplicate order.</li>
     *   <li>Set back to false by {@code reconcile}, once it has applied fresh order statuses.</li>
     * </ul>
     */
    @State boolean holdUntilPoll = true;

    /** Gets the state each time a Step changes it (see {@link #commit}). */
    @State final Consumer<SellDownBotState> botStatePersister;
    @State(initialized = true) SellDownBotState botState;

    SellDownFlow(SellDownBotState botState, BotExchange exchange, Clock timeStamper, Pollable<BigDecimal> prices,
                 Pollable<Command> commands, OrderStatuses orders, Consumer<SellDownBotState> botStatePersister) {
        this.botState = botState;
        this.exchange = exchange;
        this.timeStamper = timeStamper;
        this.prices = prices;
        this.commands = commands;
        this.orders = orders;
        this.botStatePersister = botStatePersister;
    }

    /** Starts watching the inputs; nothing is done before the first order status snapshot. */
    @SimpleStepFunction(note = "initializes eventBus with inputs")
    static Transition begin(@EventBus FlowEventBus bus,

                            @In Pollable<BigDecimal> prices,
                            @In Pollable<Command> commands,
                            @In OrderStatuses orders,

                            @Out OutPrm<CompletableFuture<List<Update<BigDecimal>>>> priceFuture,
                            @Out OutPrm<CompletableFuture<List<Update<Command>>>> commandFuture,
                            @Out OutPrm<CompletableFuture<List<Update<OrderStatuses.Snapshot>>>> orderFuture,

                            @StepRef(note = "eventBus initialized") Transition awaitEvent) {
        CompletableFuture<List<Update<BigDecimal>>> localPriceFuture = prices.poll(0);
        CompletableFuture<List<Update<Command>>> localCommandFuture = commands.poll(0);
        CompletableFuture<List<Update<OrderStatuses.Snapshot>>> localOrderFuture = orders.poll(0);

        priceFuture.setOutValue(localPriceFuture);
        commandFuture.setOutValue(localCommandFuture);
        orderFuture.setOutValue(localOrderFuture);

        bus.register(localCommandFuture);
        bus.register(localPriceFuture);
        bus.register(localOrderFuture);

        return awaitEvent;
    }

    /** Applies order status snapshots, oldest first; a fresh view lifts {@code holdUntilPoll}. */
    @SimpleStepFunction(note = "applies order statuses")
    static Transition reconcile(@InOut InOutPrm<SellDownBotState> botState,
                                @In Consumer<SellDownBotState> botStatePersister,
                                @In List<OrderStatuses.Snapshot> pendingOrderSnapshots,
                                @In Clock timeStamper,
                                @Out OutPrm<Boolean> holdUntilPoll,

                                @StepRef(note = "open orders tracked, hold lifted") Transition decide) {
        SellDownBotState current = botState.getInValue();
        for (OrderStatuses.Snapshot snapshot : pendingOrderSnapshots) {
            current = SellDownActions.apply(current, snapshot, timeStamper.instant());
        }
        commit(botState, current, botStatePersister);
        holdUntilPoll.setOutValue(false);
        return decide;
    }

    /** Pure: picks the next action from the state and the last price. */
    @SimpleStepFunction(note = "pure: picks next action")
    static Transition decide(@In SellDownBotState botState,
                             @In @Nullable BigDecimal lastPrice,
                             @In Boolean holdUntilPoll,

                             @StepRef(note = "finished, no open orders") Transition finish,

                             @StepRef(stepName = "cancelSells", note = "finished, sells to cancel") Transition cancelOnFinish,
                             @StepRef(stepName = "cancelSells", note = "break-even due, sells to cancel") Transition cancelForBreakEven,

                             @StepRef(note = "break-even due, no sells open") Transition placeBuy,

                             @StepRef(note = "PENDING levels triggered") Transition placeSells,

                             @StepRef(stepName = "awaitEvent", note = "cancels pending") Transition awaitCancels,
                             @StepRef(stepName = "awaitEvent", note = "on hold until next poll") Transition awaitHold,
                             @StepRef(stepName = "awaitEvent", note = "break-even buy open") Transition awaitBuyBack,
                             @StepRef(stepName = "awaitEvent", note = "nothing triggered") Transition awaitPrice) {
        if (botState.isFinished()) {
            if (SellDownActions.openLevels(botState).isEmpty()) {
                return finish;
            }
            return SellDownActions.sellsToCancel(botState).isEmpty() ? awaitCancels : cancelOnFinish;
        }
        if (holdUntilPoll) {
            return awaitHold;
        }
        if (SellDownActions.isBuyingBack(botState)) {
            return awaitBuyBack;
        }
        // TODO: break-even isn't latched. Once sells are cancelled for it, a price dip below the trigger makes it not
        //  due any more, and the cancelled levels (PENDING again) can be sold again. Record "break-even triggered" in
        //  the state, so the buy goes ahead whatever the price does meanwhile.
        if (SellDownActions.isBreakEvenDue(botState, lastPrice)) {
            if (!SellDownActions.sellsToCancel(botState).isEmpty()) {
                return cancelForBreakEven;
            }
            // Cancels requested: wait for reconcile to see them closed
            return SellDownActions.hasOpenSells(botState) ? awaitCancels : placeBuy;
        }
        return SellDownActions.triggeredSells(botState, lastPrice).isEmpty() ? awaitPrice : placeSells;
    }

    /** Sells every triggered PENDING level; stops at the first failure. */
    @SimpleStepFunction(note = "exchange: sell triggered levels")
    static Transition placeSells(@InOut InOutPrm<SellDownBotState> botState,
                                 @In Consumer<SellDownBotState> botStatePersister,
                                 @In @Nullable BigDecimal lastPrice,
                                 @In BotExchange exchange,
                                 @In Clock timeStamper,
                                 @Out(out = Output.OPTIONAL) OutPrm<Boolean> holdUntilPoll,

                                 @StepRef(note = "sells placed, or failed: hold") Transition decide) {
        SellDownBotState current = botState.getInValue();
        for (Level triggered : SellDownActions.triggeredSells(current, lastPrice)) {
            Outcome outcome = SellDownActions.sell(current, triggered, exchange, timeStamper.instant());
            current = outcome.state();
            if (outcome.failed()) {
                holdUntilPoll.setOutValue(true);
                break;
            }
        }
        commit(botState, current, botStatePersister);
        return decide;
    }

    /** Asks the exchange to cancel every open sell not asked yet; reconcile sees them closed. */
    @SimpleStepFunction(note = "exchange: cancel open sells")
    static Transition cancelSells(@InOut InOutPrm<SellDownBotState> botState,
                                  @In Consumer<SellDownBotState> botStatePersister,
                                  @In BotExchange exchange,
                                  @In Clock timeStamper,
                                  @Out(out = Output.OPTIONAL) OutPrm<Boolean> holdUntilPoll,

                                  @StepRef(note = "cancels requested") Transition awaitEvent) {
        SellDownBotState current = botState.getInValue();
        for (Level selling : SellDownActions.sellsToCancel(current)) {
            Outcome outcome = SellDownActions.cancel(current, selling, exchange, timeStamper.instant());
            current = outcome.state();
            if (outcome.failed()) {
                holdUntilPoll.setOutValue(true);
            }
        }
        commit(botState, current, botStatePersister);
        return awaitEvent;
    }

    /** Places the break-even buy of the latest SOLD level, at the break-even price as of now. */
    @SimpleStepFunction(note = "exchange: break-even buy")
    static Transition placeBuy(@InOut InOutPrm<SellDownBotState> botState,
                               @In Consumer<SellDownBotState> botStatePersister,
                               @In BotExchange exchange,
                               @In Clock timeStamper,
                               @Out(out = Output.OPTIONAL) OutPrm<Boolean> holdUntilPoll,

                               @StepRef(note = "break-even buy placed, or failed: hold") Transition awaitEvent) {
        Outcome outcome = SellDownActions.buyBack(botState.getInValue(), exchange, timeStamper.instant());
        commit(botState, outcome.state(), botStatePersister);
        if (outcome.failed()) {
            holdUntilPoll.setOutValue(true);
        }
        return awaitEvent;
    }

    /**
     * Watches the open orders, then waits for the first of: commands (Stop ends the run, others are applied), a price
     * (only the latest of a batch counts), or order status snapshots. A closed input (empty batch) ends the run.
     */
    @SimpleStepFunction(note = "watches open orders, waits: command, price or order statuses")
    @SuppressWarnings("FutureReturnValueIgnored") // false positive on next().thenApply
    static CompletableFuture<Transition> awaitEvent(
            @EventBus FlowEventBus bus,
            @In SellDownBotState botState,
            @In Pollable<BigDecimal> prices,
            @In Pollable<Command> commands,
            @In OrderStatuses orders,

            @InOut(out = Output.OPTIONAL) InOutPrm<CompletableFuture<List<Update<BigDecimal>>>> priceFuture,
            @InOut(out = Output.OPTIONAL) InOutPrm<CompletableFuture<List<Update<Command>>>> commandFuture,
            @InOut(out = Output.OPTIONAL) InOutPrm<CompletableFuture<List<Update<OrderStatuses.Snapshot>>>> orderFuture,

            @Out(out = Output.OPTIONAL) OutPrm<BigDecimal> lastPrice,
            @Out(out = Output.OPTIONAL) OutPrm<List<Command>> pendingCommands,
            @Out(out = Output.OPTIONAL) OutPrm<List<OrderStatuses.Snapshot>> pendingOrderSnapshots,

            @StepRef(note = "commands") Transition applyCommands,
            @StepRef(note = "price") Transition decide,
            @StepRef(note = "order statuses") Transition reconcile,
            @Terminal(note = "Stop command") Transition stop,
            @Terminal(note = "input closed") Transition inputClosed) {
        orders.watch(SellDownActions.openOrderIds(botState));
        return bus.next().thenApply(fired -> {
            if (fired == commandFuture.getInValue()) {
                List<Update<Command>> batch = commandFuture.getInValue().join();
                if (batch.isEmpty()) {
                    return inputClosed;
                }
                if (batch.stream().anyMatch(u -> u.value() instanceof Stop)) {
                    return stop;
                }
                commandFuture.setOutValue(watch(bus, commands.poll(lastNo(batch))));
                pendingCommands.setOutValue(batch.stream().map(Update::value).toList());
                return applyCommands;
            }
            if (fired == priceFuture.getInValue()) {
                List<Update<BigDecimal>> batch = priceFuture.getInValue().join();
                if (batch.isEmpty()) {
                    return inputClosed;
                }
                priceFuture.setOutValue(watch(bus, prices.poll(lastNo(batch))));
                BigDecimal latest = batch.get(batch.size() - 1).value();
                if (latest.signum() > 0) {
                    lastPrice.setOutValue(latest);
                }
                return decide;
            }
            if (fired == orderFuture.getInValue()) {
                List<Update<OrderStatuses.Snapshot>> batch = orderFuture.getInValue().join();
                if (batch.isEmpty()) {
                    return inputClosed;
                }
                orderFuture.setOutValue(watch(bus, orders.poll(lastNo(batch))));
                pendingOrderSnapshots.setOutValue(batch.stream().map(Update::value).toList());
                return reconcile;
            }
            throw new IllegalStateException("Unexpected event: " + fired);
        });
    }

    /** Applies SetTrigger commands in order (a set one cancels an open break-even buy). */
    @SimpleStepFunction(note = "applies SetTrigger")
    static Transition applyCommands(@InOut InOutPrm<SellDownBotState> botState,
                                    @In Consumer<SellDownBotState> botStatePersister,
                                    @In List<Command> pendingCommands,
                                    @In BotExchange exchange,
                                    @In Clock timeStamper,
                                    @Out(out = Output.OPTIONAL) OutPrm<Boolean> holdUntilPoll,

                                    @StepRef(note = "triggers set, open buy cancelled") Transition decide) {
        SellDownBotState current = botState.getInValue();
        for (Command command : pendingCommands) {
            if (command instanceof SetTrigger set && !current.isFinished()) {
                current = SellDownActions.setManualBreakEven(current, set.trigger(), timeStamper.instant());
                Level buying = current.levels().stream().filter(l -> l.status() == Status.BUYING_BACK)
                        .findFirst().orElse(null);
                if (buying != null) {
                    Outcome outcome = SellDownActions.cancel(current, buying, exchange, timeStamper.instant());
                    current = outcome.state();
                    if (outcome.failed()) {
                        holdUntilPoll.setOutValue(true);
                    }
                }
            }
        }
        commit(botState, current, botStatePersister);
        return decide;
    }

    /** Records FINISHED and ends the run. */
    @SimpleStepFunction(note = "records FINISHED")
    static Transition finish(@InOut InOutPrm<SellDownBotState> botState,
                             @In Consumer<SellDownBotState> botStatePersister,
                             @In Clock timeStamper,

                             @Terminal(note = "FINISHED recorded") Transition end) {
        commit(botState, SellDownActions.finish(botState.getInValue(), timeStamper.instant()), botStatePersister);
        return end;
    }

    /** Sets a Step's resulting state, and passes it to the persister if it changed. */
    @SuppressWarnings("ReferenceEquality") // states are immutable: unchanged means the same instance
    private static void commit(InOutPrm<SellDownBotState> botState, SellDownBotState next,
                               Consumer<SellDownBotState> botStatePersister) {
        botState.setOutValue(next);
        if (next != botState.getInValue()) {
            botStatePersister.accept(next);
        }
    }

    private static <T> CompletableFuture<T> watch(FlowEventBus bus, CompletableFuture<T> future) {
        bus.register(future);
        return future;
    }

    private static long lastNo(List<? extends Update<?>> batch) {
        return batch.get(batch.size() - 1).updateNo();
    }
}
