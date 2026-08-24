package com.poplavok.forms;

import com.poplavok.api.kucoin.KucoinApiClient;
import com.poplavok.api.kucoin.auth.KucoinCredentialsProvider;
import com.poplavok.api.kucoin.model.request.ImmutableOrderCreateRequest;
import com.poplavok.api.kucoin.model.request.OrderCreateRequest;
import com.poplavok.api.kucoin.model.response.OrderCreateResponse;
import com.flower.fxutils.JavaFxUtils;

import java.net.Proxy;
import java.util.UUID;
import com.poplavok.data.model.Direction;
import com.poplavok.data.model.Trade;
import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.model.TradeOperation;
import com.poplavok.data.utils.AmountAndCommission;
import com.poplavok.data.utils.LongShortCalculator;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import static com.flower.fxutils.JavaFxUtils.YesNo.NO;
import static com.flower.fxutils.JavaFxUtils.isInvalidAmountChar;
import static com.flower.fxutils.JavaFxUtils.showYesNoDialog;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.utils.BigDecimalUtil.SCALE;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;
import static com.poplavok.data.utils.BigDecimalUtil.fromString;
import static com.poplavok.data.utils.BigDecimalUtil.nullToZero;

public class PerformTradeDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(PerformTradeDialog.class);

    @FXML @Nullable TextField priceTextField;
    @FXML @Nullable Label tickerLabel;
    @FXML @Nullable ToolBar availableToolBar;
    @FXML @Nullable Separator availableAmountSeparator;
    @FXML @Nullable TextField availableBaseTextField;
    @FXML @Nullable TextField availableQuoteTextField;
    @FXML @Nullable Button useAllBaseButton;
    @FXML @Nullable Button useAllQuoteButton;
    @FXML @Nullable TextField feeTextField;
    @FXML @Nullable Label quoteBuyLabel;
    @FXML @Nullable Label baseBuyLabel;
    @FXML @Nullable Label baseSellLabel;
    @FXML @Nullable Label quoteSellLabel;
    @FXML @Nullable Button buyButton;
    @FXML @Nullable Button sellButton;

    @FXML @Nullable CheckBox manualEntryCheckBox;
    @FXML @Nullable Tab buyTab;
    @FXML @Nullable Tab sellTab;
    @FXML @Nullable TextField giveQuoteBuyTextField;
    @FXML @Nullable TextField getBaseBuyTextField;
    @FXML @Nullable TextField giveBaseSellTextField;
    @FXML @Nullable TextField getQuoteSellTextField;

    @FXML @Nullable TabPane tradeTabPane;

    @FXML @Nullable TextField commissionQuoteBuyTextField;
    @FXML @Nullable Label quoteBuyCommissionLabel;

    @FXML @Nullable TextField commissionsBaseSellTextField;
    @FXML @Nullable Label quoteSellCommissionLabel;

    @FXML @Nullable Separator debtSeparator;
    @FXML @Nullable Label debtLabel;
    @FXML @Nullable TextField debtTextField;
    @FXML @Nullable Label debtCurrencyLabel;

    @FXML @Nullable CheckBox autoBorrowBuyCheckBox;
    @FXML @Nullable CheckBox autoBorrowSellCheckBox;

    @Nullable Stage stage;
    @Nullable Trade returnTrade;

    protected final MainForm mainApp;
    final Direction direction;
    final MarketTicker ticker;
    final boolean isAveragingTrade;

    /** Regular Trade */
    public PerformTradeDialog(MainForm mainApp, @Nullable BigDecimal availableAmountBase, @Nullable BigDecimal availableAmountQuote,
                              MarketTicker ticker, Direction direction, @Nullable BigDecimal price) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PerformTradeDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.mainApp = mainApp;
        this.isAveragingTrade = false;
        this.ticker = ticker;
        this.direction = direction;

        checkNotNull(tickerLabel).textProperty().setValue(ticker.getSymbol());
        checkNotNull(priceTextField).textProperty().setValue(price != null ? price.toPlainString() : "");
        checkNotNull(availableBaseTextField).textProperty().setValue(formatAmount(availableAmountBase));
        checkNotNull(availableQuoteTextField).textProperty().setValue(formatAmount(availableAmountQuote));
        checkNotNull(useAllBaseButton).textProperty().setValue(ticker.getBase().getCurrency());
        checkNotNull(useAllQuoteButton).textProperty().setValue(ticker.getQuote().getCurrency());
        BigDecimal fee = ticker.getMakerFee();
        checkNotNull(feeTextField).textProperty().setValue(formatAmount(fee));
        checkNotNull(quoteBuyLabel).textProperty().setValue(ticker.getQuote().getCurrency());
        checkNotNull(quoteBuyCommissionLabel).textProperty().setValue(ticker.getQuote().getCurrency());
        checkNotNull(baseBuyLabel).textProperty().setValue(ticker.getBase().getCurrency());
        checkNotNull(baseSellLabel).textProperty().setValue(ticker.getBase().getCurrency());
        checkNotNull(quoteSellCommissionLabel).textProperty().setValue(ticker.getQuote().getCurrency());
        checkNotNull(quoteSellLabel).textProperty().setValue(ticker.getQuote().getCurrency());
        checkNotNull(buyButton).textProperty().setValue("BUY " + ticker.getBase().getCurrency());
        checkNotNull(sellButton).textProperty().setValue("SELL " + ticker.getBase().getCurrency());
        
        checkNotNull(priceTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(availableBaseTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(availableQuoteTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(giveQuoteBuyTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(getBaseBuyTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(giveBaseSellTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(getQuoteSellTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        
        checkNotNull(priceTextField).textProperty().addListener(this::onPriceChanged);
        checkNotNull(feeTextField).textProperty().addListener(this::onFeeChanged);

        checkNotNull(manualEntryCheckBox).selectedProperty().addListener(this::onManualEntryCheckedChanged);

        checkNotNull(debtSeparator).visibleProperty().setValue(false);
        checkNotNull(debtLabel).visibleProperty().setValue(false);
        checkNotNull(debtTextField).visibleProperty().setValue(false);
        checkNotNull(debtCurrencyLabel).visibleProperty().setValue(false);

        switch (direction) {
            case LONG:
                checkNotNull(tradeTabPane).getSelectionModel().select(checkNotNull(buyTab));
                break;
            case SHORT:
                checkNotNull(tradeTabPane).getSelectionModel().select(checkNotNull(sellTab));
                break;
            default: throw new IllegalStateException("Unknown direction: " + direction);
        }
    }

    /** Averaging Trade */
    public PerformTradeDialog(MainForm mainApp, @Nullable BigDecimal availableAmountBase, @Nullable BigDecimal availableAmountQuote,
                              BigDecimal debt, MarketTicker ticker, Direction direction, @Nullable BigDecimal price) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PerformTradeDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.mainApp = mainApp;
        this.isAveragingTrade = true;
        this.ticker = ticker;
        this.direction = direction;

        checkNotNull(debtTextField).textProperty().setValue(formatAmount(debt));
        checkNotNull(availableToolBar).getItems().remove(availableAmountSeparator);
        checkNotNull(debtCurrencyLabel).visibleProperty().setValue(false);

        checkNotNull(tickerLabel).textProperty().setValue(ticker.getSymbol());
        checkNotNull(priceTextField).textProperty().setValue(price != null ? price.toPlainString() : "");
        checkNotNull(availableBaseTextField).textProperty().setValue(formatAmount(availableAmountBase));
        checkNotNull(availableQuoteTextField).textProperty().setValue(formatAmount(availableAmountQuote));
        checkNotNull(useAllBaseButton).textProperty().setValue(ticker.getBase().getCurrency());
        checkNotNull(useAllQuoteButton).textProperty().setValue(ticker.getQuote().getCurrency());
        BigDecimal fee = ticker.getMakerFee();
        checkNotNull(feeTextField).textProperty().setValue(formatAmount(fee));
        checkNotNull(quoteBuyLabel).textProperty().setValue(ticker.getQuote().getCurrency());
        checkNotNull(quoteBuyCommissionLabel).textProperty().setValue(ticker.getQuote().getCurrency());
        checkNotNull(baseBuyLabel).textProperty().setValue(ticker.getBase().getCurrency());
        checkNotNull(baseSellLabel).textProperty().setValue(ticker.getBase().getCurrency());
        checkNotNull(quoteSellCommissionLabel).textProperty().setValue(ticker.getQuote().getCurrency());
        checkNotNull(quoteSellLabel).textProperty().setValue(ticker.getQuote().getCurrency());
        checkNotNull(buyButton).textProperty().setValue("BUY " + ticker.getBase().getCurrency());
        checkNotNull(sellButton).textProperty().setValue("SELL " + ticker.getBase().getCurrency());

        checkNotNull(priceTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(availableBaseTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(availableQuoteTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(giveQuoteBuyTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(getBaseBuyTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(giveBaseSellTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());
        checkNotNull(getQuoteSellTextField).setTextFormatter(JavaFxUtils.createDecimalTextFormatter());

        checkNotNull(priceTextField).textProperty().addListener(this::onPriceChanged);
        checkNotNull(feeTextField).textProperty().addListener(this::onFeeChanged);

        checkNotNull(manualEntryCheckBox).selectedProperty().addListener(this::onManualEntryCheckedChanged);

        switch (direction) {
            case LONG:
                // LONG: Sell BASE to average
                checkNotNull(tradeTabPane).getSelectionModel().select(checkNotNull(sellTab));
                checkNotNull(tradeTabPane).getTabs().remove(checkNotNull(buyTab));
                checkNotNull(availableToolBar).getItems().remove(availableQuoteTextField);
                checkNotNull(availableToolBar).getItems().remove(useAllQuoteButton);
                break;
            case SHORT:
                // SHORT: Buy BASE to average
                checkNotNull(tradeTabPane).getSelectionModel().select(checkNotNull(buyTab));
                checkNotNull(tradeTabPane).getTabs().remove(checkNotNull(sellTab));
                checkNotNull(availableToolBar).getItems().remove(availableBaseTextField);
                checkNotNull(availableToolBar).getItems().remove(useAllBaseButton);
                break;
            default:
                throw new IllegalStateException("Unknown direction: " + direction);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void buy() {
        try {
            returnTrade = new Trade();

            BigDecimal price = nullToZero(fromString((checkNotNull(priceTextField).getText())));
            BigDecimal fee = nullToZero(fromString(checkNotNull(feeTextField).getText()));
            BigDecimal tradeQuoteAmount = nullToZero(fromString(checkNotNull(giveQuoteBuyTextField).getText()));
            BigDecimal tradeBaseAmount = nullToZero(fromString(checkNotNull(getBaseBuyTextField).getText()));
            returnTrade.setAmountQuoteIn(tradeQuoteAmount);
            returnTrade.setAmountBaseOut(tradeBaseAmount);
            returnTrade.setOperation(TradeOperation.BUY);
            returnTrade.setDate(new Date());

            returnTrade.setCommissionQuote(tradeQuoteAmount.multiply(fee));
            returnTrade.setCommissionBase(tradeQuoteAmount.multiply(fee).divide(price, SCALE, RoundingMode.CEILING));

            if (isAveragingTrade) {
                BigDecimal debt = nullToZero(fromString(checkNotNull(debtTextField).getText()));
                boolean debtCovered = tradeBaseAmount.compareTo(debt) >= 0;

                if (!debtCovered) {
                    if (NO == showYesNoDialog("Debt not fully covered",
                            "The amount of proceeds in " + ticker.getBase().getCurrency() + " is less than debt amount. Trade anyway?")) {
                        return;
                    }
                }
            }

            checkNotNull(stage).close();
        } catch (Exception e) {
            LOGGER.error("Buy Error:", e);
            JavaFxUtils.showErrorMessage("Buy Error: " + e);
        }
    }

    public void sell() {
        try {
            returnTrade = new Trade();

            BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText()));
            BigDecimal fee = nullToZero(fromString(checkNotNull(feeTextField).getText()));

            BigDecimal tradeBaseAmount = nullToZero(fromString(checkNotNull(giveBaseSellTextField).getText()));
            BigDecimal tradeQuoteAmount = nullToZero(fromString(checkNotNull(getQuoteSellTextField).getText()));
            returnTrade.setAmountBaseIn(tradeBaseAmount);
            returnTrade.setAmountQuoteOut(tradeQuoteAmount);
            returnTrade.setOperation(TradeOperation.SELL);
            returnTrade.setDate(new Date());

            returnTrade.setCommissionBase(tradeBaseAmount.multiply(fee));
            returnTrade.setCommissionQuote(tradeBaseAmount.multiply(fee).multiply(price));

            if (isAveragingTrade) {
                BigDecimal debt = nullToZero(fromString(checkNotNull(debtTextField).getText()));
                boolean debtCovered = tradeQuoteAmount.compareTo(debt) >= 0;

                if (!debtCovered) {
                    if (NO == showYesNoDialog("Debt not fully covered",
                            "The amount of proceeds in " + ticker.getQuote().getCurrency() + " is less than debt amount. Trade anyway?")) {
                        return;
                    }
                }
            }

            checkNotNull(stage).close();
       } catch (Exception e) {
            LOGGER.error("Sell Error:", e);
            JavaFxUtils.showErrorMessage("Sell Error: " + e);
        }
    }

    public void onPriceChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        priceFeeUpdate(true, true);
    }

    public void onFeeChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        priceFeeUpdate(true, true);
    }

    public void onManualEntryCheckedChanged(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        priceFeeUpdate(true, true);
    }

    public void useAllBase() {
        try {
            if (!checkNotNull(tradeTabPane).getSelectionModel().getSelectedItem().equals(sellTab)) {
                JavaFxUtils.showErrorMessage("Select Sell tab to use all BASE");
                return;
            }

            if (!StringUtils.isBlank(checkNotNull(availableBaseTextField).getText())) {
                BigDecimal availableBase = nullToZero(fromString(checkNotNull(availableBaseTextField).getText()));
                checkNotNull(giveBaseSellTextField).textProperty().setValue(formatAmount(availableBase));
                onGiveBaseSellChanged(null);
            }
        } catch (Exception e) {
            LOGGER.error("Error on useAllBase:", e);
            JavaFxUtils.showErrorMessage("Error on useAllBase: " + e);
        }
    }

    public void useAllQuote() {
        try {
            if (!checkNotNull(tradeTabPane).getSelectionModel().getSelectedItem().equals(buyTab)) {
                JavaFxUtils.showErrorMessage("Select Buy tab to use all QUOTE");
                return;
            }

            if (!StringUtils.isBlank(checkNotNull(availableQuoteTextField).getText())) {
                BigDecimal availableQuote = nullToZero(fromString(checkNotNull(availableQuoteTextField).getText()));
                checkNotNull(giveQuoteBuyTextField).textProperty().setValue(formatAmount(availableQuote));
                onGiveQuoteBuyChanged(null);
            }
        } catch (Exception e) {
            LOGGER.error("Error on useAllQuote:", e);
            JavaFxUtils.showErrorMessage("Error on useAllQuote: " + e);
        }
    }

    public void onGiveQuoteBuyChanged(@Nullable KeyEvent event) {
        if (isInvalidAmountChar(event)) return;
        priceFeeUpdate(true, false);
    }

    public void onGiveBaseSellChanged(@Nullable KeyEvent event) {
        if (isInvalidAmountChar(event)) return;
        priceFeeUpdate(false, true);
    }

    public void onGetBaseBuyChanged(@Nullable KeyEvent event) {
        if (isInvalidAmountChar(event)) return;
        reversePriceFeeUpdate(true, false);
    }

    public void onGetQuoteSellChanged(@Nullable KeyEvent event) {
        if (isInvalidAmountChar(event)) return;
        reversePriceFeeUpdate(false, true);
    }

    // TODO: move `priceFeeUpdate`, `reversePriceFeeUpdate` to a separate service class

    /** Update quote sell, base buy, if manual input is not checked */
    protected void priceFeeUpdate(boolean updateBaseBuy, boolean updateQuoteSell) {
        try {
            boolean manualInput = checkNotNull(manualEntryCheckBox).isSelected();
            if (!manualInput) {
                if (StringUtils.isBlank(checkNotNull(priceTextField).getText())) { return; }
                if (StringUtils.isBlank(checkNotNull(feeTextField).getText())) { return; }
                BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText()));
                BigDecimal fee = nullToZero(fromString(checkNotNull(feeTextField).getText()));

                if (price.compareTo(BigDecimal.ZERO) == 0) { return; }

                // Update GET BASE BUY amount
                if (updateBaseBuy && !StringUtils.isBlank(checkNotNull(giveQuoteBuyTextField).getText())) {
                    BigDecimal giveQuoteBuy = nullToZero(fromString(checkNotNull(giveQuoteBuyTextField).getText()));

                    AmountAndCommission getBaseBuy = LongShortCalculator.calculateBaseAmountToGetLong(giveQuoteBuy, price, fee);

                    checkNotNull(getBaseBuyTextField).setText(formatAmount(getBaseBuy.amount));
                    checkNotNull(commissionQuoteBuyTextField).setText(formatAmount(getBaseBuy.commissionQuote));
                }

                // Update GET QUOTE SELL amount
                if (updateQuoteSell && !StringUtils.isBlank(checkNotNull(giveBaseSellTextField).getText())) {
                    BigDecimal giveBaseSell = nullToZero(fromString(checkNotNull(giveBaseSellTextField).getText()));

                    AmountAndCommission getQuoteSell = LongShortCalculator.calculateQuoteAmountToGetShort(giveBaseSell, price, fee);

                    checkNotNull(getQuoteSellTextField).setText(formatAmount(getQuoteSell.amount));
                    checkNotNull(commissionsBaseSellTextField).setText(formatAmount(getQuoteSell.commissionQuote));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error on Recalc:", e);
            JavaFxUtils.showErrorMessage("Error on Recalc: " + e);
        }
    }

    /** Update quote buy, base sell, if manual input is not checked */
    protected void reversePriceFeeUpdate(boolean updateQuoteBuy, boolean updateBaseSell) {
        try {
            boolean manualInput = checkNotNull(manualEntryCheckBox).isSelected();
            if (!manualInput) {
                if (StringUtils.isBlank(checkNotNull(priceTextField).getText())) { return; }
                if (StringUtils.isBlank(checkNotNull(feeTextField).getText())) { return; }
                BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText()));
                BigDecimal fee = nullToZero(fromString(checkNotNull(feeTextField).getText()));

                // Update GIVE QUOTE BUY amount
                if (updateQuoteBuy && !StringUtils.isBlank(checkNotNull(getBaseBuyTextField).getText())) {
                    BigDecimal getBaseBuy = nullToZero(fromString(checkNotNull(getBaseBuyTextField).getText()));

                    AmountAndCommission giveQuoteBuy = LongShortCalculator.calculateQuoteAmountToGiveLong(getBaseBuy, price, fee);

                    checkNotNull(giveQuoteBuyTextField).setText(formatAmount(giveQuoteBuy.amount));
                    checkNotNull(commissionQuoteBuyTextField).setText(formatAmount(giveQuoteBuy.commissionQuote));
                }

                // Update GIVE BASE SELL amount
                if (updateBaseSell && !StringUtils.isBlank(checkNotNull(getQuoteSellTextField).getText())) {
                    BigDecimal getQuoteSell = nullToZero(fromString(checkNotNull(getQuoteSellTextField).getText()));

                    AmountAndCommission giveBaseSell = LongShortCalculator.calculateBaseAmountToGiveShort(getQuoteSell, price, fee);

                    checkNotNull(giveBaseSellTextField).setText(formatAmount(giveBaseSell.amount));
                    checkNotNull(commissionsBaseSellTextField).setText(formatAmount(giveBaseSell.commissionQuote));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error on reverse Recalc:", e);
            JavaFxUtils.showErrorMessage("Error on reverse Recalc: " + e);
        }
    }

    @Nullable
    public Trade getReturnTrade() {
        return returnTrade;
    }

    public void apiSellOrder() {
        try {
            if (StringUtils.isBlank(checkNotNull(priceTextField).getText()) || 
                StringUtils.isBlank(checkNotNull(giveBaseSellTextField).getText())) {
                JavaFxUtils.showErrorMessage("Price and Base amount must be provided.");
                return;
            }

            BigDecimal price = nullToZero(fromString(priceTextField.getText()));
            BigDecimal size = nullToZero(fromString(giveBaseSellTextField.getText()));

            if (price.compareTo(BigDecimal.ZERO) <= 0 || size.compareTo(BigDecimal.ZERO) <= 0) {
                JavaFxUtils.showErrorMessage("Price and Size must be greater than zero.");
                return;
            }

            KucoinCredentialsProvider provider = mainApp.getApiSettingsDialog().getCredentialsProvider();
            Proxy proxy = mainApp.getApiSettingsDialog().getProxy();
            if (provider == null) {
                JavaFxUtils.showErrorMessage("API credentials not available. Please configure in Settings.");
                return;
            }

            OrderCreateResponse response = createMarginSellOrder(provider, proxy, price, size);
            JavaFxUtils.showMessage("Margin Sell Order created successfully! ID: " + response.orderId());
        } catch (Exception e) {
            LOGGER.error("Error creating sell order", e);
            JavaFxUtils.showErrorMessage("Error creating margin sell order: " + e.getMessage());
        }
    }

    public void apiBuyOrder() {
        try {
            if (StringUtils.isBlank(checkNotNull(priceTextField).getText()) || 
                StringUtils.isBlank(checkNotNull(getBaseBuyTextField).getText())) {
                JavaFxUtils.showErrorMessage("Price and Base amount must be provided.");
                return;
            }

            BigDecimal price = nullToZero(fromString(priceTextField.getText()));
            BigDecimal size = nullToZero(fromString(getBaseBuyTextField.getText()));

            if (price.compareTo(BigDecimal.ZERO) <= 0 || size.compareTo(BigDecimal.ZERO) <= 0) {
                JavaFxUtils.showErrorMessage("Price and Size must be greater than zero.");
                return;
            }

            KucoinCredentialsProvider provider = mainApp.getApiSettingsDialog().getCredentialsProvider();
            Proxy proxy = mainApp.getApiSettingsDialog().getProxy();
            if (provider == null) {
                JavaFxUtils.showErrorMessage("API credentials not available. Please configure in Settings.");
                return;
            }

            OrderCreateResponse response = createMarginBuyOrder(provider, proxy, price, size);
            JavaFxUtils.showMessage("Margin Buy Order created successfully! ID: " + response.orderId());
        } catch (Exception e) {
            LOGGER.error("Error creating buy order", e);
            JavaFxUtils.showErrorMessage("Error creating margin buy order: " + e.getMessage());
        }
    }

    protected OrderCreateResponse createSpotBuyOrder(KucoinCredentialsProvider provider, @Nullable Proxy proxy,
                                                     BigDecimal price, BigDecimal size) throws IOException {
        KucoinApiClient apiClient = new KucoinApiClient(provider, proxy);
        OrderCreateRequest request = ImmutableOrderCreateRequest.builder()
                .clientOid(UUID.randomUUID().toString())
                .side("buy")
                .symbol(ticker.getSymbol())
                .type("limit")
                .price(price)
                .size(size)
                .build();

        return apiClient.createOrder(request);
    }

    protected OrderCreateResponse createSpotSellOrder(KucoinCredentialsProvider provider, @Nullable Proxy proxy,
                                                     BigDecimal price, BigDecimal size) throws IOException {
        KucoinApiClient apiClient = new KucoinApiClient(provider, proxy);
        OrderCreateRequest request = ImmutableOrderCreateRequest.builder()
                .clientOid(UUID.randomUUID().toString())
                .side("sell")
                .symbol(ticker.getSymbol())
                .type("limit")
                .price(price)
                .size(size)
                .build();

        return apiClient.createOrder(request);
    }

    protected OrderCreateResponse createMarginBuyOrder(KucoinCredentialsProvider provider, @Nullable Proxy proxy,
                                                     BigDecimal price, BigDecimal size) throws IOException {
        KucoinApiClient apiClient = new KucoinApiClient(provider, proxy);
        OrderCreateRequest request = ImmutableOrderCreateRequest.builder()
                .clientOid(UUID.randomUUID().toString())
                .side("buy")
                .symbol(ticker.getSymbol())
                .type("limit")
                .price(price)
                .size(size)
                .isIsolated(true)
                .autoBorrow(checkNotNull(autoBorrowBuyCheckBox).isSelected())
                .build();

        return apiClient.createMarginOrder(request);
    }

    protected OrderCreateResponse createMarginSellOrder(KucoinCredentialsProvider provider, @Nullable Proxy proxy,
                                                      BigDecimal price, BigDecimal size) throws IOException {
        KucoinApiClient apiClient = new KucoinApiClient(provider, proxy);
        OrderCreateRequest request = ImmutableOrderCreateRequest.builder()
                .clientOid(UUID.randomUUID().toString())
                .side("sell")
                .symbol(ticker.getSymbol())
                .type("limit")
                .price(price)
                .size(size)
                .isIsolated(true)
                .autoBorrow(checkNotNull(autoBorrowSellCheckBox).isSelected())
                .build();

        return apiClient.createMarginOrder(request);
    }
}
