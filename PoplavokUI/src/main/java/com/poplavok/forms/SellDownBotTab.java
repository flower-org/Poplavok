package com.poplavok.forms;

import com.poplavok.data.bot.BotExchange;
import com.poplavok.data.utils.LiquidationCalculator;
import com.poplavok.forms.SellDownBotState.Status;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;

/** Sell-down bot: shows a bot's definition, level statuses and history; {@link #update} refreshes it. */
public class SellDownBotTab extends VBox {
    @FXML @Nullable Label headerLabel;
    @FXML @Nullable GridPane definitionGrid;
    @FXML @Nullable GridPane currentGrid;
    @FXML @Nullable TableView<SellDownBotState.Level> levelsTable;
    @FXML @Nullable TableView<SellDownBotState.Event> historyTable;
    @FXML @Nullable TableView<SellDownBotState.BotOrder> ordersTable;
    @FXML @Nullable TableView<SellDownBotState.BotOrderEvent> orderHistoryTable;
    @FXML @Nullable VBox breakEvenBox;
    @FXML @Nullable Label breakEvenLabel;
    @FXML @Nullable TextField breakEvenPriceField;
    @FXML @Nullable Button useCurrentPriceButton;
    @FXML @Nullable Label breakEvenProfitLabel;
    @FXML @Nullable Label breakEvenRatioLabel;
    @FXML @Nullable Button setBreakEvenButton;
    @FXML @Nullable Button clearBreakEvenButton;

    SellDownBotState state;
    @Nullable BigDecimal currentPrice;
    boolean running;
    /** Sets (null: clears) a level's manual break-even trigger; null: the panel is hidden. */
    @Nullable Consumer<BigDecimal> manualBreakEvenSetter;

    public SellDownBotTab(SellDownBotState state) {
        this.state = state;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SellDownBotTab.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        initDefinition();
        initTable();
        initHistory();
        initOrders();
        checkNotNull(breakEvenPriceField).textProperty().addListener((observable, previous, text) -> showBreakEvenEffect());
        update(state, false, null);
    }

    /** Shows the break-even panel; {@code setter} sets (null: clears) a level's manual break-even trigger. */
    public void setManualBreakEvenSetter(Consumer<BigDecimal> setter) {
        manualBreakEvenSetter = setter;
        VBox box = checkNotNull(breakEvenBox);
        box.setVisible(true);
        box.setManaged(true);
    }

    public SellDownBotState state() {
        return state;
    }

    /** Shows {@code newState}, current figures at {@code price} (null: the planned price); call on the FX thread. */
    public void update(SellDownBotState newState, boolean running, @Nullable BigDecimal price) {
        state = newState;
        checkNotNull(headerLabel).setText(header(newState, running));
        GridPane current = checkNotNull(currentGrid);
        current.getChildren().clear();
        fillGrid(current, currentCells(newState, price));
        currentPrice = price;
        this.running = running;
        showBreakEven();
        checkNotNull(levelsTable).getItems().setAll(newState.levels());
        checkNotNull(historyTable).getItems().setAll(newState.history());
        TableView<SellDownBotState.BotOrder> orders = checkNotNull(ordersTable);
        SellDownBotState.BotOrder selected = orders.getSelectionModel().getSelectedItem();
        orders.getItems().setAll(newState.orders());
        SellDownBotState.BotOrder reselect = selected == null ? null : newState.order(selected.orderId());
        if (reselect != null) {
            orders.getSelectionModel().select(reselect);
        }
        showOrderHistory(orders.getSelectionModel().getSelectedItem());
    }

    /** Break-even status line, and the buttons enabled for it. */
    void showBreakEven() {
        SellDownBotState.BreakEven breakEven = state.breakEven();
        Label label = checkNotNull(breakEvenLabel);
        boolean stopped = !running && !state.isFinished();
        label.setText((stopped ? STOPPED : "") + breakEvenText(state, breakEven));
        label.setStyle(BADGE + "-fx-background-color: " + (stopped ? GREY : switch (breakEvenOutlook(state, breakEven)) {
            case NONE -> GREY;
            case NO_LOSS -> GREEN;
            case LOSS, OFF -> RED;
        }) + ";");
        boolean editable = !state.isFinished();
        checkNotNull(breakEvenPriceField).setDisable(!editable);
        checkNotNull(useCurrentPriceButton).setDisable(!editable || currentPrice == null);
        checkNotNull(setBreakEvenButton).setDisable(!editable);
        checkNotNull(clearBreakEvenButton).setDisable(!editable || state.manualBreakEvenTrigger() == null);
        showBreakEvenEffect();
    }

    static final String STOPPED = "BOT STOPPED — NOTHING WILL TRIGGER. Once started: ";
    static final String BADGE = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8 4 8; "
            + "-fx-background-radius: 4; ";
    static final String GREEN = "#2e7d32";
    static final String RED = "#c62828";
    static final String GREY = "#757575";

    /** NONE: no break-even yet, or done; NO_LOSS / LOSS: the break-even in use; OFF: automatic one unavailable, no manual. */
    enum Outlook { NONE, NO_LOSS, LOSS, OFF }

    /** Status of the break-even's level N: SOLD (waiting), BUYING_BACK or BROKEN_EVEN. */
    static Status status(SellDownBotState state, SellDownBotState.BreakEven breakEven) {
        return state.levels().get(breakEven.level() - 1).status();
    }

    /** Price of the open break-even buy if there is one, else the price the break-even would buy at. */
    static @Nullable BigDecimal buyPrice(SellDownBotState state, SellDownBotState.BreakEven breakEven) {
        if (status(state, breakEven) == Status.BUYING_BACK) {
            SellDownBotState.BotOrder order = state.lastOrder(breakEven.level());
            if (order != null && order.side() == BotExchange.Side.BUY) {
                return new BigDecimal(order.price());
            }
        }
        return breakEven.price();
    }

    /** BASE gained (or lost) by the break-even buy, against the BASE sold. */
    static String gainText(BigDecimal gain) {
        return (gain.signum() < 0 ? "LOSS " + stat(gain.negate()) : "NO LOSS, +" + stat(gain)) + " BASE";
    }

    /** "buys S BASE for all Q QUOTE at X" */
    static String buyText(SellDownBotState state, SellDownBotState.BreakEven breakEven, BigDecimal price) {
        return "buys " + stat(state.breakEvenSize(breakEven, price)) + " BASE (sold " + stat(breakEven.base())
                + ") for all " + stat(breakEven.quote()) + " QUOTE at " + stat(price);
    }

    static Outlook breakEvenOutlook(SellDownBotState state, @Nullable SellDownBotState.BreakEven breakEven) {
        if (breakEven == null || status(state, breakEven) == Status.BROKEN_EVEN) {
            return Outlook.NONE;
        }
        BigDecimal price = buyPrice(state, breakEven);
        if (price == null) {
            return Outlook.OFF;
        }
        return state.breakEvenGain(breakEven, price).signum() < 0 ? Outlook.LOSS : Outlook.NO_LOSS;
    }

    /** One line: will break-even happen, and at a loss or not. */
    static String breakEvenText(SellDownBotState state, @Nullable SellDownBotState.BreakEven breakEven) {
        if (breakEven == null) {
            String manual = state.manualBreakEvenTrigger();
            return "BREAK-EVEN: NOT YET — starts once level 2 is SOLD"
                    + (manual == null ? "" : "; MANUAL trigger " + stat(new BigDecimal(manual)) + " set");
        }
        String levels = " (levels " + (breakEven.level() - 1) + "-" + breakEven.level() + ")";
        Status status = status(state, breakEven);
        if (status == Status.BROKEN_EVEN) {
            return "BROKEN EVEN" + levels + ": DONE";
        }
        BigDecimal manual = breakEven.manualPrice();
        String kind = manual != null ? "MANUAL BREAK-EVEN (bot-wide), trigger " + stat(checkNotNull(breakEven.manualTrigger()))
                : "AUTO BREAK-EVEN";
        if (status == Status.BUYING_BACK) {
            BigDecimal price = checkNotNull(buyPrice(state, breakEven));
            return kind + levels + ": BUY ORDER OPEN, " + gainText(state.breakEvenGain(breakEven, price)) + " — "
                    + buyText(state, breakEven, price);
        }
        if (manual != null) {
            return kind + levels + ": " + gainText(state.breakEvenGain(breakEven, manual)) + " — "
                    + buyText(state, breakEven, manual) + " once price ≥ " + stat(checkNotNull(breakEven.trigger()));
        }
        if (breakEven.autoUnavailable() != null) {
            return "AUTO BREAK-EVEN" + levels + ": OFF, WON'T TRIGGER — " + breakEven.autoUnavailable()
                    + ". Set a manual price.";
        }
        return "AUTO BREAK-EVEN" + levels + ": ON, " + gainText(state.breakEvenGain(breakEven, breakEven.autoPrice()))
                + " — " + buyText(state, breakEven, breakEven.autoPrice()) + " once price ≥ "
                + stat(breakEven.autoTrigger());
    }

    /** Buy price, P/L and resulting debt ratio for the typed trigger, or for the break-even in use if none is typed. */
    void showBreakEvenEffect() {
        Label profitLabel = checkNotNull(breakEvenProfitLabel);
        Label ratioLabel = checkNotNull(breakEvenRatioLabel);
        SellDownBotState.BreakEven breakEven = state.breakEven();
        BigDecimal trigger = breakEven == null ? null : typedBreakEvenTrigger();
        BigDecimal price = trigger == null || trigger.signum() <= 0 ? null : state.breakEvenPriceForTrigger(trigger);
        if (price == null && breakEven != null && checkNotNull(breakEvenPriceField).getText().isBlank()) {
            price = breakEven.price();
        }
        if (breakEven == null || price == null) {
            profitLabel.setText("P/L: " + SellDownApproximatorTab.NONE);
            profitLabel.setStyle("");
            ratioLabel.setText("Debt ratio after: " + SellDownApproximatorTab.NONE);
            return;
        }
        BigDecimal gain = state.breakEvenGain(breakEven, price);
        profitLabel.setText("Buy price " + stat(price) + ", buys " + stat(state.breakEvenSize(breakEven, price))
                + " BASE, P/L: " + (gain.signum() > 0 ? "+" : "") + stat(gain) + " BASE");
        profitLabel.setStyle(gain.signum() < 0 ? "-fx-text-fill: red; -fx-font-weight: bold;"
                : "-fx-text-fill: green; -fx-font-weight: bold;");
        ratioLabel.setText("Debt ratio after: " + ratioPercent(state.debtRatioAfterBreakEven(breakEven, price)));
    }

    @Nullable BigDecimal typedBreakEvenTrigger() {
        try {
            return new BigDecimal(checkNotNull(breakEvenPriceField).getText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @FXML
    void useCurrentPrice() {
        if (currentPrice != null) {
            checkNotNull(breakEvenPriceField).setText(currentPrice.stripTrailingZeros().toPlainString());
        }
    }

    @FXML
    void setBreakEven() {
        BigDecimal trigger = typedBreakEvenTrigger();
        if (manualBreakEvenSetter == null) {
            return;
        }
        if (trigger == null || trigger.signum() <= 0) {
            error("Enter a positive break-even trigger price.");
            return;
        }
        applyManualBreakEven(trigger);
    }

    @FXML
    void clearBreakEven() {
        if (manualBreakEvenSetter != null) {
            applyManualBreakEven(null);
        }
    }

    void applyManualBreakEven(@Nullable BigDecimal trigger) {
        try {
            checkNotNull(manualBreakEvenSetter).accept(trigger);
        } catch (RuntimeException e) {
            error(String.valueOf(e.getMessage()));
        }
    }

    void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    static String ratioPercent(@Nullable BigDecimal ratio) {
        return ratio == null ? SellDownApproximatorTab.NONE
                : ratio.movePointRight(2).setScale(2, RoundingMode.CEILING).toPlainString() + "%";
    }

    void showOrderHistory(@Nullable SellDownBotState.BotOrder order) {
        checkNotNull(orderHistoryTable).getItems().setAll(order == null ? List.of() : order.history());
    }

    static String header(SellDownBotState state) {
        return header(state, false);
    }

    static String header(SellDownBotState state, boolean running) {
        SellDownBotState.Definition definition = state.definition();
        return definition.symbol() + " — single sell-down cycle, " + state.levels().size() + " level(s), created "
                + definition.createdAt() + ", exchange: "
                + (state.exchangeName() == null ? "not bound yet" : state.exchangeName()) + ". " + (state.isFinished() ? "Finished." : running ? "Running." : "Not running.");
    }

    static String fileTitle(File file) {
        String name = file.getName();
        return "Sell-down Bot: " + (name.endsWith(".bot") ? name.substring(0, name.length() - 4) : name);
    }

    void initDefinition() {
        GridPane grid = checkNotNull(definitionGrid);
        SellDownBotState.Definition definition = state.definition();
        String[][] cells = {
                { "Price:", amount(definition.price()), "Start debt ratio:", percent(definition.startRatio()),
                        "Fill buffer:", percent(definition.fillBuffer()) },
                { "Debt (QUOTE):", amount(definition.debt()), "End debt ratio:", percent(definition.endRatio()),
                        "Slippage (sell now):", percent(definition.slippage()) },
                { "Holding BASE:", amount(definition.base()), "Liquidation debt ratio:",
                        percent(definition.liquidationRatio()), "Maker fee:", percent(definition.makerFee()) },
                { "Holding QUOTE:", amount(definition.quote()), "", "", "Taker fee (sell now):",
                        percent(definition.takerFee()) },
        };
        fillGrid(grid, cells);
    }

    /** Label/value pairs; values bold. */
    static void fillGrid(GridPane grid, String[][] cells) {
        for (int row = 0; row < cells.length; row++) {
            for (int column = 0; column < cells[row].length; column++) {
                Label label = new Label(cells[row][column]);
                if (column % 2 == 1) {
                    label.setStyle("-fx-font-weight: bold;");
                }
                grid.add(label, column, row);
            }
        }
    }

    /** Bot-wide holdings now, and the debt ratio and liquidation price they give (debt unchanged). */
    static String[][] currentCells(SellDownBotState state, @Nullable BigDecimal price) {
        SellDownBotState.Definition definition = state.definition();
        SellDownBotState.Holdings holdings = state.holdings();
        BigDecimal debt = new BigDecimal(definition.debt());
        BigDecimal at = price == null ? new BigDecimal(definition.price()) : price;
        BigDecimal ratio = LiquidationCalculator.debtRatio(debt, true, holdings.base(), holdings.quote(), at);
        BigDecimal liquidation = LiquidationCalculator.priceAtDebtRatio(debt, true, holdings.base(),
                holdings.quote(), new BigDecimal(definition.liquidationRatio()).movePointLeft(2));
        return new String[][] {
                { "Price now:", stat(at) + (price == null ? " (planned)" : ""), "BASE now:",
                        stat(holdings.base()), "Debt ratio now:", ratioPercent(ratio) },
                { "Fees paid (QUOTE):", stat(holdings.fee()), "QUOTE now:", stat(holdings.quote()),
                        "Liquidation price now:",
                        liquidation == null ? SellDownApproximatorTab.NONE : stat(liquidation) },
        };
    }

    /** At most 6 decimals (rounded half-up), at least 2. */
    static String stat(BigDecimal amount) {
        return formatAmount(amount.setScale(STAT_DECIMALS, RoundingMode.HALF_UP));
    }

    static final int STAT_DECIMALS = 6;

    void initTable() {
        TableView<SellDownBotState.Level> table = checkNotNull(levelsTable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<SellDownBotState.Level, String> numberColumn = column("#", level -> String.valueOf(level.number()));
        numberColumn.setPrefWidth(40);
        numberColumn.setMaxWidth(40);
        table.getColumns().add(numberColumn);
        table.getColumns().add(column("Status", level -> level.status().name()));
        table.getColumns().add(column("BASE held", level -> stat(state.holdings(level).base())));
        table.getColumns().add(column("QUOTE held", level -> stat(state.holdings(level).quote())));
        table.getColumns().add(column("Fee paid (QUOTE)", level -> stat(state.holdings(level).fee())));
        // Plan columns: the level's definition
        table.getColumns().add(column("Target ratio", level -> percent(plan(level).targetRatio())));
        table.getColumns().add(column("Sell price", level -> amount(plan(level).sellPrice())));
        table.getColumns().add(column("Sell trigger price", level -> amount(plan(level).sellTriggerPrice())));
        table.getColumns().add(column("BASE to sell", level -> amount(plan(level).baseToSell())));
        table.getColumns().add(column("Expected QUOTE", level -> amount(plan(level).expectedQuote())));
        table.getColumns().add(column("Planned break-even price", level -> amount(plan(level).breakEvenPrice())));
        table.getColumns().add(column("Planned break-even trigger",
                level -> amount(plan(level).breakEvenTriggerPrice())));
        TableColumn<SellDownBotState.Level, String> breakEvenColumn = column("Break-even now", this::breakEvenCell);
        breakEvenColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                SellDownBotState.BreakEven breakEven = state.breakEven();
                boolean breakEvenRow = !empty && breakEven != null && getIndex() >= 0
                        && getIndex() < getTableView().getItems().size()
                        && getTableView().getItems().get(getIndex()).number() == breakEven.level();
                setStyle(!breakEvenRow ? "" : "-fx-font-weight: bold; -fx-text-fill: "
                        + (breakEvenOutlook(state, breakEven) == Outlook.NO_LOSS ? GREEN : RED) + ";");
            }
        });
        table.getColumns().add(breakEvenColumn);
    }

    static final String AUTO_UNAVAILABLE = "⚠ AUTO OFF";

    /** For the level the break-even applies to: the price in use, or a warning; empty for others. */
    String breakEvenCell(SellDownBotState.Level level) {
        SellDownBotState.BreakEven breakEven = state.breakEven();
        if (breakEven == null || breakEven.level() != level.number()) {
            return "";
        }
        Status status = status(state, breakEven);
        if (status == Status.BROKEN_EVEN) {
            return "done";
        }
        String loss = breakEvenOutlook(state, breakEven) == Outlook.LOSS ? " (LOSS)" : " (no loss)";
        if (status == Status.BUYING_BACK) {
            return "buying at " + stat(checkNotNull(buyPrice(state, breakEven))) + loss;
        }
        if (breakEven.manualPrice() != null) {
            return "manual: trigger " + stat(checkNotNull(breakEven.manualTrigger())) + ", buy " + stat(breakEven.manualPrice())
                    + loss;
        }
        return breakEven.autoUnavailable() != null ? AUTO_UNAVAILABLE : "auto " + stat(breakEven.autoPrice())
                + " (no loss)";
    }

    void initHistory() {
        TableView<SellDownBotState.Event> table = checkNotNull(historyTable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("Time", SellDownBotState.Event::time));
        table.getColumns().add(column("Action", event -> event.type().name()));
        table.getColumns().add(column("Level", event -> event.level() == null ? SellDownApproximatorTab.NONE
                : String.valueOf(event.level())));
        table.getColumns().add(column("Price", event -> amount(event.price())));
        table.getColumns().add(column("BASE", event -> amount(event.base())));
        table.getColumns().add(column("QUOTE", event -> amount(event.quote())));
        table.getColumns().add(column("Order", event -> event.orderId() == null ? SellDownApproximatorTab.NONE
                : event.orderId()));
        table.getColumns().add(column("Note", SellDownBotTab::historyNote));
    }

    /** Note column: for an ERROR event, its counts, time span and latest message. */
    static String historyNote(SellDownBotState.Event event) {
        String note = event.note() == null ? "" : event.note();
        if (event.errors() == null) {
            return note;
        }
        return event.errorCount() + " error(s): " + event.errorSummary()
                + (event.lastTime() == null || event.lastTime().equals(event.time()) ? "" : ", until " + event.lastTime())
                + ". Latest: " + note;
    }

    SellDownBotState.LevelPlan plan(SellDownBotState.Level level) {
        return state.plan(level);
    }

    void initOrders() {
        TableView<SellDownBotState.BotOrder> table = checkNotNull(ordersTable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("Order", SellDownBotState.BotOrder::orderId));
        table.getColumns().add(column("Level", order -> String.valueOf(order.level())));
        table.getColumns().add(column("Side", order -> order.side().name()));
        table.getColumns().add(column("Status", order -> order.status().name()
                + (order.isCancelRequested() && order.status() == BotExchange.OrderStatus.OPEN ? " (cancelling)" : "")));
        table.getColumns().add(column("Price", order -> amount(order.price())));
        table.getColumns().add(column("Size (BASE)", order -> amount(order.size())));
        table.getColumns().add(column("Filled (BASE)", order -> amount(order.filled())));
        table.getColumns().add(column("Remaining (BASE)", order -> formatAmount(order.remaining())));
        table.getColumns().add(column("Avg fill price", order -> order.averageFillPrice() == null
                ? SellDownApproximatorTab.NONE : formatAmount(order.averageFillPrice())));
        table.getColumns().add(column("Filled QUOTE", order -> amount(order.filledQuote())));
        table.getColumns().add(column("Fee (QUOTE)", order -> amount(order.fee())));
        table.getColumns().add(column("Placed", SellDownBotState.BotOrder::placedAt));
        table.getColumns().add(column("Closed", order -> order.closedAt() == null ? SellDownApproximatorTab.NONE
                : order.closedAt()));
        table.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showOrderHistory(selected));

        TableView<SellDownBotState.BotOrderEvent> history = checkNotNull(orderHistoryTable);
        history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        history.setPlaceholder(new Label("Select an order"));
        history.getColumns().add(column("Time", SellDownBotState.BotOrderEvent::time));
        history.getColumns().add(column("Event", event -> event.type().name()));
        history.getColumns().add(column("Status", event -> event.status().name()));
        history.getColumns().add(column("Filled (BASE)", event -> amount(event.filled())));
        history.getColumns().add(column("Filled QUOTE", event -> amount(event.filledQuote())));
        history.getColumns().add(column("Fee (QUOTE)", event -> amount(event.fee())));
    }

    static <T> TableColumn<T, String> column(String title, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setSortable(false);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    static String amount(@Nullable String value) {
        return value == null ? SellDownApproximatorTab.NONE : formatAmount(new BigDecimal(value));
    }

    static String percent(@Nullable String value) {
        return value == null ? SellDownApproximatorTab.NONE : value + "%";
    }
}
