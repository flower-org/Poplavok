package com.poplavok.forms;

import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.utils.AmountAndCommission;
import com.poplavok.data.utils.LongShortCalculator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.flower.fxutils.JavaFxUtils.createDecimalTextFormatter;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.utils.BigDecimalUtil.SCALE;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;

/** Outcome and commission of a single LONG (buy BASE) or SHORT (sell BASE) trade at a given price. */
public class TradeCalculatorDialog extends VBox {
    static final Logger LOGGER = LoggerFactory.getLogger(TradeCalculatorDialog.class);

    @FXML @Nullable TickerPricePicker tickerPricePicker;
    @FXML @Nullable ToggleGroup directionGroup;
    @FXML @Nullable RadioButton longRadioButton;
    @FXML @Nullable ToggleGroup feeGroup;
    @FXML @Nullable RadioButton makerRadioButton;
    @FXML @Nullable TextField feeTextField;
    @FXML @Nullable Label giveLabel;
    @FXML @Nullable TextField giveTextField;
    @FXML @Nullable Label getLabel;
    @FXML @Nullable TextField getTextField;
    @FXML @Nullable Label commissionQuoteLabel;
    @FXML @Nullable TextField commissionQuoteTextField;
    @FXML @Nullable Label commissionBaseLabel;
    @FXML @Nullable TextField commissionBaseTextField;
    @FXML @Nullable Label hintLabel;

    /** Which of "You give" / "You get" the user typed in last; the other one is calculated. */
    boolean giveIsInput = true;
    /** Set while results are written, so those writes aren't treated as user input. */
    boolean updating;

    public TradeCalculatorDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TradeCalculatorDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        TickerPricePicker picker = checkNotNull(tickerPricePicker);
        for (TextField input : new TextField[] { checkNotNull(giveTextField), checkNotNull(getTextField),
                checkNotNull(feeTextField) }) {
            input.setTextFormatter(createDecimalTextFormatter());
        }

        picker.selectedTickerProperty().addListener((observable, oldValue, newValue) -> {
            updateFee();
            recalculate();
        });
        checkNotNull(feeGroup).selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateFee());
        checkNotNull(directionGroup).selectedToggleProperty().addListener((observable, oldValue, newValue) -> recalculate());
        for (TextField input : new TextField[] { picker.priceTextField(), feeTextField }) {
            input.textProperty().addListener((observable, oldValue, newValue) -> recalculate());
        }
        giveTextField.textProperty().addListener((observable, oldValue, newValue) -> onAmountEdited(true));
        getTextField.textProperty().addListener((observable, oldValue, newValue) -> onAmountEdited(false));
        recalculate();
    }

    /** Fills the fee % from the selected ticker (rate x coefficient) for the chosen maker/taker side. */
    void updateFee() {
        MarketTicker ticker = checkNotNull(tickerPricePicker).selectedTickerProperty().get();
        if (ticker == null) {
            return;
        }
        try {
            BigDecimal fee = checkNotNull(makerRadioButton).isSelected() ? ticker.getMakerFee() : ticker.getTakerFee();
            checkNotNull(feeTextField).setText(fee.movePointRight(2).stripTrailingZeros().toPlainString());
        } catch (RuntimeException e) {
            LOGGER.warn("No fees for {}; keeping the entered fee", ticker.getSymbol(), e);
        }
    }

    void onAmountEdited(boolean give) {
        if (updating) {
            return;
        }
        giveIsInput = give;
        recalculate();
    }

    void recalculate() {
        TickerPricePicker picker = checkNotNull(tickerPricePicker);
        String base = picker.baseCurrency();
        String quote = picker.quoteCurrency();
        boolean isLong = checkNotNull(longRadioButton).isSelected();
        boolean amountIsGive = giveIsInput;
        String giveCurrency = isLong ? quote : base;
        String getCurrency = isLong ? base : quote;

        checkNotNull(giveLabel).setText("You give (" + giveCurrency + "):");
        checkNotNull(getLabel).setText("You get (" + getCurrency + "):");
        checkNotNull(commissionQuoteLabel).setText("Commission (" + quote + "):");
        checkNotNull(commissionBaseLabel).setText("Commission (" + base + " equiv.):");

        BigDecimal price = SellDownGameTab.parse(picker.priceTextField());
        TextField inputField = checkNotNull(amountIsGive ? giveTextField : getTextField);
        TextField outputField = checkNotNull(amountIsGive ? getTextField : giveTextField);
        BigDecimal amount = SellDownGameTab.parse(inputField);
        BigDecimal fee = SellDownGameTab.parsePercent(feeTextField);
        if (price.signum() <= 0 || amount.signum() <= 0) {
            clearResults(outputField, "Enter a price and what you give or get.");
            return;
        }
        if (fee.signum() < 0 || fee.compareTo(BigDecimal.ONE) >= 0) {
            clearResults(outputField, "Fee must be between 0% and 100%.");
            return;
        }

        AmountAndCommission result;
        BigDecimal give;
        BigDecimal get;
        if (isLong) {
            if (amountIsGive) {
                result = LongShortCalculator.calculateBaseAmountToGetLong(amount, price, fee);
                give = amount;
                get = result.amount;
            } else {
                result = LongShortCalculator.calculateQuoteAmountToGiveLong(amount, price, fee);
                give = result.amount;
                get = amount;
            }
        } else {
            if (amountIsGive) {
                result = LongShortCalculator.calculateQuoteAmountToGetShort(amount, price, fee);
                give = amount;
                get = result.amount;
            } else {
                result = LongShortCalculator.calculateBaseAmountToGiveShort(amount, price, fee);
                give = result.amount;
                get = amount;
            }
        }

        setOutput(outputField, formatAmount(amountIsGive ? get : give));
        checkNotNull(commissionQuoteTextField).setText(formatAmount(result.commissionQuote));
        checkNotNull(commissionBaseTextField).setText(formatAmount(result.commissionBase));
        if (give.signum() <= 0 || get.signum() <= 0) {
            checkNotNull(hintLabel).setText("Amount too small.");
            return;
        }
        BigDecimal giveInQuote = isLong ? give : give.multiply(price);
        BigDecimal getInQuote = isLong ? get.multiply(price) : get;
        checkNotNull(hintLabel).setText("Effective price: "
                + formatAmount(isLong ? give.divide(get, SCALE, RoundingMode.CEILING)
                        : get.divide(give, SCALE, RoundingMode.FLOOR))
                + " " + quote + " per " + base + ". Value lost to fee: "
                + formatAmount(giveInQuote.subtract(getInQuote)) + " " + quote + ".");
    }

    void clearResults(TextField outputField, String hint) {
        setOutput(outputField, "");
        checkNotNull(commissionQuoteTextField).setText("");
        checkNotNull(commissionBaseTextField).setText("");
        checkNotNull(hintLabel).setText(hint);
    }

    void setOutput(TextField outputField, String text) {
        updating = true;
        try {
            outputField.setText(text);
        } finally {
            updating = false;
        }
    }

    /** Stops price streaming. Safe to call multiple times. */
    public void dispose() {
        checkNotNull(tickerPricePicker).dispose();
    }
}
