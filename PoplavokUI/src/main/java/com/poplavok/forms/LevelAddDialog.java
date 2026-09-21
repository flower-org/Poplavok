package com.poplavok.forms;

import com.poplavok.data.model.Direction;
import com.poplavok.data.model.Level;
import com.poplavok.data.model.LevelState;
import com.poplavok.data.utils.AmountAndCommission;
import com.poplavok.data.utils.LongShortCalculator;
import com.poplavok.data.utils.PriceCalculator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import static com.flower.fxutils.JavaFxUtils.createDecimalTextFormatter;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.utils.BigDecimalUtil.SCALE;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;
import static com.poplavok.data.utils.BigDecimalUtil.fromString;
import static com.poplavok.data.utils.BigDecimalUtil.nullToZero;

public class LevelAddDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(LevelAddDialog.class);

    @FXML @Nullable TextField feeTextField;
    @FXML @Nullable TextField directionTextField;
    @FXML @Nullable TextField tickerTextField;
    @FXML @Nullable TextField priceTextField;
    @FXML @Nullable TextField notesTextField;
    @FXML @Nullable TextField quoteAmountTextField;
    @FXML @Nullable TextField baseAmountTextField;
    @FXML @Nullable TextField commissionTextField;
    @FXML @Nullable Button addButton;
    @FXML @Nullable CheckBox excludeLoansCheckBox;
    @FXML @Nullable Button useCurrentAmountsButton;
    @FXML @Nullable RowConstraints topRowConstraints;

    @FXML @Nullable TextField fxAmountTextField;

    @Nullable Stage stage;

    @Nullable Long levelId = null;
    @Nullable Level level;
    @Nullable volatile Level returnLevel = null;
    final Direction tradeDirection;

    public LevelAddDialog(@Nullable Level level, String ticker, @Nullable BigDecimal price, @Nullable BigDecimal fee, Direction tradeDirection) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LevelAddDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.tradeDirection = tradeDirection;
        checkNotNull(directionTextField).textProperty().setValue(tradeDirection.name());

        checkNotNull(feeTextField).setTextFormatter(createDecimalTextFormatter());
        checkNotNull(priceTextField).setTextFormatter(createDecimalTextFormatter());
        if (baseAmountTextField != null) {
            baseAmountTextField.setTextFormatter(createDecimalTextFormatter());
            baseAmountTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (baseAmountTextField != null && baseAmountTextField.isFocused()) recalculateQuoteFromBase();
            });
        }
        if (quoteAmountTextField != null) {
            quoteAmountTextField.setTextFormatter(createDecimalTextFormatter());
            quoteAmountTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (quoteAmountTextField != null && quoteAmountTextField.isFocused()) recalculateBaseFromQuote();
            });
        }
        if (fxAmountTextField != null) {
            fxAmountTextField.setTextFormatter(createDecimalTextFormatter());
            fxAmountTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (fxAmountTextField != null && fxAmountTextField.isFocused()) recalculateBaseFromFxQuote();
            });
        }

        checkNotNull(priceTextField).textProperty().addListener((observable, oldValue, newValue) -> {
            if (priceTextField != null && priceTextField.isFocused()) recalculateBaseFromQuote();
        });

        if (fee != null) {
            checkNotNull(feeTextField).textProperty().setValue(formatAmount(fee));
        }
        checkNotNull(tickerTextField).textProperty().set(ticker);
        if (level == null) {
            checkNotNull(addButton).textProperty().set("Add New Level");
            if (price != null) {
                checkNotNull(priceTextField).textProperty().set(formatAmount(price));
            }
        } else {
            this.level = level;
            checkNotNull(addButton).textProperty().set("Update Level");
            levelId = level.getId();

            checkNotNull(priceTextField).textProperty().set(formatAmount(level.getProjectedPrice()));
            checkNotNull(notesTextField).textProperty().set(level.getNotes());
            checkNotNull(quoteAmountTextField).textProperty().set(formatAmount(level.getProjectedAmountQuote()));
            checkNotNull(baseAmountTextField).textProperty().set(formatAmount(level.getProjectedAmountBase()));

            checkNotNull(excludeLoansCheckBox).setVisible(true);
            checkNotNull(excludeLoansCheckBox).setManaged(true);
            checkNotNull(useCurrentAmountsButton).setVisible(true);
            checkNotNull(useCurrentAmountsButton).setManaged(true);
            if (topRowConstraints != null) {
                topRowConstraints.setMinHeight(10.0);
                topRowConstraints.setPrefHeight(30.0);
                topRowConstraints.setMaxHeight(Region.USE_COMPUTED_SIZE);
            }
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void useCurrentAmounts() {
        try {
            Level lvl = level;
            if (lvl == null) { return; }

            boolean excludeLoans = excludeLoansCheckBox != null && excludeLoansCheckBox.isSelected();

            BigDecimal projectedBase;
            BigDecimal projectedQuote;
            if (tradeDirection == Direction.LONG) {
                // LONG: hold BASE (available + lent), owe QUOTE (debt)
                BigDecimal holdingsBase = nullToZero(lvl.getAvailableAmountBase());
                if (!excludeLoans) {
                    holdingsBase = holdingsBase.add(nullToZero(lvl.getLentAmountBase()));
                }
                projectedBase = holdingsBase;
                projectedQuote = nullToZero(lvl.getDebtQuote());
            } else {
                // SHORT: hold QUOTE (available + lent), owe BASE (debt)
                BigDecimal holdingsQuote = nullToZero(lvl.getAvailableAmountQuote());
                if (!excludeLoans) {
                    holdingsQuote = holdingsQuote.add(nullToZero(lvl.getLentAmountQuote()));
                }
                projectedQuote = holdingsQuote;
                projectedBase = nullToZero(lvl.getDebtBase());
            }

            checkNotNull(baseAmountTextField).setText(formatAmount(projectedBase));
            checkNotNull(quoteAmountTextField).setText(formatAmount(projectedQuote));

            if (projectedBase.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal projectedPrice = projectedQuote.divide(projectedBase, SCALE, RoundingMode.HALF_UP);
                checkNotNull(priceTextField).setText(formatAmount(projectedPrice));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Use current amounts error: " + e, ButtonType.OK);
            LOGGER.error("Use current amounts error:", e);
            alert.showAndWait();
        }
    }

    public void okClose() {
        try {
            if (level == null) {
                Level level = new Level();
                level.setState(LevelState.INCEPTION);
                level.setId(levelId); // TODO: wtf
                level.setCreationDate(new Date());
                level.setProjectedPrice(nullToZero(fromString(checkNotNull(priceTextField).textProperty().get())));
                level.setProjectedAmountBase(nullToZero(fromString(checkNotNull(baseAmountTextField).textProperty().get())));
                level.setProjectedAmountQuote(nullToZero(fromString(checkNotNull(quoteAmountTextField).textProperty().get())));
                level.setNotes(checkNotNull(notesTextField).textProperty().get());

                returnLevel = level;
            } else {
                level.setProjectedPrice(nullToZero(fromString(checkNotNull(priceTextField).textProperty().get())));
                level.setProjectedAmountBase(nullToZero(fromString(checkNotNull(baseAmountTextField).textProperty().get())));
                level.setProjectedAmountQuote(nullToZero(fromString(checkNotNull(quoteAmountTextField).textProperty().get())));
                level.setNotes(checkNotNull(notesTextField).textProperty().get());

                returnLevel = level;
            }
            checkNotNull(stage).close();
       } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "LevelAddDialog close Error: " + e, ButtonType.OK);
            LOGGER.error("LevelAddDialog close Error:", e);
            alert.showAndWait();
        }
    }

    private void recalculateQuoteFromBase() {
        try {
            BigDecimal base = nullToZero(fromString(checkNotNull(baseAmountTextField).getText().replace(',', '.')));
            BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText().replace(',', '.')));
            BigDecimal fee = nullToZero(fromString(checkNotNull(feeTextField).getText().replace(',', '.')));

            if (price.compareTo(BigDecimal.ZERO) <= 0) { return; }

            AmountAndCommission quote;
            if (tradeDirection == Direction.LONG) {
                quote = LongShortCalculator.calculateQuoteAmountToGiveLong(base, price, fee);
            } else {
                quote = LongShortCalculator.calculateQuoteAmountToGetShort(base, price, fee);
            }

            BigDecimal fxEntryAmount = PriceCalculator.calculateFxEntryAmount(quote.amount, fee);
            checkNotNull(quoteAmountTextField).setText(formatAmount(quote.amount));
            checkNotNull(commissionTextField).setText(formatAmount(fxEntryAmount.multiply(fee)));
            checkNotNull(fxAmountTextField).setText(formatAmount(fxEntryAmount));
        } catch (Exception e) {
            // ignore parsing errors
        }
    }

    private void recalculateBaseFromQuote() {
        try {
            BigDecimal quote = nullToZero(fromString(checkNotNull(quoteAmountTextField).getText().replace(',', '.')));
            BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText().replace(',', '.')));
            BigDecimal fee = nullToZero(fromString(checkNotNull(checkNotNull(feeTextField)).getText().replace(',', '.')));
            BigDecimal fxEntryAmount = PriceCalculator.calculateFxEntryAmount(quote, fee);

            if (price.compareTo(BigDecimal.ZERO) <= 0) { return; }

            AmountAndCommission base;
            if (tradeDirection == Direction.LONG) {
                base = LongShortCalculator.calculateBaseAmountToGetLong(quote, price, fee);
            } else {
                base = LongShortCalculator.calculateBaseAmountToGiveShort(quote, price, fee);
            }

            checkNotNull(baseAmountTextField).setText(formatAmount(base.amount));
//            checkNotNull(commissionTextField).setText(formatAmount(base.commissionQuote));
            checkNotNull(commissionTextField).setText(formatAmount(fxEntryAmount.multiply(fee)));
            checkNotNull(fxAmountTextField).setText(formatAmount(fxEntryAmount));
        } catch (Exception e) {
            // ignore parsing errors
        }
    }

    private void recalculateBaseFromFxQuote() {
        try {
            BigDecimal fxQuoteAmount = nullToZero(fromString(checkNotNull(fxAmountTextField).getText().replace(',', '.')));
            BigDecimal price = nullToZero(fromString(checkNotNull(priceTextField).getText().replace(',', '.')));
            BigDecimal fee = nullToZero(fromString(checkNotNull(checkNotNull(feeTextField)).getText().replace(',', '.')));
            BigDecimal quote = PriceCalculator.calculateAmountFromFxEntry(fxQuoteAmount, fee);

            if (price.compareTo(BigDecimal.ZERO) <= 0) { return; }

            AmountAndCommission base;
            if (tradeDirection == Direction.LONG) {
                base = LongShortCalculator.calculateBaseAmountToGetLong(quote, price, fee);
            } else {
                base = LongShortCalculator.calculateBaseAmountToGiveShort(quote, price, fee);
            }

            checkNotNull(baseAmountTextField).setText(formatAmount(base.amount));
            checkNotNull(quoteAmountTextField).setText(formatAmount(quote));
            checkNotNull(commissionTextField).setText(formatAmount(fxQuoteAmount.multiply(fee)));
        } catch (Exception e) {
            // ignore parsing errors
        }
    }

    @Nullable Level getReturnLevel() {
        return returnLevel;
    }
}
