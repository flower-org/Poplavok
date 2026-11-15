package com.poplavok.forms;

import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.utils.LiquidationCalculator;
import com.poplavok.data.utils.SellDownPlanner;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.flower.fxutils.JavaFxUtils.createDecimalTextFormatter;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;

/** Sell-down approximator: plans equal BASE chunk sells at debt ratio levels so a long never gets liquidated. */
public class SellDownApproximatorTab extends VBox {
    static final Logger LOGGER = LoggerFactory.getLogger(SellDownApproximatorTab.class);
    static final String NONE = "—";
    static final String DANGER_STYLE = "-fx-background-color: #ffe0b3; -fx-text-background-color: black;";
    static final String DANGER_SELECTED_STYLE = "-fx-background-color: #ffb84d; -fx-text-background-color: black;";
    static final String CANT_CONTINUE_STYLE = "-fx-background-color: #ffd6d6; -fx-text-background-color: black;";
    static final String CANT_CONTINUE_SELECTED_STYLE =
            "-fx-background-color: #ff8a8a; -fx-text-background-color: black;";
    static final String ACCENTED_COLUMN_STYLE = "-fx-font-weight: bold; -fx-border-color: transparent #2f6fd0;"
            + " -fx-border-width: 0 2 0 2;";
    static final BigDecimal DEFAULT_START_PERCENT = new BigDecimal("85");

    @FXML @Nullable TickerPricePicker tickerPricePicker;
    @FXML @Nullable Label debtLabel;
    @FXML @Nullable Label baseLabel;
    @FXML @Nullable Label quoteLabel;
    @FXML @Nullable TextField debtTextField;
    @FXML @Nullable TextField baseTextField;
    @FXML @Nullable TextField quoteTextField;
    @FXML @Nullable TextField startRatioTextField;
    @FXML @Nullable CheckBox startRatioCurrentCheckBox;
    @FXML @Nullable TextField endRatioTextField;
    @FXML @Nullable TextField liquidationRatioTextField;
    @FXML @Nullable TextField levelsTextField;
    @FXML @Nullable TextField fillBufferTextField;
    @FXML @Nullable TextField slippageTextField;
    @FXML @Nullable TextField makerFeeTextField;
    @FXML @Nullable TextField takerFeeTextField;
    @FXML @Nullable Label startStateLabel;
    @FXML @Nullable TableView<SellDownPlanner.Level> levelsTable;
    @FXML @Nullable Label summaryLabel;

    /** Opens a new approximator tab (content, title); used by "Break even from here". */
    final BiConsumer<SellDownApproximatorTab, String> openTab;
    @Nullable SellDownPlanner.Params lastParams;
    @Nullable SellDownPlanner.Plan lastPlan;
    /** Tab title; becomes "Sell-down: {file name}" after saving/loading. */
    final StringProperty title = new SimpleStringProperty("Sell-down Approximator");
    /** Folder of the last saved/loaded approximation, shared by all approximator tabs. */
    static @Nullable File lastDirectory;

    public SellDownApproximatorTab(BiConsumer<SellDownApproximatorTab, String> openTab) {
        this.openTab = openTab;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SellDownApproximatorTab.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        for (TextField input : new TextField[] { checkNotNull(debtTextField),
                checkNotNull(baseTextField), checkNotNull(quoteTextField), checkNotNull(startRatioTextField),
                checkNotNull(endRatioTextField), checkNotNull(liquidationRatioTextField),
                checkNotNull(fillBufferTextField), checkNotNull(slippageTextField), checkNotNull(makerFeeTextField),
                checkNotNull(takerFeeTextField) }) {
            input.setTextFormatter(createDecimalTextFormatter());
        }
        checkNotNull(tickerPricePicker).selectedTickerProperty()
                .addListener((observable, oldValue, newValue) -> {
                    updateCurrencyLabels();
                    updateFee(newValue);
                });
        checkNotNull(startRatioCurrentCheckBox).selectedProperty()
                .addListener((observable, oldValue, newValue) -> updateStartRatioMode());
        for (TextField input : new TextField[] { checkNotNull(tickerPricePicker).priceTextField(),
                checkNotNull(debtTextField), checkNotNull(baseTextField), checkNotNull(quoteTextField) }) {
            input.textProperty().addListener((observable, oldValue, newValue) -> {
                if (checkNotNull(startRatioCurrentCheckBox).isSelected()) {
                    updateStartRatio();
                }
            });
        }
        updateStartRatioMode();
        initTable();
    }

    /** While "current" is checked the start debt ratio follows the current debt ratio and is read only. */
    void updateStartRatioMode() {
        boolean current = checkNotNull(startRatioCurrentCheckBox).isSelected();
        TextField field = checkNotNull(startRatioTextField);
        field.setEditable(!current);
        field.setStyle(current ? TickerPricePicker.READ_ONLY_STYLE : "");
        if (current) {
            updateStartRatio();
        }
    }

    /** Start debt ratio = current debt ratio, rounded up. */
    void updateStartRatio() {
        BigDecimal price = parse(checkNotNull(tickerPricePicker).priceTextField());
        BigDecimal debt = parse(debtTextField);
        BigDecimal base = parse(baseTextField);
        BigDecimal quote = parse(quoteTextField);
        if (price.signum() <= 0 || debt.signum() <= 0 || base.signum() < 0 || quote.signum() < 0) {
            return;
        }
        BigDecimal ratio = LiquidationCalculator.debtRatio(debt, true, base, quote, price);
        if (ratio != null) {
            checkNotNull(startRatioTextField).setText(
                    ratio.movePointRight(2).setScale(2, RoundingMode.CEILING).stripTrailingZeros().toPlainString());
        }
    }

    void updateCurrencyLabels() {
        TickerPricePicker picker = checkNotNull(tickerPricePicker);
        String base = picker.baseCurrency();
        String quote = picker.quoteCurrency();
        checkNotNull(debtLabel).setText("Debt (" + quote + "):");
        checkNotNull(baseLabel).setText("Holding " + base + ":");
        checkNotNull(quoteLabel).setText("Holding " + quote + ":");
    }

    /**
     * Resting sell orders pay the pair's maker fee, levels already passed are sold now and pay the taker fee
     * (rate x coefficient).
     */
    void updateFee(@Nullable MarketTicker ticker) {
        if (ticker == null) {
            return;
        }
        try {
            checkNotNull(makerFeeTextField).setText(asPercent(ticker.getMakerFee()));
            checkNotNull(takerFeeTextField).setText(asPercent(ticker.getTakerFee()));
        } catch (RuntimeException e) {
            LOGGER.warn("No fees for {}; keeping the entered fees", ticker.getSymbol(), e);
        }
    }

    static String asPercent(BigDecimal fraction) {
        return fraction.movePointRight(2).stripTrailingZeros().toPlainString();
    }

    /** Stops price streaming. Safe to call multiple times. */
    public void dispose() {
        checkNotNull(tickerPricePicker).dispose();
    }

    void initTable() {
        TableView<SellDownPlanner.Level> table = checkNotNull(levelsTable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<SellDownPlanner.Level, String> numberColumn = column("#", level -> String.valueOf(level.number()));
        numberColumn.setPrefWidth(40);
        numberColumn.setMaxWidth(40);
        table.getColumns().add(numberColumn);
        table.getColumns().add(column("Target ratio", level -> percent(level.targetRatio())));
        table.getColumns().add(column("Ratio after", level -> percent(level.ratioAfter())));
        table.getColumns().add(column("Ratio hit at", level -> formatAmount(level.ratioPrice())));
        table.getColumns().add(column("Sell price", level -> formatAmount(level.sellPrice())
                + (level.immediate() ? " (now)" : "")));
        table.getColumns().add(column("Sell trigger price", level -> formatAmount(level.sellTriggerPrice())));
        table.getColumns().add(column("BASE sold", level -> formatAmount(level.baseSold())));
        table.getColumns().add(column("QUOTE received", level -> formatAmount(level.quoteReceived())));
        table.getColumns().add(column("BASE left", level -> formatAmount(level.baseLeft())));
        table.getColumns().add(column("QUOTE after", level -> formatAmount(level.quoteAfter())));
        table.getColumns().add(column("Break-even price", level -> level.breakEvenPrice() == null ? NONE
                : formatAmount(level.breakEvenPrice())));
        table.getColumns().add(column("Break-even trigger price", level -> priceWithDrop(
                level.breakEvenTriggerPrice(), level.sellPrice())));
        table.getColumns().add(column("BASE if break even", level -> {
            SellDownPlanner.BreakEven be = breakEven(level);
            return be == null ? NONE : formatAmount(be.base());
        }));
        table.getColumns().add(accented(column("Ratio if break even", level -> {
            SellDownPlanner.BreakEven be = breakEven(level);
            return be == null ? NONE : percent(breakEvenRatio(be));
        })));
        table.getColumns().add(accented(column("Liq. if break even", level -> {
            SellDownPlanner.BreakEven be = breakEven(level);
            SellDownPlanner.Params params = lastParams;
            return be == null || params == null ? NONE
                    : priceWithDrop(breakEvenLiqPrice(be, params.liquidationRatio()), be.price());
        })));
        table.getColumns().add(column("Liq. if unfilled", level -> priceWithDrop(level.liqPriceIfUnfilled(),
                level.sellPrice())));
        table.getColumns().add(column("Liq. after", level -> priceWithDrop(level.liqPriceAfter(), level.sellPrice())));
        table.setRowFactory(view -> {
            TableRow<SellDownPlanner.Level> row = new TableRow<>() {
                @Override
                protected void updateItem(@Nullable SellDownPlanner.Level level, boolean empty) {
                    super.updateItem(level, empty);
                    restyle();
                }

                {
                    selectedProperty().addListener((observable, oldValue, newValue) -> restyle());
                }

                private void restyle() {
                    SellDownPlanner.Level level = getItem();
                    setStyle(level == null || isEmpty() ? "" : rowStyle(level, isSelected()));
                }
            };
            MenuItem breakEvenItem = new MenuItem("Break even from here");
            breakEvenItem.setOnAction(event -> {
                SellDownPlanner.Level level = row.getItem();
                if (level != null) {
                    openBreakEven(level);
                }
            });
            ContextMenu menu = new ContextMenu(breakEvenItem);
            menu.setOnShowing(event -> breakEvenItem.setDisable(row.getItem() == null
                    || cantContinue(row.getItem())
                    || breakEven(row.getItem()) == null));
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null).otherwise(menu));
            return row;
        });
    }

    static TableColumn<SellDownPlanner.Level, String> column(String title,
                                                             Function<SellDownPlanner.Level, String> value) {
        TableColumn<SellDownPlanner.Level, String> column = new TableColumn<>(title);
        column.setSortable(false);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    /** Bold, framed in blue; cell backgrounds are untouched, so row colours still show through. */
    static TableColumn<SellDownPlanner.Level, String> accented(TableColumn<SellDownPlanner.Level, String> column) {
        column.setStyle(ACCENTED_COLUMN_STYLE);
        return column;
    }

    @Nullable SellDownPlanner.BreakEven breakEven(SellDownPlanner.Level level) {
        SellDownPlanner.Params params = lastParams;
        SellDownPlanner.Plan plan = lastPlan;
        return params == null || plan == null ? null : SellDownPlanner.breakEven(params, plan, level);
    }

    /** New approximator tab starting from the position after breaking even at {@code level}, with the same settings. */
    void openBreakEven(SellDownPlanner.Level level) {
        SellDownPlanner.BreakEven be = breakEven(level);
        if (be == null) {
            return;
        }
        SellDownApproximatorState current = state();
        SellDownApproximatorTab approximator = new SellDownApproximatorTab(openTab);
        approximator.apply(new SellDownApproximatorState(current.symbol(),
                formatAmount(be.price().setScale(8, RoundingMode.HALF_UP)), formatAmount(be.debt()),
                formatAmount(be.base().setScale(8, RoundingMode.DOWN)),
                formatAmount(be.quote().setScale(8, RoundingMode.DOWN)), current.startRatio(), current.endRatio(),
                current.liquidationRatio(), current.fillBuffer(), current.slippage(), current.makerFee(),
                current.takerFee(), current.startRatioCurrent(), false));
        String symbol = current.symbol();
        openTab.accept(approximator, "Sell-down" + (symbol == null ? "" : " " + symbol) + " (BE L" + be.fromLevel() + ")");
    }

    /** Current inputs as entered. */
    SellDownApproximatorState state() {
        TickerPricePicker picker = checkNotNull(tickerPricePicker);
        return new SellDownApproximatorState(picker.selectedSymbol(), picker.priceTextField().getText(),
                text(debtTextField), text(baseTextField), text(quoteTextField), text(startRatioTextField),
                text(endRatioTextField), text(liquidationRatioTextField), text(fillBufferTextField),
                text(slippageTextField), text(makerFeeTextField), text(takerFeeTextField),
                checkNotNull(startRatioCurrentCheckBox).isSelected(), picker.isStreamSelected());
    }

    /**
     * Fills the inputs from {@code state} and recalculates. The saved price stays fixed unless the state streams it.
     * Missing values are kept.
     */
    void apply(SellDownApproximatorState state) {
        // Pair first: selecting it overwrites the fees
        checkNotNull(tickerPricePicker).selectFixedPrice(state.symbol(),
                state.price() == null ? checkNotNull(tickerPricePicker).priceTextField().getText() : state.price());
        setText(endRatioTextField, state.endRatio());
        setText(liquidationRatioTextField, state.liquidationRatio());
        setText(fillBufferTextField, state.fillBuffer());
        setText(slippageTextField, state.slippage());
        setText(makerFeeTextField, state.makerFee());
        setText(takerFeeTextField, state.takerFee());
        setText(debtTextField, state.debt());
        setText(baseTextField, state.base());
        setText(quoteTextField, state.quote());
        setText(startRatioTextField, state.startRatio());
        Boolean startRatioCurrent = state.startRatioCurrent();
        if (startRatioCurrent != null) {
            checkNotNull(startRatioCurrentCheckBox).setSelected(startRatioCurrent);
        }
        if (Boolean.TRUE.equals(state.streamPrice())) {
            checkNotNull(tickerPricePicker).setStreamSelected(true);
        }
        updateStartRatioMode();
        calculate();
    }

    static @Nullable String text(@Nullable TextField field) {
        return checkNotNull(field).getText();
    }

    static void setText(@Nullable TextField field, @Nullable String value) {
        if (value != null) {
            checkNotNull(field).setText(value);
        }
    }

    public StringProperty titleProperty() {
        return title;
    }

    static String fileTitle(File file) {
        String name = file.getName();
        return "Sell-down: " + (name.endsWith(".sd") ? name.substring(0, name.length() - 3) : name);
    }

    FileChooser fileChooser(String dialogTitle) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Sell-down Approximator (" +
                SellDownApproximatorState.EXTENSION + ")", SellDownApproximatorState.EXTENSION));
        File directory = lastDirectory;
        if (directory != null && directory.isDirectory()) {
            chooser.setInitialDirectory(directory);
        }
        return chooser;
    }

    @FXML
    public void save() {
        FileChooser chooser = fileChooser("Save Sell-down Approximator");
        String symbol = checkNotNull(tickerPricePicker).selectedSymbol();
        chooser.setInitialFileName((symbol == null ? "approximation" : symbol) + ".sd");
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().endsWith(".sd")) {
            file = new File(file.getParentFile(), file.getName() + ".sd");
        }
        try {
            state().write(file);
            lastDirectory = file.getParentFile();
            title.set(fileTitle(file));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save approximation to {}", file, e);
            new Alert(Alert.AlertType.ERROR, "Failed to save approximation: " + e, ButtonType.OK).showAndWait();
        }
    }

    @FXML
    public void load() {
        File file = fileChooser("Load Sell-down Approximator").showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            SellDownApproximatorState state = SellDownApproximatorState.read(file);
            lastDirectory = file.getParentFile();
            title.set(fileTitle(file));
            apply(state);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load approximation from {}", file, e);
            new Alert(Alert.AlertType.ERROR, "Failed to load approximation: " + e, ButtonType.OK).showAndWait();
        }
    }

    /** Red: can't continue after break even; orange: dangerous. Selected rows get a stronger shade. */
    String rowStyle(SellDownPlanner.Level level, boolean selected) {
        if (cantContinue(level)) {
            return selected ? CANT_CONTINUE_SELECTED_STYLE : CANT_CONTINUE_STYLE;
        }
        if (isDangerous(level)) {
            return selected ? DANGER_SELECTED_STYLE : DANGER_STYLE;
        }
        return "";
    }

    boolean cantContinue(SellDownPlanner.Level level) {
        SellDownPlanner.Params params = lastParams;
        SellDownPlanner.BreakEven be = breakEven(level);
        return params != null && be != null && cantContinue(be, params.endRatio());
    }

    /**
     * Breaking even is possible, but the position after it is already at/above the end ratio, so the sell-down can't
     * continue from there.
     */
    static boolean cantContinue(SellDownPlanner.BreakEven be, BigDecimal endRatio) {
        BigDecimal ratio = breakEvenRatio(be);
        return ratio != null && ratio.compareTo(endRatio) >= 0;
    }

    /** Debt ratio right after breaking even, at the break-even price. */
    static @Nullable BigDecimal breakEvenRatio(SellDownPlanner.BreakEven be) {
        return LiquidationCalculator.debtRatio(be.debt(), true, be.base(), be.quote(), be.price());
    }

    /** Liquidation price of the position after breaking even. */
    static @Nullable BigDecimal breakEvenLiqPrice(SellDownPlanner.BreakEven be, BigDecimal liquidationRatio) {
        return LiquidationCalculator.priceAtDebtRatio(be.debt(), true, be.base(), be.quote(), liquidationRatio);
    }

    /** A level is dangerous if the position would already be liquidated at its sell price should it not fill. */
    static boolean isDangerous(SellDownPlanner.Level level) {
        BigDecimal liq = level.liqPriceIfUnfilled();
        return liq != null && liq.compareTo(level.sellPrice()) >= 0;
    }

    @FXML
    public void calculate() {
        TableView<SellDownPlanner.Level> table = checkNotNull(levelsTable);
        Label summary = checkNotNull(summaryLabel);
        Label startState = checkNotNull(startStateLabel);
        table.getItems().clear();
        checkNotNull(levelsTextField).setText("");
        lastParams = null;
        lastPlan = null;
        try {
            SellDownPlanner.Params params = new SellDownPlanner.Params(
                    parse(checkNotNull(tickerPricePicker).priceTextField()), parse(debtTextField), parse(baseTextField), parse(quoteTextField),
                    parsePercent(startRatioTextField), parsePercent(endRatioTextField),
                    0,
                    parsePercent(fillBufferTextField), parsePercent(slippageTextField),
                    parsePercent(makerFeeTextField), parsePercent(takerFeeTextField),
                    parsePercent(liquidationRatioTextField));
            SellDownPlanner.Plan plan = SellDownPlanner.planAutoLevels(params);
            lastParams = params;
            lastPlan = plan;
            checkNotNull(levelsTextField).setText(String.valueOf(plan.levels().size()));

            BigDecimal startRatio = LiquidationCalculator.debtRatio(params.debt(), true, params.base(),
                    params.quote(), params.price());
            BigDecimal startLiq = LiquidationCalculator.priceAtDebtRatio(params.debt(), true, params.base(),
                    params.quote(), params.liquidationRatio());
            startState.setText(startState(startRatio, startLiq, params.price()));

            table.getItems().setAll(plan.levels());
            long cantContinue = plan.levels().stream().filter(this::cantContinue).count();
            summary.setText(summarize(plan, params) + (cantContinue > 0 ? " " + cantContinue
                    + " level(s) in red: break even possible, but the sell-down can't continue from there." : ""));
        } catch (IllegalArgumentException e) {
            startState.setText(String.valueOf(e.getMessage()));
            summary.setText("");
        }
    }

    /** Flags a debt ratio still below the designed 85% start; the start ratio itself is never changed. */
    static String startState(@Nullable BigDecimal ratio, @Nullable BigDecimal liquidationPrice, BigDecimal price) {
        String below = ratio != null
                && ratio.movePointRight(2).setScale(2, RoundingMode.CEILING).compareTo(DEFAULT_START_PERCENT) < 0
                ? " (< " + DEFAULT_START_PERCENT.toPlainString() + "%)" : "";
        return "Debt ratio: " + percent(ratio) + below + ", liquidation at " + priceWithDrop(liquidationPrice, price);
    }

    static String summarize(SellDownPlanner.Plan plan, SellDownPlanner.Params params) {
        StringBuilder text = new StringBuilder();
        if (plan.note() != null) {
            text.append(plan.note()).append(' ');
        }
        BigDecimal finalRatio = plan.finalRatio();
        if (finalRatio != null) {
            text.append("All QUOTE: ").append(formatAmount(plan.finalQuote())).append(", final debt ratio ")
                    .append(percent(finalRatio))
                    .append(finalRatio.compareTo(params.liquidationRatio()) < 0
                            ? " — can't be liquidated at any price. "
                            : " — at/above liquidation even with no BASE left (fees)! ");
        } else if (plan.finalBase().signum() > 0) {
            text.append("BASE left: ").append(formatAmount(plan.finalBase())).append(". ");
        }
        if (!plan.levels().isEmpty()) {
            SellDownPlanner.Level last = plan.levels().get(plan.levels().size() - 1);
            text.append("If the last chunk doesn't fill, liquidation at ")
                    .append(priceWithDrop(last.liqPriceIfUnfilled(), last.sellPrice())).append('.');
        }
        long dangerous = plan.levels().stream().filter(SellDownApproximatorTab::isDangerous).count();
        if (dangerous > 0) {
            text.append(" ").append(dangerous).append(" level(s) in orange: missing that fill means liquidation.");
        }
        return text.toString();
    }

    static String percent(@Nullable BigDecimal ratio) {
        return ratio == null ? NONE : ratio.movePointRight(2).setScale(2, RoundingMode.CEILING).toPlainString() + "%";
    }

    /** "price (-x.xx% from reference)". */
    static String priceWithDrop(@Nullable BigDecimal price, BigDecimal reference) {
        if (price == null) {
            return NONE;
        }
        BigDecimal move = LiquidationCalculator.priceMoveToPercent(reference, price);
        return formatAmount(price) + " (" + move.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%)";
    }

    static BigDecimal parse(@Nullable TextField textField) {
        String text = checkNotNull(textField).getText();
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    static BigDecimal parsePercent(@Nullable TextField textField) {
        return LiquidationCalculator.percentToRatio(parse(textField));
    }
}
