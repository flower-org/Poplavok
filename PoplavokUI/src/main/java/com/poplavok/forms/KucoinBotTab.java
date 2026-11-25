package com.poplavok.forms;

import com.poplavok.api.kucoin.KucoinApiClient;
import com.poplavok.data.bot.BotExchange;
import com.poplavok.kucoin.KucoinBotExchange;
import com.poplavok.kucoin.TickerPriceService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;

/**
 * Runs a sell-down bot on KuCoin: real orders on the isolated margin account of the loaded API key. Saved as
 * described in {@link BotFile}. When it can't run (no credentials, bound elsewhere) it only shows the bot, with the
 * start button disabled and the reason next to it.
 */
public class KucoinBotTab extends VBox {
    static final long POLL_MILLIS = 2000;

    @FXML @Nullable Circle statusCircle;
    final Tooltip statusTooltip = new Tooltip();
    @FXML @Nullable Button startStopButton;
    @FXML @Nullable Label exchangeLabel;
    @FXML @Nullable Label currentPriceLabel;
    @FXML @Nullable Label messageLabel;
    @FXML @Nullable VBox botPane;

    final File file;
    @Nullable final BotFile botFile;
    @Nullable final KucoinBotExchange exchange;
    @Nullable final SellDownBotEngine bot;
    final SellDownBotTab botTab;
    final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sell-down-bot-kucoin");
        thread.setDaemon(true);
        return thread;
    });
    final LatestValueDispatcher<BigDecimal> shownPrice = new LatestValueDispatcher<>(Platform::runLater, latest -> {
        price = latest;
        refresh();
    });
    final TickerPriceService.PriceListener shownPriceListener = this::onShownPrice;
    @Nullable BigDecimal price;

    /** A bot that runs on {@code client}'s account. */
    public KucoinBotTab(SellDownBotState state, File file, KucoinApiClient client) {
        this(state, file, client, null);
    }

    /** A bot that can't run, because of {@code reason}. */
    public static KucoinBotTab readOnly(SellDownBotState state, File file, String reason) {
        return new KucoinBotTab(state, file, null, reason);
    }

    private KucoinBotTab(SellDownBotState state, File file, @Nullable KucoinApiClient client,
                         @Nullable String readOnlyReason) {
        this.file = file;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("KucoinBotTab.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        botTab = new SellDownBotTab(state);
        VBox.setVgrow(botTab, Priority.ALWAYS);
        checkNotNull(botPane).getChildren().add(botTab);
        if (client != null && readOnlyReason == null) {
            botFile = new BotFile(file, state, this::message);
            exchange = new KucoinBotExchange(client, this::subscribeOnBotThread);
            SellDownBot runnable = new SellDownBot(state, exchange, Clock.systemUTC(), this::onBotChange);
            bot = runnable;
            botTab.setManualBreakEvenSetter(runnable::setManualBreakEvenTrigger);
            checkNotNull(exchangeLabel).setText(KucoinBotExchange.NAME);
        } else {
            botFile = null;
            exchange = null;
            bot = null;
            message("Read-only: " + (readOnlyReason == null ? "no KuCoin client" : readOnlyReason));
        }
        Tooltip.install(checkNotNull(statusCircle), statusTooltip);
        TickerPriceService.getInstance().subscribe(state.definition().symbol(), shownPriceListener);
        refresh();
    }

    /** The bot gets prices on its own thread, never on the websocket's, and only the latest if it falls behind. */
    BotExchange.Subscription subscribeOnBotThread(String symbol, BotExchange.PriceListener listener)
            throws IOException {
        LatestValueDispatcher<BigDecimal> botPrice = new LatestValueDispatcher<>(scheduler, listener::onPrice);
        return KucoinBotExchange.tickerPrices().subscribe(symbol, streamed -> {
            if (streamed.signum() <= 0) {
                return;
            }
            try {
                botPrice.submit(streamed);
            } catch (RejectedExecutionException closed) {
                // tab closed
            }
        });
    }

    void onShownPrice(String symbol, @Nullable BigDecimal streamed) {
        if (streamed != null && streamed.signum() > 0) {
            shownPrice.submit(streamed);
        }
    }

    /** Called by the bot, holding its lock, on every change. */
    void onBotChange(SellDownBotState state) {
        BotFile saver = checkNotNull(botFile);
        saver.changed(state);
        if (!checkNotNull(bot).isRunning()) {
            saver.saveOnExit();
        }
        Platform.runLater(this::refresh);
    }

    void refresh() {
        SellDownBotState state = bot == null ? botTab.state() : bot.state();
        boolean running = bot != null && bot.isRunning();
        botTab.update(state, running, price);
        checkNotNull(currentPriceLabel).setText(price == null ? "No price yet" : "Current: " + formatAmount(price));
        Button button = checkNotNull(startStopButton);
        button.setText(running ? "Stop bot" : "Start bot");
        button.setDisable(bot == null || (!running && state.isFinished()));
        Circle circle = checkNotNull(statusCircle);
        if (running) {
            circle.setFill(Color.LIMEGREEN);
            statusTooltip.setText("Running");
        } else if (state.isFinished()) {
            circle.setFill(Color.RED);
            statusTooltip.setText("Finished");
        } else {
            circle.setFill(Color.ORANGE);
            statusTooltip.setText("Not running");
        }
    }

    /** Asks before an unbound bot gets bound to this KuCoin account for good. */
    boolean confirmBind() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Starting binds this bot to "
                + checkNotNull(exchange).exchangeName()
                + " and places REAL orders there with the loaded API key. After that it will only run on "
                + checkNotNull(exchange).exchangeName() + ". Bind and start?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Bind bot to KuCoin");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    @FXML
    public void startStop() {
        SellDownBotEngine bot = this.bot;
        if (bot == null) {
            return;
        }
        message("");
        if (bot.isRunning()) {
            bot.stop();
            checkNotNull(botFile).saveOnExit();
        } else {
            if (bot.state().exchangeName() == null && !confirmBind()) {
                return;
            }
            try {
                bot.start(scheduler, POLL_MILLIS);
            } catch (IOException | RuntimeException e) {
                message("Start failed: " + e.getMessage());
            }
        }
        refresh();
    }

    /** Stops the bot and its thread and saves the bot file; open orders stay on KuCoin. Call when the tab closes. */
    public void close() {
        TickerPriceService.getInstance().unsubscribe(botTab.state().definition().symbol(), shownPriceListener);
        if (bot != null) {
            bot.stop();
        }
        scheduler.shutdownNow();
        if (botFile != null) {
            botFile.saveOnExit();
        }
    }

    void message(String text) {
        checkNotNull(messageLabel).setText(text);
    }
}
