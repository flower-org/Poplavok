package com.poplavok.forms;

import com.poplavok.data.dao.MarketTickerDAO;
import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.utils.DBUtil;
import com.poplavok.kucoin.TickerPriceService;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static com.flower.fxutils.JavaFxUtils.createDecimalTextFormatter;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;

/**
 * Reusable "Ticker: [filter] [pair] Price: [price] [x] stream" row. Pairs come from the DB; while "stream" is
 * checked the price field shows the pair's live last trade price, otherwise it's entered manually.
 * Call {@link #dispose()} when the owning window closes.
 */
public class TickerPricePicker extends HBox {
    static final Logger LOGGER = LoggerFactory.getLogger(TickerPricePicker.class);
    static final String READ_ONLY_STYLE = "-fx-control-inner-background: #e8e8e8;";

    final TextField filterTextField = new TextField();
    final ComboBox<MarketTicker> tickerComboBox = new ComboBox<>();
    final TextField priceTextField = new TextField();
    final CheckBox streamCheckBox = new CheckBox("stream");
    final FilteredList<MarketTicker> tickers;

    @Nullable String streamedSymbol;
    final LatestValueDispatcher<StreamedPrice> shownPrice = new LatestValueDispatcher<>(Platform::runLater, latest -> {
        if (latest.symbol().equals(streamedSymbol)) {
            priceTextField.setText(formatAmount(latest.price()));
        }
    });
    final TickerPriceService.PriceListener priceListener = this::onStreamedPrice;

    public TickerPricePicker() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8.0);

        Label tickerLabel = new Label("Ticker:");
        tickerLabel.setMinWidth(USE_PREF_SIZE);
        filterTextField.setPromptText("Filter");
        filterTextField.setPrefWidth(70.0);
        filterTextField.setOnKeyTyped(event -> filterTickers());
        tickerComboBox.setPrefWidth(140.0);
        Label priceLabel = new Label("Price:");
        priceLabel.setMinWidth(USE_PREF_SIZE);
        priceTextField.setPrefWidth(100.0);
        priceTextField.setTextFormatter(createDecimalTextFormatter());
        HBox.setHgrow(priceTextField, Priority.ALWAYS);
        streamCheckBox.setMinWidth(USE_PREF_SIZE);
        streamCheckBox.setSelected(true);
        getChildren().addAll(tickerLabel, filterTextField, tickerComboBox, priceLabel, priceTextField, streamCheckBox);

        tickerComboBox.setConverter(new StringConverter<>() {
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
        tickerComboBox.setItems(new SortedList<>(tickers, Comparator.comparing(MarketTicker::getSymbol)));
        tickerComboBox.valueProperty().addListener((observable, oldValue, newValue) -> updateStreaming());
        streamCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updateStreaming());
        updateStreaming();
    }

    public ReadOnlyObjectProperty<MarketTicker> selectedTickerProperty() {
        return tickerComboBox.valueProperty();
    }

    public TextField priceTextField() {
        return priceTextField;
    }

    public boolean isStreamSelected() {
        return streamCheckBox.isSelected();
    }

    /** Checks/unchecks "stream"; streaming only runs while a pair is selected. */
    public void setStreamSelected(boolean stream) {
        streamCheckBox.setSelected(stream);
    }

    public @Nullable String selectedSymbol() {
        MarketTicker ticker = tickerComboBox.getValue();
        return ticker == null ? null : ticker.getSymbol();
    }

    /** BASE name from the selected "BASE-QUOTE" symbol (avoids touching lazy-loaded currencies), or "BASE". */
    public String baseCurrency() {
        String[] parts = symbolParts();
        return parts == null ? "BASE" : parts[0];
    }

    /** QUOTE name from the selected "BASE-QUOTE" symbol, or "QUOTE". */
    public String quoteCurrency() {
        String[] parts = symbolParts();
        return parts == null ? "QUOTE" : parts[1];
    }

    @Nullable String[] symbolParts() {
        String symbol = selectedSymbol();
        if (symbol == null) {
            return null;
        }
        int dash = symbol.indexOf('-');
        return dash > 0 && dash < symbol.length() - 1
                ? new String[] { symbol.substring(0, dash), symbol.substring(dash + 1) }
                : null;
    }

    /** Selects the pair with {@code symbol} (if loaded) and shows a fixed, non-streamed {@code price}. */
    public void selectFixedPrice(@Nullable String symbol, String price) {
        streamCheckBox.setSelected(false);
        if (symbol != null) {
            tickers.setPredicate(null);
            filterTextField.setText("");
            tickerComboBox.getItems().stream()
                    .filter(ticker -> symbol.equals(ticker.getSymbol()))
                    .findFirst()
                    .ifPresent(tickerComboBox::setValue);
        }
        priceTextField.setText(price);
    }

    void filterTickers() {
        String searchText = filterTextField.getText();
        String needle = searchText == null ? "" : searchText.trim().toLowerCase();
        tickers.setPredicate(ticker -> needle.isEmpty() || ticker.getSymbol().toLowerCase().contains(needle));
        tickerComboBox.getSelectionModel().selectFirst();
    }

    /** Subscribes to the selected ticker while "stream" is checked; otherwise the price is entered manually. */
    void updateStreaming() {
        String symbol = selectedSymbol();
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
                priceTextField.setText(formatAmount(cached));
            }
        }

        priceTextField.setEditable(!stream);
        priceTextField.setStyle(stream ? READ_ONLY_STYLE : "");
    }

    void onStreamedPrice(String symbol, @Nullable BigDecimal price) {
        if (price == null) {
            return;
        }
        shownPrice.submit(new StreamedPrice(symbol, price));
    }

    record StreamedPrice(String symbol, BigDecimal price) {}

    /** Stops price streaming. Safe to call multiple times. */
    public void dispose() {
        if (streamedSymbol != null) {
            TickerPriceService.getInstance().unsubscribe(streamedSymbol, priceListener);
            streamedSymbol = null;
        }
    }
}
