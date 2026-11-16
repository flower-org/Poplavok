package com.poplavok.forms;

import com.poplavok.data.bot.BotExchange;
import com.poplavok.data.bot.SimulatedExchange;
import com.poplavok.kucoin.TickerPriceService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;

/**
 * Runs a sell-down bot against a {@link SimulatedExchange} the user operates: set the price, then fill (partially or
 * fully, at the limit price or better) or cancel the bot's orders. The bot form above shows how the bot reacts.
 * While the bot runs, every change is saved to {@code <file>.bak}; on exit (stop, finish, tab close) the bot file is
 * overwritten and the backup deleted. So a backup found when loading means the bot didn't shut down cleanly.
 */
public class SimulatedExchangeTab extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatedExchangeTab.class);
    static final long POLL_MILLIS = 1000;
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    @FXML @Nullable Button startStopButton;
    @FXML @Nullable Circle statusCircle;
    final Tooltip statusTooltip = new Tooltip();
    @FXML @Nullable TextField priceField;
    @FXML @Nullable Label currentPriceLabel;
    @FXML @Nullable CheckBox livePriceBox;
    @FXML @Nullable Button sendPriceButton;
    /** Forwards KuCoin's streamed price to the simulated exchange (and the price field) while "live" is checked. */
    final TickerPriceService.PriceListener livePriceListener = this::onLivePrice;
    @FXML @Nullable CheckBox failPlaceBox;
    @FXML @Nullable CheckBox failPollBox;
    @FXML @Nullable CheckBox failCancelBox;
    @FXML @Nullable VBox botPane;
    @FXML @Nullable TableView<BotExchange.Order> ordersTable;
    @FXML @Nullable Label pollsLabel;
    @FXML @Nullable TableView<SimulatedExchange.Poll> pollsTable;
    @FXML @Nullable TextField fillSizeField;
    @FXML @Nullable TextField fillPriceField;
    @FXML @Nullable Label messageLabel;

    final SimulatedExchange exchange;
    /** Order whose limit price was last put into the fill price field. */
    @Nullable String fillPriceOrderId;
    final File file;
    /** Bot's latest state, so saving never takes the bot's lock (the bot holds it when calling us). */
    SellDownBotState latest;
    /** True if {@code latest} isn't written to the bot file yet. */
    boolean unsaved;
    final SellDownBot bot;
    final SellDownBotTab botTab;
    final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sell-down-bot-simulation");
        thread.setDaemon(true);
        return thread;
    });

    public SimulatedExchangeTab(SellDownBotState state, File file) {
        this.file = file;
        this.latest = state;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SimulatedExchangeTab.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        exchange = new SimulatedExchange(new BigDecimal(state.definition().makerFee()).movePointLeft(2));
        state.orders().forEach(order -> exchange.restore(order.toExchangeOrder()));
        botTab = new SellDownBotTab(state);
        VBox.setVgrow(botTab, Priority.ALWAYS);
        checkNotNull(botPane).getChildren().add(botTab);
        bot = new SellDownBot(state, exchange, Clock.systemUTC(), this::onBotChange);
        botTab.setManualBreakEvenSetter(bot::setManualBreakEvenTrigger);
        exchange.addChangeListener(() -> Platform.runLater(this::refreshOrders));

        initFailures();
        initOrdersTable();
        initPollsTable();
        checkNotNull(priceField).setText(state.definition().price());
        Tooltip.install(checkNotNull(statusCircle), statusTooltip);
        refreshBot();
        refreshOrders();
    }

    static File backupFile(File file) {
        return new File(file.getPath() + ".bak");
    }

    /** Called by the bot, holding its lock, on every change. */
    void onBotChange(SellDownBotState state) {
        synchronized (this) {
            latest = state;
            unsaved = true;
            write(state, backupFile(file));
        }
        if (!bot.isRunning()) {
            saveOnExit();
        }
        Platform.runLater(this::refreshBot);
    }

    /** Overwrites the bot file with the bot's state (if it changed), then deletes the backup. */
    synchronized void saveOnExit() {
        if (unsaved && write(latest, file)) {
            unsaved = false;
        }
        File backup = backupFile(file);
        if (!unsaved && backup.exists() && !backup.delete()) {
            LOGGER.error("Failed to delete {}", backup);
            Platform.runLater(() -> message("Couldn't delete " + backup.getName()));
        }
    }

    boolean write(SellDownBotState state, File target) {
        try {
            state.write(target);
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save sell-down bot to {}", target, e);
            Platform.runLater(() -> message("Save to " + target.getName() + " failed: " + e.getMessage()));
            return false;
        }
    }

    static String title(File file) {
        String name = file.getName();
        return "Simulation: " + (name.endsWith(".bot") ? name.substring(0, name.length() - 4) : name);
    }

    /** Each checkbox makes every such bot call fail while checked. */
    void initFailures() {
        Map<CheckBox, SimulatedExchange.Failure> boxes = Map.of(checkNotNull(failPlaceBox),
                SimulatedExchange.Failure.PLACE, checkNotNull(failPollBox), SimulatedExchange.Failure.POLL,
                checkNotNull(failCancelBox), SimulatedExchange.Failure.CANCEL);
        boxes.forEach((box, failure) -> box.selectedProperty().addListener(
                (observable, previous, selected) -> exchange.setFailing(failure, selected)));
    }

    void initOrdersTable() {
        TableView<BotExchange.Order> table = checkNotNull(ordersTable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(SellDownBotTab.column("Order", BotExchange.Order::orderId));
        table.getColumns().add(SellDownBotTab.column("Level", order -> levelOf(bot.state(), order.orderId())));
        table.getColumns().add(SellDownBotTab.column("Side", order -> order.side().name()));
        table.getColumns().add(SellDownBotTab.column("Status", order -> order.status().name()));
        table.getColumns().add(SellDownBotTab.column("Price", order -> formatAmount(order.price())));
        table.getColumns().add(SellDownBotTab.column("Size (BASE)", order -> formatAmount(order.size())));
        table.getColumns().add(SellDownBotTab.column("Filled (BASE)", order -> formatAmount(order.filledSize())));
        table.getColumns().add(SellDownBotTab.column("Remaining (BASE)",
                order -> formatAmount(order.remainingSize())));
        table.getColumns().add(SellDownBotTab.column("Avg fill price", order -> order.averageFillPrice() == null
                ? SellDownApproximatorTab.NONE : formatAmount(order.averageFillPrice())));
        table.getColumns().add(SellDownBotTab.column("Filled QUOTE", order -> formatAmount(order.filledFunds())));
        table.getColumns().add(SellDownBotTab.column("Fee (QUOTE)", order -> formatAmount(order.fee())));
        // Table reloads re-select the same order; only a different order resets the fill price
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            if (selected != null && !selected.orderId().equals(fillPriceOrderId)) {
                fillPriceOrderId = selected.orderId();
                checkNotNull(fillPriceField).setText(selected.price().stripTrailingZeros().toPlainString());
            }
        });
    }

    void initPollsTable() {
        checkNotNull(pollsLabel).setText("Order polls (latest " + SimulatedExchange.MAX_POLLS + ")");
        TableView<SimulatedExchange.Poll> table = checkNotNull(pollsTable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(SellDownBotTab.column("Time", poll -> TIME.format(poll.time())));
        table.getColumns().add(SellDownBotTab.column("Order", poll -> poll.order().orderId()));
        table.getColumns().add(SellDownBotTab.column("Status",
                poll -> poll.failed() ? "FAILED (injected)" : poll.order().status().name()));
        table.getColumns().add(SellDownBotTab.column("Filled (BASE)",
                poll -> formatAmount(poll.order().filledSize())));
    }

    /** Level number the bot placed {@code orderId} for, from its history. */
    static String levelOf(SellDownBotState state, String orderId) {
        return state.history().stream()
                .filter(event -> event.type() == SellDownBotState.EventType.ORDER_PLACED
                        && orderId.equals(event.orderId()) && event.level() != null)
                .map(event -> String.valueOf(event.level()))
                .findFirst().orElse(SellDownApproximatorTab.NONE);
    }

    void refreshBot() {
        botTab.update(bot.state(), bot.isRunning(), exchange.price());
        checkNotNull(startStopButton).setText(bot.isRunning() ? "Stop bot" : "Start bot");
        checkNotNull(startStopButton).setDisable(!bot.isRunning() && bot.state().isFinished());
        Circle circle = checkNotNull(statusCircle);
        if (bot.isRunning()) {
            circle.setFill(Color.LIMEGREEN);
            statusTooltip.setText("Running");
        } else if (bot.state().isFinished()) {
            circle.setFill(Color.RED);
            statusTooltip.setText("Finished");
        } else {
            circle.setFill(Color.ORANGE);
            statusTooltip.setText("Not running");
        }
        checkNotNull(ordersTable).refresh();
    }

    void refreshOrders() {
        TableView<BotExchange.Order> table = checkNotNull(ordersTable);
        String selectedId = selectedOrderId();
        List<BotExchange.Order> orders = exchange.orders();
        table.getItems().setAll(orders);
        orders.stream().filter(order -> order.orderId().equals(selectedId)).findFirst()
                .ifPresent(order -> table.getSelectionModel().select(order));
        checkNotNull(pollsTable).getItems().setAll(exchange.polls());
        BigDecimal price = exchange.price();
        checkNotNull(currentPriceLabel).setText(price == null ? "No price yet" : "Current: " + formatAmount(price));
        botTab.update(bot.state(), bot.isRunning(), price);
    }

    /** Asks before an unbound bot gets bound to this exchange for good. */
    boolean confirmBind() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Starting binds this bot to the "
                + exchange.exchangeName() + " exchange. After that it will only run on " + exchange.exchangeName()
                + ". Bind and start?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Bind bot to exchange");
        alert.setHeaderText(null);
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    @FXML
    public void startStop() {
        message("");
        if (bot.isRunning()) {
            bot.stop();
            saveOnExit();
        } else {
            if (bot.state().exchangeName() == null && !confirmBind()) {
                return;
            }
            try {
                bot.start(scheduler, POLL_MILLIS);
            } catch (IOException e) {
                message("Start failed: " + e.getMessage());
            }
        }
        refreshBot();
    }

    @FXML
    public void toggleLivePrice() {
        boolean live = checkNotNull(livePriceBox).isSelected();
        checkNotNull(priceField).setDisable(live);
        checkNotNull(sendPriceButton).setDisable(live);
        if (live) {
            TickerPriceService.getInstance().subscribe(bot.state().definition().symbol(), livePriceListener);
        } else {
            stopLivePrice();
        }
    }

    /** Streamed on the websocket thread; the bot reacts on its own thread. */
    void onLivePrice(String symbol, @Nullable BigDecimal price) {
        if (price != null && price.signum() > 0) {
            try {
                scheduler.execute(() -> exchange.setPrice(price));
                Platform.runLater(() -> {
                    if (checkNotNull(livePriceBox).isSelected()) {
                        checkNotNull(priceField).setText(price.toPlainString());
                    }
                });
            } catch (RejectedExecutionException closed) {
                // tab closed
            }
        }
    }

    void stopLivePrice() {
        TickerPriceService.getInstance().unsubscribe(bot.state().definition().symbol(), livePriceListener);
    }

    @FXML
    public void setPrice() {
        message("");
        try {
            exchange.setPrice(new BigDecimal(checkNotNull(priceField).getText().trim()));
        } catch (RuntimeException e) {
            message("Price: " + e.getMessage());
        }
    }

    @FXML
    public void fill() {
        String orderId = requireSelected();
        if (orderId == null) {
            return;
        }
        try {
            String fillPrice = checkNotNull(fillPriceField).getText().trim();
            exchange.fill(orderId, new BigDecimal(checkNotNull(fillSizeField).getText().trim()),
                    fillPrice.isEmpty() ? null : new BigDecimal(fillPrice));
            pollSoon();
        } catch (RuntimeException e) {
            message("Fill: " + e.getMessage());
        }
    }

    /** Puts the selected order's remaining size into the fill size field. */
    @FXML
    public void max() {
        message("");
        BotExchange.Order selected = checkNotNull(ordersTable).getSelectionModel().getSelectedItem();
        if (selected == null) {
            message("Select an order");
            return;
        }
        checkNotNull(fillSizeField).setText(selected.remainingSize().stripTrailingZeros().toPlainString());
    }

    @FXML
    public void cancelOrder() {
        String orderId = requireSelected();
        if (orderId == null) {
            return;
        }
        try {
            exchange.cancel(orderId);
            pollSoon();
        } catch (RuntimeException e) {
            message("Cancel: " + e.getMessage());
        }
    }

    /** Stops the bot and its thread and saves the bot file; call when the tab closes. */
    public void close() {
        stopLivePrice();
        bot.stop();
        scheduler.shutdownNow();
        saveOnExit();
    }

    /** Lets a running bot see the change right away instead of on its next poll. */
    void pollSoon() {
        if (bot.isRunning()) {
            scheduler.execute(bot::poll);
        }
    }

    @Nullable String selectedOrderId() {
        BotExchange.Order selected = checkNotNull(ordersTable).getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.orderId();
    }

    @Nullable String requireSelected() {
        message("");
        String orderId = selectedOrderId();
        if (orderId == null) {
            message("Select an order");
        }
        return orderId;
    }

    void message(String text) {
        checkNotNull(messageLabel).setText(text);
    }
}
