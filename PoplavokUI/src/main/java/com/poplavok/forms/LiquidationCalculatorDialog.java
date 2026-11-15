package com.poplavok.forms;

import com.poplavok.api.kucoin.model.response.IsolatedSymbolResponse;
import com.poplavok.data.dao.MarketTickerDAO;
import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.utils.DBUtil;
import com.poplavok.data.utils.LiquidationCalculator;
import com.poplavok.kucoin.KucoinTool;
import com.poplavok.kucoin.TickerPriceService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.flower.fxutils.JavaFxUtils.createDecimalTextFormatter;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;

/**
 * Calculates the prices at which a margin position reaches given debt ratios. Recalculates on every input change.
 * The current price is either streamed for the selected ticker or entered manually.
 */
public class LiquidationCalculatorDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(LiquidationCalculatorDialog.class);
    static final String NOT_REACHABLE = "N/A";
    static final String READ_ONLY_STYLE = "-fx-control-inner-background: #e8e8e8;";

    @FXML @Nullable TextField tickerFilterTextField;
    @FXML @Nullable ComboBox<MarketTicker> tickerComboBox;
    @FXML @Nullable TextField priceTextField;
    @FXML @Nullable CheckBox streamPriceCheckBox;
    @FXML @Nullable Label marginInfoLabel;

    @FXML @Nullable TextField debtTextField;
    @FXML @Nullable RadioButton debtInQuoteRadioButton;
    @FXML @Nullable RadioButton debtInBaseRadioButton;
    @FXML @Nullable Label holdingBaseLabel;
    @FXML @Nullable TextField holdingBaseTextField;
    @FXML @Nullable Label holdingQuoteLabel;
    @FXML @Nullable TextField holdingQuoteTextField;
    @FXML @Nullable TextField liquidationRatioTextField;

    @FXML @Nullable TextField price85TextField;
    @FXML @Nullable TextField price90TextField;
    @FXML @Nullable TextField warningPriceTextField;
    @FXML @Nullable Label liquidationPriceLabel;
    @FXML @Nullable TextField liquidationPriceTextField;
    @FXML @Nullable TextField currentDebtRatioTextField;
    @FXML @Nullable TextField distanceToLiquidationTextField;
    @FXML @Nullable Label hintLabel;

    @Nullable FilteredList<MarketTicker> tickers;
    @Nullable String streamedSymbol;
    long marginInfoRequest;
    final AtomicReference<BigDecimal> pendingPrice = new AtomicReference<>();
    final TickerPriceService.PriceListener priceListener = this::onStreamedPrice;

    public LiquidationCalculatorDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LiquidationCalculatorDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        for (TextField input : new TextField[] { checkNotNull(priceTextField), checkNotNull(debtTextField),
                checkNotNull(holdingBaseTextField), checkNotNull(holdingQuoteTextField),
                checkNotNull(liquidationRatioTextField) }) {
            input.setTextFormatter(createDecimalTextFormatter());
            input.textProperty().addListener((observable, oldValue, newValue) -> recalculate());
        }
        checkNotNull(debtInQuoteRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> recalculate());

        initTickers();
        checkNotNull(streamPriceCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> updateStreaming());

        updateStreaming();
        recalculate();
    }

    void initTickers() {
        ComboBox<MarketTicker> comboBox = checkNotNull(tickerComboBox);
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(@Nullable MarketTicker ticker) {
                return ticker != null ? ticker.getSymbol() : "";
            }

            @Override
            public MarketTicker fromString(String string) {
                throw new UnsupportedOperationException("ComboBox is not editable");
            }
        });

        List<MarketTicker> allTickers;
        try {
            allTickers = DBUtil.connectGetResultAndClose(MarketTickerDAO::findAll);
        } catch (Exception e) {
            LOGGER.error("Failed to load market tickers", e);
            allTickers = List.of();
        }
        tickers = new FilteredList<>(FXCollections.observableArrayList(allTickers));
        comboBox.setItems(new SortedList<>(tickers, Comparator.comparing(MarketTicker::getSymbol)));
        comboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateCurrencyLabels();
            updateStreaming();
            updateMarginInfo();
        });
    }

    /** Requests KuCoin isolated margin pair settings off the FX thread. */
    static CompletableFuture<Map<String, IsolatedSymbolResponse>> requestIsolatedSymbols() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return KucoinTool.createApiClient().getIsolatedSymbols().stream()
                        .collect(Collectors.toMap(IsolatedSymbolResponse::symbol, s -> s, (a, b) -> a));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /**
     * Clears the liquidation ratio, then requests the selected pair's live KuCoin isolated margin "flDebtRatio"
     * and fills it in when the response arrives. Responses for a previously selected pair are ignored.
     */
    void updateMarginInfo() {
        String symbol = selectedSymbol();
        Label infoLabel = checkNotNull(marginInfoLabel);
        checkNotNull(liquidationRatioTextField).clear();
        long request = ++marginInfoRequest;
        if (symbol == null) {
            infoLabel.setText("");
            return;
        }
        infoLabel.setText("Loading KuCoin margin settings for " + symbol + "…");
        var unused = requestIsolatedSymbols().whenComplete((symbols, error) -> Platform.runLater(() -> {
            if (request != marginInfoRequest) {
                return;
            }
            if (error != null) {
                LOGGER.error("Failed to load KuCoin isolated margin symbols", error);
                infoLabel.setText("Failed to load KuCoin margin settings; enter the liquidation ratio manually.");
                return;
            }
            IsolatedSymbolResponse info = symbols.get(symbol);
            if (info == null) {
                infoLabel.setText(symbol + " is not available for KuCoin isolated margin.");
                return;
            }
            BigDecimal liqRatio = info.flDebtRatio();
            if (liqRatio != null) {
                checkNotNull(liquidationRatioTextField).setText(
                        liqRatio.movePointRight(2).stripTrailingZeros().toPlainString());
            }
            infoLabel.setText("KuCoin isolated " + symbol + ": liquidation " + percent(liqRatio)
                    + ", auto-renew up to " + percent(info.autoRenewMaxDebtRatio())
                    + ", max leverage " + (info.maxLeverage() == null ? "?" : info.maxLeverage() + "x")
                    + ", borrow " + borrowable(info));
        }));
    }

    static String percent(@Nullable BigDecimal ratio) {
        return ratio == null ? "?" : ratio.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }

    static String borrowable(IsolatedSymbolResponse info) {
        boolean base = Boolean.TRUE.equals(info.baseBorrowEnable());
        boolean quote = Boolean.TRUE.equals(info.quoteBorrowEnable());
        if (base && quote) {
            return "base & quote";
        }
        return base ? "base only" : quote ? "quote only" : "disabled";
    }

    public void filterTickers() {
        if (tickers == null) {
            return;
        }
        String searchText = checkNotNull(tickerFilterTextField).getText();
        String needle = searchText == null ? "" : searchText.trim().toLowerCase();
        tickers.setPredicate(ticker -> needle.isEmpty() || ticker.getSymbol().toLowerCase().contains(needle));
        checkNotNull(tickerComboBox).getSelectionModel().selectFirst();
    }

    @Nullable String selectedSymbol() {
        MarketTicker ticker = checkNotNull(tickerComboBox).getValue();
        return ticker == null ? null : ticker.getSymbol();
    }

    /** Base/quote names from a KuCoin "BASE-QUOTE" symbol (avoids touching lazy-loaded currencies). */
    void updateCurrencyLabels() {
        String symbol = selectedSymbol();
        String base = "BASE";
        String quote = "QUOTE";
        if (symbol != null) {
            int dash = symbol.indexOf('-');
            if (dash > 0 && dash < symbol.length() - 1) {
                base = symbol.substring(0, dash);
                quote = symbol.substring(dash + 1);
            }
        }
        checkNotNull(holdingBaseLabel).setText("Holding " + base + ":");
        checkNotNull(holdingQuoteLabel).setText("Holding " + quote + ":");
        checkNotNull(debtInQuoteRadioButton).setText(quote + " (long)");
        checkNotNull(debtInBaseRadioButton).setText(base + " (short)");
    }

    /** Subscribes to the selected ticker while "stream" is checked; otherwise the price is entered manually. */
    void updateStreaming() {
        String symbol = selectedSymbol();
        CheckBox streamCheckBox = checkNotNull(streamPriceCheckBox);
        streamCheckBox.setDisable(symbol == null);
        boolean stream = symbol != null && streamCheckBox.isSelected();
        String targetSymbol = stream ? symbol : null;

        if (streamedSymbol != null && !streamedSymbol.equals(targetSymbol)) {
            TickerPriceService.getInstance().unsubscribe(streamedSymbol, priceListener);
            streamedSymbol = null;
        }
        if (targetSymbol != null && streamedSymbol == null) {
            streamedSymbol = targetSymbol;
            TickerPriceService.getInstance().subscribe(targetSymbol, priceListener);
            BigDecimal cached = TickerPriceService.getInstance().getLastPrice(targetSymbol);
            if (cached != null) {
                checkNotNull(priceTextField).setText(formatAmount(cached));
            }
        }

        TextField priceField = checkNotNull(priceTextField);
        priceField.setEditable(!stream);
        priceField.setStyle(stream ? READ_ONLY_STYLE : "");
    }

    void onStreamedPrice(String symbol, @Nullable BigDecimal price) {
        if (price == null) {
            return;
        }
        // Coalesce bursts into a single FX update with the latest price
        if (pendingPrice.getAndSet(price) == null) {
            Platform.runLater(() -> {
                BigDecimal latest = pendingPrice.getAndSet(null);
                if (latest != null && symbol.equals(streamedSymbol)) {
                    checkNotNull(priceTextField).setText(formatAmount(latest));
                }
            });
        }
    }

    /** Stops price streaming. Safe to call multiple times. */
    public void dispose() {
        if (streamedSymbol != null) {
            TickerPriceService.getInstance().unsubscribe(streamedSymbol, priceListener);
            streamedSymbol = null;
        }
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

    void recalculate() {
        BigDecimal debt = parse(debtTextField);
        boolean debtInQuote = checkNotNull(debtInQuoteRadioButton).isSelected();
        BigDecimal holdingBase = parse(holdingBaseTextField);
        BigDecimal holdingQuote = parse(holdingQuoteTextField);
        BigDecimal liquidationPercent = parse(liquidationRatioTextField);
        BigDecimal currentPrice = parse(priceTextField);

        setPrice(price85TextField, debt, debtInQuote, holdingBase, holdingQuote, new BigDecimal("85"));
        setPrice(price90TextField, debt, debtInQuote, holdingBase, holdingQuote, new BigDecimal("90"));
        setPrice(warningPriceTextField, debt, debtInQuote, holdingBase, holdingQuote,
                LiquidationCalculator.KUCOIN_WARNING_DEBT_RATIO.movePointRight(2));
        BigDecimal liquidationPrice = null;
        if (liquidationPercent.signum() > 0) {
            liquidationPrice = setPrice(liquidationPriceTextField, debt, debtInQuote, holdingBase, holdingQuote,
                    liquidationPercent);
        } else {
            checkNotNull(liquidationPriceTextField).setText(NOT_REACHABLE);
        }

        BigDecimal debtRatio = currentPrice.signum() > 0
                ? LiquidationCalculator.debtRatio(debt, debtInQuote, holdingBase, holdingQuote, currentPrice)
                : null;
        checkNotNull(currentDebtRatioTextField).setText(debtRatio == null ? NOT_REACHABLE
                : debtRatio.movePointRight(2).setScale(2, RoundingMode.CEILING).toPlainString() + "%");

        String distance = NOT_REACHABLE;
        if (liquidationPrice != null && currentPrice.signum() > 0) {
            BigDecimal move = LiquidationCalculator.priceMoveToPercent(currentPrice, liquidationPrice);
            distance = (move.signum() > 0 ? "+" : "") + move.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
        }
        checkNotNull(distanceToLiquidationTextField).setText(distance);

        checkNotNull(liquidationPriceLabel).setText(liquidationPercent.signum() > 0
                ? "Liquidation price (" + liquidationPercent.stripTrailingZeros().toPlainString() + "%):"
                : "Liquidation price:");
        checkNotNull(hintLabel).setText(debtInQuote
                ? "Debt in QUOTE: debt ratio grows as the price falls below these levels."
                : "Debt in BASE: debt ratio grows as the price rises above these levels.");
    }

    static @Nullable BigDecimal setPrice(@Nullable TextField target, BigDecimal debt, boolean debtInQuote,
                                         BigDecimal holdingBase, BigDecimal holdingQuote, BigDecimal percent) {
        BigDecimal price = LiquidationCalculator.priceAtDebtRatio(debt, debtInQuote, holdingBase, holdingQuote,
                LiquidationCalculator.percentToRatio(percent));
        checkNotNull(target).setText(price == null ? NOT_REACHABLE : formatAmount(price));
        return price;
    }
}
