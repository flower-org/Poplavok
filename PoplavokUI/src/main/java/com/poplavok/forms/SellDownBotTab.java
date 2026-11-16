package com.poplavok.forms;

import com.poplavok.data.bot.BotExchange;
import com.poplavok.data.utils.LiquidationCalculator;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

    SellDownBotState state;

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
        update(state, false, null);
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

    void showOrderHistory(@Nullable SellDownBotState.BotOrder order) {
        checkNotNull(orderHistoryTable).getItems().setAll(order == null ? List.of() : order.history());
    }

    static String header(SellDownBotState state) {
        return header(state, false);
    }

    static String header(SellDownBotState state, boolean running) {
        SellDownBotState.Definition definition = state.definition();
        return definition.symbol() + " — single sell-down cycle, " + state.levels().size() + " level(s), created "
                + definition.createdAt() + ". " + (state.isFinished() ? "Finished." : running ? "Running." : "Not running.");
    }

    static String fileTitle(File file) {
        String name = file.getName();
        return "Sell-down Bot: " + (name.endsWith(".sdb") ? name.substring(0, name.length() - 4) : name);
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
                        stat(holdings.base()), "Debt ratio now:",
                        ratio == null ? SellDownApproximatorTab.NONE
                                : ratio.movePointRight(2).setScale(2, RoundingMode.CEILING).toPlainString() + "%" },
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
        table.getColumns().add(column("Sell price", level -> amount(plan(level).sellPrice())
                + (plan(level).immediate() ? " (now)" : "")));
        table.getColumns().add(column("Sell trigger price", level -> amount(plan(level).sellTriggerPrice())));
        table.getColumns().add(column("BASE to sell", level -> amount(plan(level).baseToSell())));
        table.getColumns().add(column("Expected QUOTE", level -> amount(plan(level).expectedQuote())));
        table.getColumns().add(column("Break-even price", level -> amount(plan(level).breakEvenPrice())));
        table.getColumns().add(column("Break-even trigger price",
                level -> amount(plan(level).breakEvenTriggerPrice())));
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
        table.getColumns().add(column("Note", event -> event.note() == null ? "" : event.note()));
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
