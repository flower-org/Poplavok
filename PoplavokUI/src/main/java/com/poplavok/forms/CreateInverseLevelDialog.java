package com.poplavok.forms;

import com.flower.fxutils.JavaFxUtils;
import com.poplavok.data.dao.PoplavokDAO;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.Direction;
import com.poplavok.data.model.Level;
import com.poplavok.data.model.LevelState;
import com.poplavok.data.model.Loan;
import com.poplavok.data.model.LoanType;
import com.poplavok.data.model.Poplavok;
import com.poplavok.data.utils.AmountAndCommission;
import com.poplavok.data.utils.DBUtil;
import com.poplavok.data.utils.LongShortCalculator;
import com.poplavok.forms.wrapper.PoplavokAndInverseLevel;
import com.poplavok.forms.wrapper.PoplavokWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.flower.fxutils.JavaFxUtils.createDecimalTextFormatter;
import static com.flower.fxutils.JavaFxUtils.isInvalidAmountChar;
import static com.flower.fxutils.JavaFxUtils.showMessage;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.model.Direction.LONG;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;
import static com.poplavok.data.utils.BigDecimalUtil.fromString;
import static com.poplavok.data.utils.BigDecimalUtil.nullToZero;

public class CreateInverseLevelDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(CreateInverseLevelDialog.class);

    @FXML @Nullable Label directionLabel;

    @FXML @Nullable RadioButton createNewRadioButton;
    @FXML @Nullable RadioButton useExistingRadioButton;

    @FXML @Nullable TextField newPoplavokNameTextField;
    @FXML @Nullable ComboBox<PoplavokWrapper> existingPoplavokComboBox;

    @FXML @Nullable TextField feeTextField;
    @FXML @Nullable TextField priceTextField;

    @FXML @Nullable TextField amountTextField;
    @FXML @Nullable TextField proceedsTextField;
    @FXML @Nullable TextField commissionTextField;
    @FXML @Nullable Label commissionCurrencyLabel;

    @FXML @Nullable Label proceedsCurrencyLabel;
    @FXML @Nullable Label amountCurrencyLabel;
    @FXML @Nullable Label priceTickerLabel;
    @FXML @Nullable Button availableCurrencyButton;
    @FXML @Nullable TextField availableTextField;

    @FXML @Nullable TextField amountPercentTextField;
    @FXML @Nullable Slider amountPercentSlider;
    @FXML @Nullable ChoiceBox<String> amountPercentTypeComboBox;

    @FXML @Nullable TextField totalTextField;
    @FXML @Nullable Label totalCurrencyLabel;

    @Nullable Stage stage;

    final Poplavok sourcePoplavok;
    final List<Level> sourceLevels;
    final Direction direction;
    final BigDecimal availableAmount;
    final BigDecimal totalAmount;
    private boolean isUpdatingAmount = false;

    @Nullable FilteredList<PoplavokWrapper> destinationPoplavoks;

    @Nullable volatile PoplavokAndInverseLevel returnPoplavokAndInverseLevel = null;

    public CreateInverseLevelDialog(List<Level> sourceLevels, Poplavok sourcePoplavok, @Nullable BigDecimal fee, @Nullable BigDecimal price) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("CreateInverseLevelDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.sourceLevels = sourceLevels;
        this.sourcePoplavok = sourcePoplavok;

        List<Poplavok> poplavoks = DBUtil.connectGetResultAndClose(
                sess -> PoplavokDAO.findInversePoplavoks(sess, checkNotNull(sourcePoplavok.getTicker()), checkNotNull(sourcePoplavok.getDirection())));
        this.destinationPoplavoks = new FilteredList<>(FXCollections.observableArrayList(
                poplavoks.stream().map(PoplavokWrapper::new).toList()
            ));
        checkNotNull(existingPoplavokComboBox).setItems(destinationPoplavoks);
        checkNotNull(newPoplavokNameTextField).setText("Inverse " + sourcePoplavok.getName());
        if (!poplavoks.isEmpty()) {
            existingPoplavokComboBox.getSelectionModel().selectFirst();
        }

        direction = checkNotNull(sourcePoplavok.getDirection()) == Direction.LONG ? Direction.SHORT : Direction.LONG;

        checkNotNull(directionLabel).textProperty().setValue(direction.name());

        checkNotNull(amountTextField).setTextFormatter(createDecimalTextFormatter());
        checkNotNull(feeTextField).setTextFormatter(createDecimalTextFormatter());
        checkNotNull(priceTextField).setTextFormatter(createDecimalTextFormatter());
        checkNotNull(proceedsTextField).setTextFormatter(createDecimalTextFormatter());

        checkNotNull(createNewRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> updateControlsState());
        checkNotNull(useExistingRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> updateControlsState());

        checkNotNull(priceTickerLabel).textProperty().setValue(sourcePoplavok.getTicker().getSymbol());
        String base = sourcePoplavok.getTicker().getBase().getCurrency();
        String quote = sourcePoplavok.getTicker().getQuote().getCurrency();
        if (direction == Direction.SHORT) {
            checkNotNull(proceedsCurrencyLabel).textProperty().setValue(quote);
            checkNotNull(amountCurrencyLabel).textProperty().setValue(base);
            checkNotNull(totalCurrencyLabel).textProperty().setValue(base);
            checkNotNull(availableCurrencyButton).textProperty().setValue(base);
        } else { //if (direction == Direction.LONG) {
            checkNotNull(proceedsCurrencyLabel).textProperty().setValue(base);
            checkNotNull(amountCurrencyLabel).textProperty().setValue(quote);
            checkNotNull(totalCurrencyLabel).textProperty().setValue(quote);
            checkNotNull(availableCurrencyButton).textProperty().setValue(quote);
        }

        checkNotNull(commissionCurrencyLabel).textProperty().setValue(quote);
        checkNotNull(priceTextField).textProperty().addListener(this::onPriceChanged);
        checkNotNull(feeTextField).textProperty().addListener(this::onFeeChanged);

        checkNotNull(feeTextField).textProperty().setValue(formatAmount(nullToZero(fee)));
        checkNotNull(priceTextField).textProperty().setValue(formatAmount(nullToZero(price)));

        BigDecimal available = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Level lvl : sourceLevels) {
            if (direction == Direction.SHORT) {
                available = available.add(nullToZero(lvl.getAvailableAmountBase()));
                total = total.add(nullToZero(lvl.getAvailableAmountBase())).add(nullToZero(lvl.getLentAmountBase()));
            } else { //if (direction == Direction.LONG) {
                available = available.add(nullToZero(lvl.getAvailableAmountQuote()));
                total = total.add(nullToZero(lvl.getAvailableAmountQuote())).add(nullToZero(lvl.getLentAmountQuote()));
            }
        }
        this.availableAmount = available;
        this.totalAmount = total;

        checkNotNull(availableTextField).textProperty().setValue(formatAmount(available));
        checkNotNull(totalTextField).textProperty().setValue(formatAmount(total));

        checkNotNull(amountPercentSlider).setMin(0);
        checkNotNull(amountPercentSlider).setMax(100);
        checkNotNull(amountPercentSlider).valueProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingAmount) return;
            isUpdatingAmount = true;
            try {
                double val = newValue.doubleValue();
                
                // Snap to closest special value
                double[] snapValues = {0.0, 16.6, 20.0, 25.0, 33.3, 50.0, 66.6, 75.0, 80.0, 83.3, 100.0};
                double closest = snapValues[0];
                double minDiff = Math.abs(val - closest);
                for (double snap : snapValues) {
                    double diff = Math.abs(val - snap);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closest = snap;
                    }
                }
                val = closest;
                checkNotNull(amountPercentSlider).setValue(val);

                BigDecimal percent = BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP);
                checkNotNull(amountPercentTextField).setText(formatAmount(percent));
                updateAmountFromPercent(percent);
            } finally {
                isUpdatingAmount = false;
            }
        });

        checkNotNull(amountPercentTextField).textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingAmount) return;
            isUpdatingAmount = true;
            try {
                if (!StringUtils.isBlank(newValue)) {
                    BigDecimal percent = fromString(newValue);
                    if (percent != null && percent.compareTo(BigDecimal.ZERO) >= 0 && percent.compareTo(BigDecimal.valueOf(100)) <= 0) {
                        checkNotNull(amountPercentSlider).setValue(percent.doubleValue());
                        updateAmountFromPercent(percent);
                    }
                }
            } catch (Exception e) {
                // ignore
            } finally {
                isUpdatingAmount = false;
            }
        });

        checkNotNull(amountPercentTypeComboBox).valueProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingAmount) return;
            isUpdatingAmount = true;
            try {
                String pctStr = checkNotNull(amountPercentTextField).getText();
                if (!StringUtils.isBlank(pctStr)) {
                    BigDecimal percent = fromString(pctStr);
                    if (percent != null) {
                        updateAmountFromPercent(percent);
                    }
                }
            } catch (Exception e) {
                // ignore
            } finally {
                isUpdatingAmount = false;
            }
        });

        checkNotNull(amountTextField).textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingAmount) return;
            isUpdatingAmount = true;
            try {
                if (!StringUtils.isBlank(newValue)) {
                    BigDecimal amount = fromString(newValue);
                    amount = amount == null ? BigDecimal.ZERO : amount;
                    updatePercentFromAmount(amount);
                }
            } catch (Exception e) {
                // ignore
            } finally {
                isUpdatingAmount = false;
            }
        });

        updateControlsState();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void addAllAvailable() {
        try {
            if (!StringUtils.isBlank(checkNotNull(availableTextField).getText())) {
                BigDecimal availableBase = nullToZero(fromString(checkNotNull(availableTextField).getText()));
                checkNotNull(amountTextField).textProperty().setValue(formatAmount(availableBase));

                priceFeeUpdate();
            }
        } catch (Exception e) {
            LOGGER.error("Error on useAllBase:", e);
            JavaFxUtils.showErrorMessage("Error on useAllBase: " + e);
        }
    }

    public void okClose() {
        try {
            BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText()));

            // 1. Create poplavok if it's not supplied
            boolean isNewPoplavok;
            Poplavok poplavok;
            if (checkNotNull(createNewRadioButton).selectedProperty().get()) {
                isNewPoplavok = true;

                poplavok = new Poplavok();
                poplavok.setCreationDate(new Date());
                poplavok.setActive(true);
                poplavok.setTicker(sourcePoplavok.getTicker());
                poplavok.setName(checkNotNull(newPoplavokNameTextField).textProperty().get());
                poplavok.setDirection(direction);
            } else if (checkNotNull(useExistingRadioButton).selectedProperty().get()) {
                isNewPoplavok = false;

                PoplavokWrapper poplavokWrapper = checkNotNull(existingPoplavokComboBox).getSelectionModel().getSelectedItem();
                if (poplavokWrapper == null) {
                    showMessage("Please select poplavok");
                    return;
                }
                poplavok = poplavokWrapper.poplavok();
            } else {
                showMessage("Unknown poplavok selection");
                return;
            }

            Currency loanCurrency;
            if (direction == LONG) {
                loanCurrency = sourcePoplavok.getTicker().getQuote();
            } else { //if (direction == SHORT) {
                loanCurrency = sourcePoplavok.getTicker().getBase();
            }

            // 2. Create a new level for this poplavok to transfer retained debt and amount to
            Level newLevel = new Level();
            newLevel.setState(LevelState.INCEPTION);
            newLevel.setCreationDate(new Date());
            newLevel.setPoplavok(checkNotNull(poplavok));
            newLevel.setProjectedPrice(nullToZero(price));
            newLevel.setState(LevelState.TRADING);

            BigDecimal amount = nullToZero(fromString(checkNotNull(amountTextField).getText()));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showMessage("Amount to transfer to the inverted level must be positive");
                return;
            }
            if (amount.compareTo(availableAmount) > 0) {
                showMessage("Amount to transfer to the inverted level can't be larger than the available amount on source levels");
                return;
            }

            BigDecimal proceeds = nullToZero(fromString(checkNotNull(proceedsTextField).getText()));
            if (direction == LONG) {
                newLevel.setProjectedAmountQuote(amount);
                newLevel.setProjectedAmountBase(proceeds);
            } else { //if (direction == SHORT) {
                newLevel.setProjectedAmountBase(amount);
                newLevel.setProjectedAmountQuote(proceeds);
            }

            // 3. Loan from source levels
            BigDecimal remainingAmountToFill = amount;
            StringBuilder lvlInfo = new StringBuilder();
            List<Level> initialLoanSourceLevels = new ArrayList<>();
            List<Loan> initialLoans = new ArrayList<>();
            for (Level lvl : sourceLevels) {
                if (remainingAmountToFill.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }

                final BigDecimal levelAvailable;
                if (direction == Direction.SHORT) {
                    levelAvailable = nullToZero(lvl.getAvailableAmountBase());
                } else { //if (direction == Direction.LONG) {
                    levelAvailable = nullToZero(lvl.getAvailableAmountQuote());
                }

                if (levelAvailable.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal levelLoanAmount;
                    if (levelAvailable.compareTo(remainingAmountToFill) > 0) {
                        levelLoanAmount = remainingAmountToFill;
                    } else {
                        levelLoanAmount = levelAvailable;
                    }
                    remainingAmountToFill = remainingAmountToFill.subtract(levelLoanAmount);

                    // update level
                    if (direction == Direction.SHORT) {
                        lvl.setAvailableAmountBase(nullToZero(lvl.getAvailableAmountBase()).subtract(levelLoanAmount));
                        lvl.setLentAmountBase(nullToZero(lvl.getLentAmountBase()).add(levelLoanAmount));

                        newLevel.setAvailableAmountBase(nullToZero(newLevel.getAvailableAmountBase()).add(levelLoanAmount));
                        newLevel.setDebtBase(nullToZero(newLevel.getDebtBase()).add(levelLoanAmount));
                    } else { //if (direction == Direction.LONG) {
                        lvl.setAvailableAmountQuote(nullToZero(lvl.getAvailableAmountQuote()).subtract(levelLoanAmount));
                        lvl.setLentAmountQuote(nullToZero(lvl.getLentAmountQuote()).add(levelLoanAmount));

                        newLevel.setAvailableAmountQuote(nullToZero(newLevel.getAvailableAmountQuote()).add(levelLoanAmount));
                        newLevel.setDebtQuote(nullToZero(newLevel.getDebtQuote()).add(levelLoanAmount));
                    }

                    initialLoanSourceLevels.add(lvl);

                    // create loan
                    Loan loan = new Loan(loanCurrency, levelLoanAmount, newLevel, new Date(), LoanType.POPLAVOK_FUNDED);
                    loan.setSourceLevel(lvl);
                    loan.setActive(true);
                    loan.setNotes("Inverse level loan");

                    initialLoans.add(loan);
                }

                List<String> levelNotes = initialLoanSourceLevels.stream().map(l -> StringUtils.defaultIfBlank(l.getNotes(), "")).toList();
                newLevel.setNotes("Inv " + String.join(",", levelNotes));
            }

            returnPoplavokAndInverseLevel = new PoplavokAndInverseLevel(isNewPoplavok, poplavok, newLevel, initialLoans, initialLoanSourceLevels);

            checkNotNull(stage).close();
       } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "CreateInverseLevelDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("CreateInverseLevelDialog close Error:", e);
            alert.showAndWait();
        }
    }

    @Nullable PoplavokAndInverseLevel getReturnPoplavokAndInverseLevel() {
        return returnPoplavokAndInverseLevel;
    }

    private void updateControlsState() {
        if (checkNotNull(createNewRadioButton).isSelected()) {
            checkNotNull(newPoplavokNameTextField).setDisable(false);
            checkNotNull(newPoplavokNameTextField).setEditable(true);
            checkNotNull(existingPoplavokComboBox).setDisable(true);
        } else if (checkNotNull(useExistingRadioButton).isSelected()) {
            checkNotNull(newPoplavokNameTextField).setDisable(true);
            checkNotNull(newPoplavokNameTextField).setEditable(false);
            checkNotNull(existingPoplavokComboBox).setDisable(false);
        }
    }

    public void onPriceChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        priceFeeUpdate();
    }

    public void onFeeChanged(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        priceFeeUpdate();
    }

    public void onAmountChanged(@Nullable KeyEvent event) {
        if (isInvalidAmountChar(event)) { return; }
        priceFeeUpdate();
    }

    public void onProceedsChanged(@Nullable KeyEvent event) {
        if (isInvalidAmountChar(event)) { return; }
        reversePriceFeeUpdate();
    }

    /** Update quote sell, base buy, if manual input is not checked */
    protected void priceFeeUpdate() {
        try {
            if (StringUtils.isBlank(checkNotNull(priceTextField).getText())) { return; }
            if (StringUtils.isBlank(checkNotNull(feeTextField).getText())) { return; }
            if (StringUtils.isBlank(checkNotNull(amountTextField).getText())) { return; }
            BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText()));
            BigDecimal fee = nullToZero(fromString(checkNotNull(feeTextField).getText()));

            if (price.compareTo(BigDecimal.ZERO) == 0) { return; }

            if (direction == Direction.LONG) {
                // Update GET BASE BUY amount
                BigDecimal giveQuoteBuy = nullToZero(fromString(checkNotNull(amountTextField).getText()));

                AmountAndCommission getBaseBuy = LongShortCalculator.calculateBaseAmountToGetLong(giveQuoteBuy, price, fee);

                checkNotNull(proceedsTextField).setText(formatAmount(getBaseBuy.amount));
                checkNotNull(commissionTextField).setText(formatAmount(getBaseBuy.commissionQuote));
            } else {
                // Update GET QUOTE SELL amount
                BigDecimal giveBaseSell = nullToZero(fromString(checkNotNull(amountTextField).getText()));

                AmountAndCommission getQuoteSell = LongShortCalculator.calculateQuoteAmountToGetShort(giveBaseSell, price, fee);

                checkNotNull(proceedsTextField).setText(formatAmount(getQuoteSell.amount));
                checkNotNull(commissionTextField).setText(formatAmount(getQuoteSell.commissionQuote));
            }
        } catch (Exception e) {
            LOGGER.error("Error on Recalc:", e);
            JavaFxUtils.showErrorMessage("Error on Recalc: " + e);
        }
    }

    /** Update quote buy, base sell, if manual input is not checked */
    protected void reversePriceFeeUpdate() {
        try {
            if (StringUtils.isBlank(checkNotNull(priceTextField).getText())) { return; }
            if (StringUtils.isBlank(checkNotNull(feeTextField).getText())) { return; }
            if (StringUtils.isBlank(checkNotNull(proceedsTextField).getText())) { return; }
            BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText()));
            BigDecimal fee = nullToZero(fromString(checkNotNull(feeTextField).getText()));

            if (direction == Direction.LONG) {
                // Update GIVE QUOTE BUY amount
                BigDecimal getBaseBuy = nullToZero(fromString(checkNotNull(proceedsTextField).getText()));

                AmountAndCommission giveQuoteBuy = LongShortCalculator.calculateQuoteAmountToGiveLong(getBaseBuy, price, fee);

                checkNotNull(amountTextField).setText(formatAmount(giveQuoteBuy.amount));
                checkNotNull(commissionTextField).setText(formatAmount(giveQuoteBuy.commissionQuote));
            } else {
                // Update GIVE BASE SELL amount
                BigDecimal getQuoteSell = nullToZero(fromString(checkNotNull(proceedsTextField).getText()));

                AmountAndCommission giveBaseSell = LongShortCalculator.calculateBaseAmountToGiveShort(getQuoteSell, price, fee);

                checkNotNull(amountTextField).setText(formatAmount(giveBaseSell.amount));
                checkNotNull(commissionTextField).setText(formatAmount(giveBaseSell.commissionQuote));
            }
        } catch (Exception e) {
            LOGGER.error("Error on reverse Recalc:", e);
            JavaFxUtils.showErrorMessage("Error on reverse Recalc: " + e);
        }
    }

    private void updateAmountFromPercent(BigDecimal percent) {
        BigDecimal baseValue;
        if ("% Total".equals(checkNotNull(amountPercentTypeComboBox).getValue())) {
            baseValue = totalAmount;
        } else {
            baseValue = availableAmount;
        }
        
        BigDecimal amount = baseValue.multiply(percent).divide(BigDecimal.valueOf(100), 8, java.math.RoundingMode.HALF_UP);
        checkNotNull(amountTextField).setText(formatAmount(amount));
        priceFeeUpdate();
    }

    private void updatePercentFromAmount(BigDecimal amount) {
        BigDecimal baseValue;
        if ("% Total".equals(checkNotNull(amountPercentTypeComboBox).getValue())) {
            baseValue = totalAmount;
        } else {
            baseValue = availableAmount;
        }
        
        if (baseValue.compareTo(BigDecimal.ZERO) == 0) {
            checkNotNull(amountPercentTextField).setText("0");
            checkNotNull(amountPercentSlider).setValue(0);
            return;
        }
        
        BigDecimal percentBd = amount.multiply(BigDecimal.valueOf(100)).divide(baseValue, 2, java.math.RoundingMode.HALF_UP);
        if (percentBd.compareTo(BigDecimal.valueOf(100)) > 0) percentBd = BigDecimal.valueOf(100);
        if (percentBd.compareTo(BigDecimal.ZERO) < 0) percentBd = BigDecimal.ZERO;
        
        checkNotNull(amountPercentTextField).setText(formatAmount(percentBd));
        checkNotNull(amountPercentSlider).setValue(percentBd.doubleValue());
    }
}
