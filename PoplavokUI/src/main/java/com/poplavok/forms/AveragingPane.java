package com.poplavok.forms;

import com.poplavok.data.model.Direction;
import com.poplavok.data.model.Level;
import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.utils.AmountAndCommission;
import com.poplavok.data.utils.BuyPriceInfo;
import com.poplavok.data.utils.LongShortCalculator;
import com.poplavok.data.utils.PriceCalculator;
import com.poplavok.data.utils.PriceInfo;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

import static com.flower.fxutils.JavaFxUtils.createDecimalTextFormatter;
import static com.flower.fxutils.JavaFxUtils.showMessage;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.model.Direction.LONG;
import static com.poplavok.data.model.Direction.SHORT;
import static com.poplavok.data.utils.BigDecimalUtil.SCALE;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;
import static com.poplavok.data.utils.BigDecimalUtil.fromString;
import static com.poplavok.data.utils.BigDecimalUtil.nullToZero;

public class AveragingPane extends AnchorPane {
    final static Logger LOGGER = LoggerFactory.getLogger(AveragingPane.class);

    public static final BigDecimal ONE_HUNDRED = nullToZero(fromString("100"));

    @FXML @Nullable TextField debtTextField;
    @FXML @Nullable TextField availableTextField;
    @FXML @Nullable TextField toRepayTextField;
    @FXML @Nullable TextField holdingTextField;

    @FXML @Nullable Label debtCurrencyLabel;
    @FXML @Nullable Label availableCurrencyLabel;
    @FXML @Nullable Label toRepayCurrencyLabel;
    @FXML @Nullable Label holdingCurrencyLabel;

    @FXML @Nullable RadioButton commsRadioButton;
    @FXML @Nullable RadioButton percentRadioButton;
    @FXML @Nullable TextField commsCountTextField;
    @FXML @Nullable Slider commsCountSlider;
    @FXML @Nullable TextField percentTextField;
    @FXML @Nullable RadioButton priceRadioButton;
    @FXML @Nullable TextField targetPriceTextField;
    @FXML @Nullable CheckBox syncPriceCheckBox;

    @FXML @Nullable TextField toSellTextField;
    @FXML @Nullable TextField sellPriceTextField;
    @FXML @Nullable TextField proceedsTextField;
    @FXML @Nullable TextField commissionTextField;
    @FXML @Nullable TextField profitTextField;
    @FXML @Nullable TextField profitQuoteTextField;

    @FXML @Nullable RadioButton profitInQuoteRadioButton;
    @FXML @Nullable RadioButton profitInBaseRadioButton;

    @FXML @Nullable Label fxUiEntryCurrencyLabel;
    @FXML @Nullable TextField fxUiEntryTextField;

    @FXML @Nullable Label toTradeCurrencyLabel;
    @FXML @Nullable Label tradePriceLabel;
    @FXML @Nullable Label proceedsCurrencyLabel;
    @FXML @Nullable Label commissionCurrencyLabel;
    @FXML @Nullable Label profitCurrencyLabel;
    @FXML @Nullable Label profitQuoteCurrencyLabel;

    @FXML @Nullable Label poplavokDirectionLabel;
    @FXML @Nullable Label poplavokTickerLabel;
    @FXML @Nullable Label averagingActionLabel;
    @FXML @Nullable Label averagingCurrencyLabel;

    @FXML @Nullable CheckBox includeLentAmountsCheckBox;
    @FXML @Nullable CheckBox excludeLoansFromHoldingsCheckBox;

    // Retain holdings

    @FXML @Nullable CheckBox retainHoldingsCheckBox;
    @FXML @Nullable CheckBox retainCommissionCheckBox;

    @FXML @Nullable Label retainCaptionLabel;
    @FXML @Nullable Label retainWorthCaptionLabel;
    @FXML @Nullable Label retainCommissionCaptionLabel;

    @FXML @Nullable TextField retainTextField;
    @FXML @Nullable TextField retainWorthTextField;
    @FXML @Nullable TextField retainCommissionTextField;
    @FXML @Nullable Label retainedCommissionInfoLabel;

    @FXML @Nullable Label retainCurrencyLabel;
    @FXML @Nullable Label retainWorhCurrencyLabel;
    @FXML @Nullable Label retainCommisionCurrencyLabel;

    protected final MarketTicker ticker;
    protected Collection<Level> averageLevels;
    protected final Direction direction;
    protected final BigDecimal fee;
    protected @Nullable BigDecimal mainPrice;

    public AveragingPane(MarketTicker ticker, Direction direction, Collection<Level> lvls, BigDecimal fee) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AveragingPane.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        this.averageLevels = lvls;
        this.ticker = ticker;
        this.direction = direction;
        this.fee = fee;

        checkNotNull(commsRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> updateAddModeControls());
        checkNotNull(percentRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> updateAddModeControls());
        checkNotNull(priceRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateAddModeControls();
            if (newValue) { updateAverageTabEvent(); }
        });

        checkNotNull(profitInQuoteRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && ticker != null) {
                checkNotNull(profitCurrencyLabel).textProperty().setValue(ticker.getQuote().getCurrency());
                updateAverageTabEvent();
            }
        });
        checkNotNull(profitInBaseRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && ticker != null) {
                checkNotNull(profitCurrencyLabel).textProperty().setValue(ticker.getBase().getCurrency());
                updateAverageTabEvent();
            }
        });
        checkNotNull(commsRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { updateAverageTabEvent(); }
        });
        checkNotNull(percentRadioButton).selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) { updateAverageTabEvent(); }
        });
        checkNotNull(includeLentAmountsCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateAverageTabEvent();
        });
        checkNotNull(excludeLoansFromHoldingsCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateAverageTabEvent();
        });

        updateAddModeControls();

        checkNotNull(commsCountSlider).valueProperty().addListener((observable, oldValue, newValue) -> {
            checkNotNull(commsCountTextField).setText(String.valueOf(newValue.intValue()));
            updateAverageTabEvent();
        });

        checkNotNull(percentTextField).setTextFormatter(createDecimalTextFormatter());
        checkNotNull(targetPriceTextField).setTextFormatter(createDecimalTextFormatter());
        checkNotNull(targetPriceTextField).textProperty().addListener((observable, oldValue, newValue) -> {
            if (checkNotNull(priceRadioButton).isSelected()) { updateAverageTabEvent(); }
        });
        checkNotNull(syncPriceCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateAddModeControls();
            if (newValue) { setMainPrice(mainPrice); }
        });
        checkNotNull(targetPriceTextField).setOnMouseClicked(event -> {
            CheckBox sync = checkNotNull(syncPriceCheckBox);
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !sync.isDisabled()) {
                sync.setSelected(!sync.isSelected());
            }
        });
        checkNotNull(retainTextField).setTextFormatter(createDecimalTextFormatter());
        checkNotNull(retainWorthTextField).setTextFormatter(createDecimalTextFormatter());

        checkNotNull(retainHoldingsCheckBox).selectedProperty().addListener((observable, oldValue, enable) -> {
            boolean disable = !enable;
            switchDisableRetainControls(disable);
            if (!disable) {
                updateRetainedAmountByQuote();
            }
        });

        checkNotNull(retainCommissionCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> {
            updateRetainedAmountByQuote();
        });

        updateAverageTabEvent();
    }

    public void updateAverageTabEvent() {
        updateAverageTab(checkNotNull(averageLevels));
    }

    protected void updateAddModeControls() {
        boolean comms = checkNotNull(commsRadioButton).isSelected();
        boolean percent = checkNotNull(percentRadioButton).isSelected();
        boolean price = checkNotNull(priceRadioButton).isSelected();
        checkNotNull(commsCountTextField).setDisable(!comms);
        checkNotNull(commsCountSlider).setDisable(!comms);
        checkNotNull(percentTextField).setDisable(!percent);
        checkNotNull(targetPriceTextField).setDisable(!price);
        boolean synced = checkNotNull(syncPriceCheckBox).isSelected();
        checkNotNull(targetPriceTextField).setEditable(!synced);
        checkNotNull(targetPriceTextField).setStyle(synced ? "-fx-control-inner-background: #e8e8e8;" : "");
        checkNotNull(syncPriceCheckBox).setDisable(!price);
    }

    /** Receives the main tab's price; mirrored into the target price field while Sync is checked. */
    public void setMainPrice(@Nullable BigDecimal price) {
        this.mainPrice = price;
        if (checkNotNull(syncPriceCheckBox).isSelected()) {
            checkNotNull(targetPriceTextField).setText(price == null ? "" : formatAmount(price));
        }
    }

    public void switchDisableRetainControls(boolean disable) {
        checkNotNull(retainCommissionCheckBox).disableProperty().setValue(disable);

        checkNotNull(retainCaptionLabel).disableProperty().setValue(disable);
        checkNotNull(retainWorthCaptionLabel).disableProperty().setValue(disable);
        checkNotNull(retainCommissionCaptionLabel).disableProperty().setValue(disable);

        checkNotNull(retainTextField).disableProperty().setValue(disable);
        checkNotNull(retainWorthTextField).disableProperty().setValue(disable);
        checkNotNull(retainCommissionTextField).disableProperty().setValue(disable);
        checkNotNull(retainedCommissionInfoLabel).disableProperty().setValue(disable);

        checkNotNull(retainCurrencyLabel).disableProperty().setValue(disable);
        checkNotNull(retainWorhCurrencyLabel).disableProperty().setValue(disable);
        checkNotNull(retainCommisionCurrencyLabel).disableProperty().setValue(disable);
    }

    public void updateRetainedAmountByQuote() {
        if (direction == LONG) {
            updateRetainedAmountByWorth();
        } else if (direction == SHORT) {
            updateRetainedAmountByRetained();
        } else {
            throw new RuntimeException("Unknown direction " + direction);
        }
    }

    public void updateRetainedAmountByRetained() {
        String retainedAmountStr = checkNotNull(retainTextField).textProperty().get();
        BigDecimal retainedAmount = nullToZero(fromString(retainedAmountStr));

        BigDecimal fee = checkNotNull(retainCommissionCheckBox).selectedProperty().get() ? this.fee : BigDecimal.ZERO;
        BigDecimal price = getAveragingPrice();

        BigDecimal debtAmount;
        BigDecimal commissionAmount;
        if (BigDecimal.ZERO.compareTo(retainedAmount) != 0 && BigDecimal.ZERO.compareTo(price) != 0) {
            AmountAndCommission aac;
            if (direction == LONG) {
                aac = LongShortCalculator.calculateQuoteAmountToGiveLong(retainedAmount, price, checkNotNull(fee));
                commissionAmount = aac.commissionQuote;
            } else if (direction == SHORT) {
                aac = LongShortCalculator.calculateBaseAmountToGiveShort(retainedAmount, price, checkNotNull(fee));
                commissionAmount = aac.commissionBase;
            } else {
                throw new RuntimeException("Unknown Direction " + direction);
            }

            debtAmount = aac.amount;
        } else {
            debtAmount = BigDecimal.ZERO;
            commissionAmount = BigDecimal.ZERO;
            checkNotNull(retainTextField).textProperty().set(formatAmount(retainedAmount));
        }

        checkNotNull(retainWorthTextField).textProperty().setValue(formatAmount(debtAmount));
        checkNotNull(retainCommissionTextField).textProperty().setValue(formatAmount(commissionAmount));
    }

    public void updateRetainedAmountByWorth() {
        String debtAmountStr = checkNotNull(retainWorthTextField).textProperty().get();
        BigDecimal debtAmount = nullToZero(fromString(debtAmountStr));

        BigDecimal fee = checkNotNull(retainCommissionCheckBox).selectedProperty().get() ? this.fee : BigDecimal.ZERO;
        BigDecimal price = getAveragingPrice();

        BigDecimal retainedAmount;
        BigDecimal commissionAmount;
        if (BigDecimal.ZERO.compareTo(debtAmount) != 0 && BigDecimal.ZERO.compareTo(price) != 0) {
            AmountAndCommission aac;
            if (direction == LONG) {
                aac = LongShortCalculator.calculateBaseAmountToGetLong(debtAmount, price, checkNotNull(fee));
                commissionAmount = aac.commissionQuote;
            } else if (direction == SHORT) {
                aac = LongShortCalculator.calculateQuoteAmountToGetShort(debtAmount, price, checkNotNull(fee));
                commissionAmount = aac.commissionBase;
            } else {
                throw new RuntimeException("Unknown Direction " + direction);
            }

            retainedAmount = aac.amount;
        } else {
            retainedAmount = BigDecimal.ZERO;
            commissionAmount = BigDecimal.ZERO;
            checkNotNull(retainWorthTextField).textProperty().set(formatAmount(debtAmount));
        }

        checkNotNull(retainTextField).textProperty().setValue(formatAmount(retainedAmount));
        checkNotNull(retainCommissionTextField).textProperty().setValue(formatAmount(commissionAmount));
    }

    public void updateAverageTab(Collection<Level> lvls) {
        if (lvls == null) { lvls = List.of(); }
        averageLevels = lvls;

        boolean includeLentAmounts = checkNotNull(includeLentAmountsCheckBox).selectedProperty().get();
        boolean excludeLoansFromHoldings = checkNotNull(excludeLoansFromHoldingsCheckBox).selectedProperty().get();

        BigDecimal absoluteDebt = BigDecimal.ZERO;
        BigDecimal available = BigDecimal.ZERO;
        BigDecimal holding = BigDecimal.ZERO;
        BigDecimal loans = BigDecimal.ZERO;

        for (Level lvl : averageLevels) {
            if (checkNotNull(direction) == LONG) {
                absoluteDebt = absoluteDebt.add(nullToZero(lvl.getDebtQuote()));
                available = available.add(nullToZero(lvl.getAvailableAmountQuote()));
                loans = loans.add(nullToZero(lvl.getLentAmountBase()));
                holding = holding.add(nullToZero(lvl.getAvailableAmountBase()));
                if (includeLentAmounts) {
                    holding = holding.add(nullToZero(lvl.getLentAmountBase()));
                }
            } else {
                absoluteDebt = absoluteDebt.add(nullToZero(lvl.getDebtBase()));
                available = available.add(nullToZero(lvl.getAvailableAmountBase()));
                loans = loans.add(nullToZero(lvl.getLentAmountQuote()));
                holding = holding.add(nullToZero(lvl.getAvailableAmountQuote()));
                if (includeLentAmounts) {
                    holding = holding.add(nullToZero(lvl.getLentAmountQuote()));
                }
            }
        }
        BigDecimal toRepay = absoluteDebt.subtract(available);

        checkNotNull(debtTextField).setText(formatAmount(absoluteDebt));
        checkNotNull(availableTextField).setText(formatAmount(available));
        checkNotNull(toRepayTextField).setText(formatAmount(toRepay));
        checkNotNull(holdingTextField).setText(formatAmount(holding));

        if (!excludeLoansFromHoldings) {
            holding = holding.add(loans);
        }

        // ------------------------

        BigDecimal profitRate = BigDecimal.ZERO;
        boolean priceMode = checkNotNull(priceRadioButton).selectedProperty().get();

        if (checkNotNull(commsRadioButton).selectedProperty().get()) {
            BigDecimal commsNumber = nullToZero(fromString(checkNotNull(commsCountTextField).textProperty().get()));
            profitRate = checkNotNull(fee).multiply(commsNumber);
        } else if (checkNotNull(percentRadioButton).selectedProperty().get()) {
            profitRate = nullToZero(fromString(checkNotNull(percentTextField).textProperty().get())).divide(ONE_HUNDRED, SCALE, RoundingMode.CEILING);
        } else if (!priceMode) {
            throw new RuntimeException("Unknown Profit Mode Selection");
        }

        boolean profitInQuote = checkNotNull(profitInQuoteRadioButton).selectedProperty().get();
        boolean profitInBase = checkNotNull(profitInBaseRadioButton).selectedProperty().get();

        boolean resetControls = true;
        if (holding.compareTo(BigDecimal.ZERO) != 0 && toRepay.compareTo(BigDecimal.ZERO) > 0) {
            if (priceMode) {
                BigDecimal targetPrice = nullToZero(fromString(checkNotNull(targetPriceTextField).textProperty().get()));
                if (targetPrice.compareTo(BigDecimal.ZERO) > 0) {
                    updateAverageTabAtPrice(holding, toRepay, targetPrice, profitInQuote);
                    resetControls = false;
                }
            } else if (checkNotNull(direction) == LONG) {
                // LONG

                if (profitInBase) {
                    // LONG - Take Profit in BASE
                    BigDecimal profitBase = holding.multiply(profitRate);
                    BigDecimal holdingWithoutProfit = holding.subtract(profitBase);

                    if (toRepay.compareTo(BigDecimal.ZERO) != 0) {
                        PriceInfo tradeResult = PriceCalculator.calculateSellPrice(holdingWithoutProfit, toRepay, checkNotNull(fee));
                        BigDecimal adjustedDebt = LongShortCalculator.calculateQuoteAmountToGetShort(holdingWithoutProfit, tradeResult.price, fee).amount;
                        if (adjustedDebt.compareTo(toRepay) < 0) {
                            showMessage(String.format("Rounding error in `calculateSellPrice`, please investigate: %s < %s", formatAmount(adjustedDebt), formatAmount(toRepay)));
                        }
                        toRepay = adjustedDebt;

                        checkNotNull(toSellTextField).textProperty().setValue(formatAmount(holdingWithoutProfit));
                        checkNotNull(sellPriceTextField).textProperty().setValue(formatAmount(tradeResult.price));
                        checkNotNull(proceedsTextField).textProperty().setValue(formatAmount(toRepay));
                        checkNotNull(commissionTextField).textProperty().setValue(formatAmount(tradeResult.commissionQuote));
                        setProfit(profitBase, tradeResult.price);
                        checkNotNull(fxUiEntryTextField).textProperty().setValue(formatAmount(holdingWithoutProfit));

                        resetControls = false;
                    }
                } else if (profitInQuote) {
                    // LONG - Take Profit in QUOTE

                    BigDecimal profitQuote = toRepay.multiply(profitRate);
                    BigDecimal proceedsQuote = toRepay.add(profitQuote);

                    if (proceedsQuote.compareTo(BigDecimal.ZERO) != 0) {
                        PriceInfo tradeResult = PriceCalculator.calculateSellPrice(holding, proceedsQuote, checkNotNull(fee));
                        BigDecimal adjustedProceedsQuote = LongShortCalculator.calculateQuoteAmountToGetShort(holding, tradeResult.price, fee).amount;
                        if (adjustedProceedsQuote.compareTo(proceedsQuote) < 0) {
                            showMessage(String.format("Rounding error in `calculateSellPrice`, please investigate: %s < %s", formatAmount(adjustedProceedsQuote), formatAmount(proceedsQuote)));
                        }
                        proceedsQuote = adjustedProceedsQuote;

                        checkNotNull(toSellTextField).textProperty().setValue(formatAmount(holding));
                        checkNotNull(sellPriceTextField).textProperty().setValue(formatAmount(tradeResult.price));
                        checkNotNull(proceedsTextField).textProperty().setValue(formatAmount(proceedsQuote));
                        checkNotNull(commissionTextField).textProperty().setValue(formatAmount(tradeResult.commissionQuote));
                        setProfit(profitQuote, tradeResult.price);
                        checkNotNull(fxUiEntryTextField).textProperty().setValue(formatAmount(holding));

                        resetControls = false;
                    }
                } else {
                    throw new RuntimeException("Please select to take profit in base or quote");
                }

                // tradeResult = PriceCalculator.calculateSellPrice(holding, proceeds, fee);
            } else {
                // SHORT

                if (profitInQuote) {
                    // SHORT - Take Profit in QUOTE

                    BigDecimal profitQuote = holding.multiply(profitRate);
                    BigDecimal holdingWithoutProfit = holding.subtract(profitQuote);

                    if (toRepay.compareTo(BigDecimal.ZERO) != 0) {
                        BuyPriceInfo tradeResult = PriceCalculator.calculateBuyPriceExact(holdingWithoutProfit, toRepay, checkNotNull(fee));
                        BigDecimal adjustedDebt = LongShortCalculator.calculateBaseAmountToGetLong(holdingWithoutProfit, tradeResult.price, fee).amount;
                        if (adjustedDebt.compareTo(toRepay) < 0) {
                            showMessage(String.format("Rounding error in `calculateBuyPriceExact`, please investigate: %s < %s", formatAmount(adjustedDebt), formatAmount(toRepay)));
                        }
                        toRepay = adjustedDebt;

                        checkNotNull(toSellTextField).textProperty().setValue(formatAmount(holdingWithoutProfit));
                        checkNotNull(sellPriceTextField).textProperty().setValue(formatAmount(tradeResult.price));
                        checkNotNull(proceedsTextField).textProperty().setValue(formatAmount(toRepay));
                        checkNotNull(commissionTextField).textProperty().setValue(formatAmount(tradeResult.commissionQuote));
                        setProfit(profitQuote, tradeResult.price);
                        checkNotNull(fxUiEntryTextField).textProperty().setValue(formatAmount((tradeResult).entryQuote));

                        resetControls = false;
                    }
                } else if (profitInBase) {
                    // SHORT - Take Profit in BASE

                    BigDecimal profitBase = toRepay.multiply(profitRate);
                    BigDecimal proceedsBase = toRepay.add(profitBase);

                    if (proceedsBase.compareTo(BigDecimal.ZERO) != 0) {
                        BuyPriceInfo tradeResult = PriceCalculator.calculateBuyPriceExact(holding, proceedsBase, checkNotNull(fee));
                        BigDecimal adjustedProceedsBase = LongShortCalculator.calculateBaseAmountToGetLong(holding, tradeResult.price, fee).amount;
                        if (adjustedProceedsBase.compareTo(proceedsBase) < 0) {
                            showMessage(String.format("Rounding error in `calculateBuyPriceExact`, please investigate: %s < %s", formatAmount(adjustedProceedsBase), formatAmount(proceedsBase)));
                        }
                        proceedsBase = adjustedProceedsBase;

                        checkNotNull(toSellTextField).textProperty().setValue(formatAmount(holding));
                        checkNotNull(sellPriceTextField).textProperty().setValue(formatAmount(tradeResult.price));
                        checkNotNull(proceedsTextField).textProperty().setValue(formatAmount(proceedsBase));
                        checkNotNull(commissionTextField).textProperty().setValue(formatAmount(tradeResult.commissionQuote));
                        setProfit(profitBase, tradeResult.price);
                        checkNotNull(fxUiEntryTextField).textProperty().setValue(formatAmount((tradeResult).entryQuote));

                        resetControls = false;
                    }
                } else {
                    throw new RuntimeException("Please select to take profit in base or quote");
                }
            }
        }

        if (resetControls) {
            checkNotNull(toSellTextField).textProperty().setValue(formatAmount(BigDecimal.ZERO));
            checkNotNull(sellPriceTextField).textProperty().setValue(formatAmount(BigDecimal.ZERO));
            checkNotNull(proceedsTextField).textProperty().setValue(formatAmount(BigDecimal.ZERO));
            checkNotNull(commissionTextField).textProperty().setValue(formatAmount(BigDecimal.ZERO));
            setProfit(BigDecimal.ZERO, BigDecimal.ZERO);
            checkNotNull(fxUiEntryTextField).textProperty().setValue(formatAmount(BigDecimal.ZERO));
        }

        updateRetainedAmountByQuote();
    }

    /** Averaging trade at a fixed price. Profit currency decides whether we trade only what's needed to cover the debt
     *  (profit = leftover holdings) or trade all holdings (profit = proceeds above debt). Profit may be negative. */
    protected void updateAverageTabAtPrice(BigDecimal holding, BigDecimal toRepay, BigDecimal price, boolean profitInQuote) {
        BigDecimal toTrade;
        BigDecimal proceeds;
        BigDecimal commissionQuote;
        BigDecimal profit;
        BigDecimal cexUiEntry;
        if (checkNotNull(direction) == LONG) {
            if (profitInQuote) {
                toTrade = holding;
                AmountAndCommission sell = LongShortCalculator.calculateQuoteAmountToGetShort(toTrade, price, fee);
                proceeds = sell.amount;
                commissionQuote = sell.commissionQuote;
                profit = proceeds.subtract(toRepay);
            } else {
                AmountAndCommission sell = LongShortCalculator.calculateBaseAmountToGiveShort(toRepay, price, fee);
                toTrade = sell.amount;
                proceeds = LongShortCalculator.calculateQuoteAmountToGetShort(toTrade, price, fee).amount;
                commissionQuote = sell.commissionQuote;
                profit = holding.subtract(toTrade);
            }
            cexUiEntry = toTrade;
        } else {
            if (profitInQuote) {
                AmountAndCommission buy = LongShortCalculator.calculateQuoteAmountToGiveLong(toRepay, price, fee);
                toTrade = buy.amount;
                proceeds = LongShortCalculator.calculateBaseAmountToGetLong(toTrade, price, fee).amount;
                commissionQuote = buy.commissionQuote;
                profit = holding.subtract(toTrade);
            } else {
                toTrade = holding;
                AmountAndCommission buy = LongShortCalculator.calculateBaseAmountToGetLong(toTrade, price, fee);
                proceeds = buy.amount;
                commissionQuote = buy.commissionQuote;
                profit = proceeds.subtract(toRepay);
            }
            cexUiEntry = toTrade.subtract(commissionQuote);
        }

        checkNotNull(toSellTextField).textProperty().setValue(formatAmount(toTrade));
        checkNotNull(sellPriceTextField).textProperty().setValue(formatAmount(price));
        checkNotNull(proceedsTextField).textProperty().setValue(formatAmount(proceeds));
        checkNotNull(commissionTextField).textProperty().setValue(formatAmount(commissionQuote));
        setProfit(profit, price);
        checkNotNull(fxUiEntryTextField).textProperty().setValue(formatAmount(cexUiEntry));
    }

    /** Sets profit and its quote-currency reference value; BASE profit is converted at the trade price without commission. */
    protected void setProfit(BigDecimal profit, BigDecimal tradePrice) {
        BigDecimal profitQuote = checkNotNull(profitInBaseRadioButton).isSelected() ? profit.multiply(tradePrice) : profit;
        checkNotNull(profitTextField).textProperty().setValue(formatAmount(profit));
        checkNotNull(profitQuoteTextField).textProperty().setValue(formatAmount(profitQuote));
    }

    public void updateLabels() {
        String debtCurrency = checkNotNull(direction) == LONG
                ? ticker.getQuote().getCurrency() : ticker.getBase().getCurrency();
        String holdCurrency = direction == LONG
                ? ticker.getBase().getCurrency() : ticker.getQuote().getCurrency();

        checkNotNull(poplavokDirectionLabel).textProperty().setValue(checkNotNull(direction).name());
        checkNotNull(poplavokTickerLabel).textProperty().setValue(checkNotNull(ticker).getSymbol());
        checkNotNull(averagingActionLabel).textProperty().setValue(checkNotNull(direction) == LONG ? "SELL" : "BUY");
        checkNotNull(averagingCurrencyLabel).textProperty().setValue(ticker.getBase().getCurrency());

        checkNotNull(debtCurrencyLabel).textProperty().setValue(debtCurrency);
        checkNotNull(availableCurrencyLabel).textProperty().setValue(debtCurrency);
        checkNotNull(toRepayCurrencyLabel).textProperty().setValue(debtCurrency);
        checkNotNull(fxUiEntryCurrencyLabel).setText(holdCurrency);
        checkNotNull(holdingCurrencyLabel).textProperty().setValue(holdCurrency);

        checkNotNull(toTradeCurrencyLabel).textProperty().setValue(holdCurrency);
        checkNotNull(tradePriceLabel).textProperty().setValue(ticker.getSymbol());
        checkNotNull(proceedsCurrencyLabel).textProperty().setValue(debtCurrency);

        checkNotNull(retainCurrencyLabel).textProperty().setValue(holdCurrency);
        checkNotNull(retainWorhCurrencyLabel).textProperty().setValue(debtCurrency);

        String quoteCurrency = checkNotNull(ticker).getQuote().getCurrency();
        String baseCurrency = checkNotNull(ticker).getBase().getCurrency();

        if (direction == LONG) {
            checkNotNull(retainCommisionCurrencyLabel).textProperty().setValue(quoteCurrency);
        } else if (direction == SHORT) {
            checkNotNull(retainCommisionCurrencyLabel).textProperty().setValue(baseCurrency);
        } else {
            throw new RuntimeException("Unknown direction " + direction);
        }

        checkNotNull(commissionCurrencyLabel).textProperty().setValue(quoteCurrency);
        checkNotNull(profitQuoteCurrencyLabel).textProperty().setValue(quoteCurrency);
        checkNotNull(profitCurrencyLabel).textProperty().setValue(
                checkNotNull(profitInBaseRadioButton).isSelected() ? baseCurrency : quoteCurrency);
    }

    public void setProfitInQuote(boolean profitInQuote) {
        if (profitInQuote) {
            checkNotNull(profitInQuoteRadioButton).setSelected(true);
        } else {
            checkNotNull(profitInBaseRadioButton).setSelected(true);
        }
    }

    /** Sets the "+ Comms" count; the slider listener mirrors it into the text field and recalculates. */
    public void setCommsCount(int commsCount) {
        checkNotNull(commsCountSlider).setValue(commsCount);
        checkNotNull(commsCountTextField).setText(String.valueOf(commsCount));
        updateAverageTabEvent();
    }

    public void setProfitPercent(String profitPercent) {
        checkNotNull(percentTextField).setText(profitPercent);
        updateAverageTabEvent();
    }

    public BigDecimal getTotalDebt() {
        return nullToZero(fromString(checkNotNull(debtTextField).textProperty().get()));
    }

    public BigDecimal getAmountToTrade() {
        return nullToZero(fromString(checkNotNull(toSellTextField).textProperty().get()));
    }

    public BigDecimal getDebtToRepay() {
        return nullToZero(fromString(checkNotNull(toRepayTextField).textProperty().get()));
    }

    public BigDecimal getProfit() {
        return nullToZero(fromString(checkNotNull(profitTextField).textProperty().get()));
    }

    public String getProfitCurrency() {
        return checkNotNull(profitCurrencyLabel).textProperty().get();
    }

    public BigDecimal getAveragingPrice() {
        return nullToZero(fromString(checkNotNull(sellPriceTextField).textProperty().get()));
    }

    public BigDecimal getRetainedDebt() {
        if (checkNotNull(retainHoldingsCheckBox).isSelected()) {
            return nullToZero(fromString(checkNotNull(retainWorthTextField).textProperty().get()));
        } else {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal getRetainedAmount() {
        if (checkNotNull(retainHoldingsCheckBox).isSelected()) {
            return nullToZero(fromString(checkNotNull(retainTextField).textProperty().get()));
        } else {
            return BigDecimal.ZERO;
        }
    }
}
