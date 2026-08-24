package com.poplavok.forms;

import com.flower.fxutils.ModalWindow;
import com.flower.fxutils.Refreshable;
import com.poplavok.data.dao.AccountDAO;
import com.poplavok.data.dao.LevelTradeDAO;
import com.poplavok.data.dao.LoanTransferDAO;
import com.poplavok.data.dao.RateDAO;
import com.poplavok.data.dao.RepaymentDAO;
import com.poplavok.data.dao.TradeDAO;
import com.poplavok.data.dao.TransactionDAO;
import com.poplavok.data.model.Account;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.Direction;
import com.poplavok.data.model.Level;
import com.poplavok.data.model.LevelState;
import com.poplavok.data.model.LevelTrade;
import com.poplavok.data.model.Loan;
import com.poplavok.data.model.LoanTransfer;
import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.model.Poplavok;
import com.poplavok.data.model.Rate;
import com.poplavok.data.model.Repayment;
import com.poplavok.data.model.RepaymentType;
import com.poplavok.data.model.Trade;
import com.poplavok.data.model.Transaction;
import com.poplavok.data.dao.LevelDAO;
import com.poplavok.data.dao.PoplavokDAO;
import com.poplavok.data.dao.LoanDAO;
import com.poplavok.data.utils.BigDecimalUtil;
import com.poplavok.data.utils.DBUtil;
import com.poplavok.data.utils.LoanTransferManager;
import com.poplavok.data.utils.RepaymentManager;
import com.poplavok.data.utils.distributors.WithdrawalDistributor;
import com.poplavok.forms.wrapper.LevelTransaction;
import com.poplavok.forms.wrapper.PoplavokAndInverseLevel;
import com.poplavok.forms.wrapper.TradeWrapper;
import com.poplavok.forms.wrapper.repayment.LossRepaymentInfo;
import com.poplavok.forms.wrapper.repayment.ProfitRepaymentInfo;
import com.poplavok.forms.wrapper.repayment.RepayRepaymentInfo;
import com.poplavok.forms.wrapper.repayment.RepaymentInfo;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tab;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import javafx.collections.ListChangeListener;
import javafx.scene.control.SelectionMode;

import static com.flower.fxutils.JavaFxUtils.autoResizeTableColumns;
import static com.flower.fxutils.JavaFxUtils.showErrorMessage;
import static com.flower.fxutils.JavaFxUtils.showMessage;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.poplavok.data.model.Direction.LONG;
import static com.poplavok.data.model.Direction.SHORT;
import static com.poplavok.data.model.LoanType.ACCOUNT_FUNDED;
import static com.poplavok.data.model.LoanType.EXTERNAL_CROSS_MARGIN;
import static com.poplavok.data.model.LoanType.EXTERNAL_ISOLATED_MARGIN;
import static com.poplavok.data.model.LoanType.POPLAVOK_FUNDED;
import static com.poplavok.data.utils.BigDecimalUtil.SCALE;
import static com.poplavok.data.utils.BigDecimalUtil.formatAmount;
import static com.poplavok.data.utils.BigDecimalUtil.nullToZero;
import static com.poplavok.data.utils.BigDecimalUtil.fromString;
import static com.poplavok.forms.AllocateFundsDialog.MOVE_TO_ZERO_FUNDING_STATUS;

public class PoplavokTab extends AnchorPane implements Refreshable {
    final static Logger LOGGER = LoggerFactory.getLogger(PoplavokTab.class);

    @Nullable Poplavok poplavok;
    @Nullable FilteredList<Level> levels;
    @Nullable FilteredList<LevelTransaction> transactions;
    @Nullable FilteredList<TradeWrapper> trades;

    @FXML @Nullable TableView<Level> levelsTable;
    @FXML @Nullable TableView<LevelTransaction> transactionsTable;
    @FXML @Nullable TableView<TradeWrapper> tradesTable;

    @FXML @Nullable TextField tickerTextField;
    @FXML @Nullable TextField feeTextField;
    @FXML @Nullable TextField priceTextField;

    @FXML @Nullable TextField directionTextBox;

    @FXML @Nullable TextField holdingBaseTextField;
    @FXML @Nullable TextField holdingQuoteTextField;
    @FXML @Nullable TextField oweBaseTextField;
    @FXML @Nullable TextField oweQuoteTextField;
    @FXML @Nullable TextField lentBaseTextField;
    @FXML @Nullable TextField lentQuoteTextField;
    @FXML @Nullable TextField marketValueBaseTextField;
    @FXML @Nullable TextField marketValueQuoteTextField;
    @FXML @Nullable TextField pnlTextField;
    @FXML @Nullable TextField averagingPriceTextField;

    @FXML @Nullable CheckBox debtCurrencyCheckBox;
    @FXML @Nullable CheckBox holdingCurrencyCheckBox;

    @FXML @Nullable Button closeLevelButton;
    @FXML @Nullable CheckBox showClosedLevelsCheckBox;

    @FXML @Nullable Tab averagingTab;
    @Nullable AveragingPane averagingPane;

    protected final MainForm mainApp;
    protected final Long poplavokId;

    public PoplavokTab(MainForm mainApp, Long poplavokId) {
        this.mainApp = mainApp;
        this.poplavokId = poplavokId;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PoplavokTab.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(levelsTable).getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        checkNotNull(levelsTable).getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends Level> c) -> {
            updateLevelsSelection();
        });

        checkNotNull(showClosedLevelsCheckBox).selectedProperty().addListener((observable, oldValue, newValue) -> {
            refreshContent();
        });

        DBUtil.connectCommitAndClose(sess -> {
            this.poplavok = PoplavokDAO.findById(sess, poplavokId)
                    .orElseThrow(() -> new RuntimeException("Poplavok not found"));
            this.poplavok.getTicker().getSymbol();
        });

        refreshContent();

        averagingPane = new AveragingPane(checkNotNull(poplavok).getTicker(),
                checkNotNull(checkNotNull(poplavok).getDirection()),
                checkNotNull(levelsTable).getSelectionModel().getSelectedItems(),
                nullToZero(fromString(checkNotNull(feeTextField).textProperty().get())));
        checkNotNull(averagingTab).setContent(averagingPane);

        averagingPane.updateLabels();
    }

    protected boolean isEmpty(@Nullable BigDecimal amount) {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }

    protected boolean isLevelEmpty(Level lvl) {
        return isEmpty(lvl.getAvailableAmountBase()) &&
                isEmpty(lvl.getAvailableAmountQuote()) &&
                isEmpty(lvl.getDebtBase()) &&
                isEmpty(lvl.getDebtQuote()) &&
                isEmpty(lvl.getLentAmountBase()) &&
                isEmpty(lvl.getLentAmountQuote());
    }

    protected void updateLevelsSelection() {
        List<Level> selected = checkNotNull(levelsTable).getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            if (closeLevelButton != null) {
                closeLevelButton.setDisable(true);
                closeLevelButton.setText("[ Close level ]");
            }
            if (holdingBaseTextField != null) holdingBaseTextField.setText("");
            if (holdingQuoteTextField != null) holdingQuoteTextField.setText("");
            if (oweBaseTextField != null) oweBaseTextField.setText("");
            if (oweQuoteTextField != null) oweQuoteTextField.setText("");
            if (lentBaseTextField != null) lentBaseTextField.setText("");
            if (lentQuoteTextField != null) lentQuoteTextField.setText("");
            if (marketValueBaseTextField != null) marketValueBaseTextField.setText("");
            if (marketValueQuoteTextField != null) marketValueQuoteTextField.setText("");
            if (pnlTextField != null) pnlTextField.setText("");
            if (averagingPriceTextField != null) averagingPriceTextField.setText("");
            if (transactionsTable != null) transactionsTable.setItems(FXCollections.emptyObservableList());
            if (tradesTable != null) tradesTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        checkNotNull(averagingPane).updateAverageTab(selected);

        if (closeLevelButton != null) {
            if (selected.size() != 1) {
                closeLevelButton.setDisable(true);
                closeLevelButton.setText("[ Close level ]");
            } else {
                Level lvl = selected.get(0);
                LevelState state = lvl.getState();
                if (state == LevelState.INCEPTION) {
                    closeLevelButton.setDisable(false);
                    closeLevelButton.setText("[ Delete level ]");
                } else if (state == LevelState.FUNDING) {
                    closeLevelButton.setDisable(false);
                    closeLevelButton.setText("[ Close level ]");
                } else if (state == LevelState.TRADING) {
                    closeLevelButton.setDisable(false);
                    closeLevelButton.setText("[ Close level ]");
                } else if (state == LevelState.CLOSED) {
                    closeLevelButton.setDisable(true);
                    closeLevelButton.setText("[ Close level ]");
                }
            }
        }

        BigDecimal holdingBase = BigDecimal.ZERO;
        BigDecimal holdingQuote = BigDecimal.ZERO;
        BigDecimal oweBase = BigDecimal.ZERO;
        BigDecimal oweQuote = BigDecimal.ZERO;
        BigDecimal lentBase = BigDecimal.ZERO;
        BigDecimal lentQuote = BigDecimal.ZERO;

        for (Level lvl : selected) {
            holdingBase = holdingBase.add(lvl.getAvailableAmountBase() != null ? lvl.getAvailableAmountBase() : BigDecimal.ZERO);
            holdingQuote = holdingQuote.add(lvl.getAvailableAmountQuote() != null ? lvl.getAvailableAmountQuote() : BigDecimal.ZERO);
            oweBase = oweBase.add(lvl.getDebtBase() != null ? lvl.getDebtBase() : BigDecimal.ZERO);
            oweQuote = oweQuote.add(lvl.getDebtQuote() != null ? lvl.getDebtQuote() : BigDecimal.ZERO);
            lentBase = lentBase.add(lvl.getLentAmountBase() != null ? lvl.getLentAmountBase() : BigDecimal.ZERO);
            lentQuote = lentQuote.add(lvl.getLentAmountQuote() != null ? lvl.getLentAmountQuote() : BigDecimal.ZERO);
        }

        if (holdingBaseTextField != null) holdingBaseTextField.setText(formatAmount(holdingBase));
        if (holdingQuoteTextField != null) holdingQuoteTextField.setText(formatAmount(holdingQuote));
        if (oweBaseTextField != null) oweBaseTextField.setText(formatAmount(oweBase));
        if (oweQuoteTextField != null) oweQuoteTextField.setText(formatAmount(oweQuote));
        if (lentBaseTextField != null) lentBaseTextField.setText(formatAmount(lentBase));
        if (lentQuoteTextField != null) lentQuoteTextField.setText(formatAmount(lentQuote));

        BigDecimal totalBase = holdingBase.add(lentBase).subtract(oweBase);
        BigDecimal totalQuote = holdingQuote.add(lentQuote).subtract(oweQuote);

        if (marketValueBaseTextField != null) marketValueBaseTextField.setText(formatAmount(totalBase));
        if (marketValueQuoteTextField != null) marketValueQuoteTextField.setText(formatAmount(totalQuote));

        BigDecimal currentPrice = BigDecimal.ZERO;
        if (priceTextField != null && priceTextField.getText() != null && !priceTextField.getText().trim().isEmpty()) {
            try {
                currentPrice = nullToZero(fromString(priceTextField.getText().trim()));
            } catch (Exception e) {}
        }
        
        if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pnl = totalQuote.add(totalBase.multiply(currentPrice));
            if (pnlTextField != null) pnlTextField.setText(formatAmount(pnl));
        } else {
            if (pnlTextField != null) pnlTextField.setText("");
        }
        
        if (totalBase.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal avgPrice = totalQuote.negate().divide(totalBase, 8, java.math.RoundingMode.HALF_UP);
            if (averagingPriceTextField != null) averagingPriceTextField.setText(formatAmount(avgPrice));
        } else {
            if (averagingPriceTextField != null) averagingPriceTextField.setText("");
        }

        if (selected.size() == 1) {
            Level selectedLevel = selected.get(0);

            // Transactions loading
            if (transactionsTable != null) {
                try {
                    List<Transaction> levelTransactions = DBUtil.connectGetResultAndClose(sess ->
                            TransactionDAO.findByLevel(sess, selectedLevel.getId())
                    );
                    this.transactions = new FilteredList<>(FXCollections.observableArrayList(
                            levelTransactions.stream().map(t -> new LevelTransaction(t, selectedLevel)).toList()));
                    transactionsTable.setItems(this.transactions);
                    autoResizeTableColumns(transactionsTable);
                } catch (Exception e) {
                    LOGGER.error("Error loading transactions for level", e);
                }
            }

            // Trades loading
            if (tradesTable != null) {
                try {
                    List<TradeWrapper> levelTrades = DBUtil.connectGetResultAndClose(sess ->
                            TradeDAO.findByLevel(sess, selectedLevel.getId())
                    ).stream().map(TradeWrapper::new).toList();
                    this.trades = new FilteredList<>(FXCollections.observableArrayList(levelTrades));
                    tradesTable.setItems(this.trades);
                    autoResizeTableColumns(tradesTable);
                } catch (Exception e) {
                    LOGGER.error("Error loading trades for level", e);
                }
            }
        } else {
            if (transactionsTable != null) {
                transactionsTable.setItems(FXCollections.emptyObservableList());
            }
            if (tradesTable != null) {
                tradesTable.setItems(FXCollections.emptyObservableList());
            }
        }
    }

    @Override
    public void refreshContent() {
        List<Long> selectedLevelIds = new ArrayList<>();
        if (levelsTable != null && levelsTable.getSelectionModel() != null) {
            List<Level> selectedItems = levelsTable.getSelectionModel().getSelectedItems();
            if (selectedItems != null) {
                for (Level l : selectedItems) {
                    if (l != null && l.getId() != null) {
                        selectedLevelIds.add(l.getId());
                    }
                }
            }
        }

        try {
            boolean showClosed = checkNotNull(showClosedLevelsCheckBox).isSelected();
            List<Level> levelList = DBUtil.connectGetResultAndClose(sess -> LevelDAO.findByPoplavokId(sess, poplavokId, showClosed));
            this.levels = new FilteredList<>(FXCollections.observableArrayList(levelList));
            checkNotNull(levelsTable).setItems(this.levels);
            autoResizeTableColumns(levelsTable);

            if (!selectedLevelIds.isEmpty()) {
                levelsTable.getSelectionModel().clearSelection();
                for (Level l : this.levels) {
                    if (l != null && l.getId() != null && selectedLevelIds.contains(l.getId())) {
                        levelsTable.getSelectionModel().select(l);
                    }
                }
            }
            MarketTicker ticker = checkNotNull(poplavok).getTicker();
            if (ticker != null) {
                checkNotNull(tickerTextField).setText(poplavok.getTicker().getSymbol());
                checkNotNull(feeTextField).setText(formatAmount(poplavok.getTicker().getMakerFee()));
                checkNotNull(directionTextBox).setText(checkNotNull(poplavok.getDirection()).name());
                Rate rate = RateDAO.getLatestRateForTicker(ticker.getId());
                if (rate != null && rate.getPrice() != null) {
                    checkNotNull(priceTextField).setText(formatAmount(rate.getPrice()));
                }
            }
            updateLevelsSelection(); // Update stats in case something changed
        } catch (Exception e) {
            LOGGER.error("Error refreshing content: ", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error loading Poplavok: " + e, ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void newLevel() {
        try {
            BigDecimal price = null;
            try {
                price = nullToZero(fromString(checkNotNull(priceTextField).textProperty().get()));
            } catch (Exception e) {}
            BigDecimal fee = null;
            try {
                fee = nullToZero(fromString(checkNotNull(feeTextField).textProperty().get()));
            } catch (Exception e) {}

            LevelAddDialog levelAddDialog = new LevelAddDialog(null, checkNotNull(poplavok).getTicker().getSymbol(), price, fee, checkNotNull(poplavok.getDirection()));
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.mainStage),
                    stage -> { levelAddDialog.setStage(stage); return levelAddDialog; },
                    "New Level");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            Level level = levelAddDialog.getReturnLevel();
                            if (level != null) {
                                level.setPoplavok(checkNotNull(poplavok));
                                DBUtil.connectCommitAndClose(sess -> LevelDAO.save(sess, checkNotNull(level)));
                                refreshContent();
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error creating level: " + e, ButtonType.OK);
                            LOGGER.error("Error creating level: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error creating level: " + e, ButtonType.OK);
            LOGGER.error("Error creating level: ", e);
            alert.showAndWait();
        }
    }

    public void editLevel() {
        try {
            List<Level> selected = checkNotNull(levelsTable).getSelectionModel().getSelectedItems();
            if (selected == null || selected.size() != 1) return;

            Level lvl = selected.get(0);
            LevelState state = lvl.getState();

            if (state == LevelState.CLOSED) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "CLOSED level can't be edited.");
                alert.showAndWait();
                return;
            }

            BigDecimal fee = null;
            try {
                fee = nullToZero(fromString(checkNotNull(feeTextField).textProperty().get()));
            } catch (Exception e) {}

            LevelAddDialog levelAddDialog = new LevelAddDialog(lvl, checkNotNull(poplavok).getTicker().getSymbol(),
                    lvl.getProjectedPrice(), fee, checkNotNull(poplavok.getDirection()));
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.mainStage),
                stage -> { levelAddDialog.setStage(stage); return levelAddDialog; },
                "Edit Level " + StringUtils.defaultIfBlank(lvl.getNotes(), "#" + lvl.getId()));

            workspaceStage.setOnHidden(
                ev -> {
                    try {
                        Level level = levelAddDialog.getReturnLevel();
                        if (level != null) {
                            level.setPoplavok(checkNotNull(poplavok));
                            DBUtil.connectCommitAndClose(sess -> LevelDAO.update(sess, checkNotNull(level)));
                            refreshContent();
                        }
                    } catch (Exception e) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Error creating level: " + e, ButtonType.OK);
                        LOGGER.error("Error creating level: ", e);
                        alert.showAndWait();
                    }
                }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error updating level: " + e, ButtonType.OK);
            LOGGER.error("Error updating level: ", e);
            alert.showAndWait();
        }
    }

    public void repay() {
        try {
            List<Level> selected = checkNotNull(levelsTable).getSelectionModel().getSelectedItems();
            if (selected == null || selected.size() != 1) return;

            Level lvl = selected.get(0);
            LevelState state = lvl.getState();

            if (state == LevelState.CLOSED) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Level CLOSED.");
                alert.showAndWait();
                return;
            }

            BigDecimal fee = null;
            try {
                fee = nullToZero(fromString(checkNotNull(feeTextField).textProperty().get()));
            } catch (Exception e) {}

            RepaySettleDebtDialog repaySettleDebtDialog = new RepaySettleDebtDialog(lvl, checkNotNull(poplavok).getTicker(),
                    lvl.getProjectedPrice(), fee, checkNotNull(poplavok.getDirection()));
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.mainStage),
                    stage -> { repaySettleDebtDialog.setStage(stage); return repaySettleDebtDialog; },
                    "Repay Level " + StringUtils.defaultIfBlank(lvl.getNotes(), "#" + lvl.getId()));

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            RepaymentInfo repayment = repaySettleDebtDialog.getReturnRepayment();
                            if (repayment != null) {
                                switch (repayment.getRepaymentType()) {
                                    case REPAY: processRepay(lvl, repayment); break;
                                    case PROFIT: processTakeProfit(lvl, repayment); break;
                                    case LOSS: processTakeLoss(lvl, repayment); break;
                                    default: throw new RuntimeException("Unknown repayment type: " + repayment.getRepaymentType());
                                }
                            }
                            refreshContent();
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error creating level: " + e, ButtonType.OK);
                            LOGGER.error("Error creating level: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error updating level: " + e, ButtonType.OK);
            LOGGER.error("Error updating level: ", e);
            alert.showAndWait();
        }
    }

    public void processTakeProfit(Level sourceLevel, RepaymentInfo _repayment) {
        if (_repayment.getRepaymentType() != RepaymentType.PROFIT) {
            throw new RuntimeException("Trying to process non-REPAY type repayment (" +
                    _repayment.getRepaymentType() + ") with repay process ");
        }

        ProfitRepaymentInfo repayment = (ProfitRepaymentInfo)_repayment;
        if (repayment.getAccountToMoveProfitTo() != null) {
            RepaymentManager.takeProfitToAccount(sourceLevel, checkNotNull(poplavok).getTicker(), repayment.getAccountToMoveProfitTo(), repayment.getAmount(), repayment.getCurrency(), new Date());
        } else if (repayment.getLevelToMoveProfitTo() != null) {
            RepaymentManager.takeProfitToLevelAndSave(sourceLevel, checkNotNull(poplavok).getTicker(), repayment.getLevelToMoveProfitTo(), repayment.getAmount(), repayment.getCurrency(), new Date());
        } else {
            throw new RuntimeException("Profit taken should be moved to either Account or Level");
        }
    }

    public void processTakeLoss(Level sourceLevel, RepaymentInfo _repayment) {
        if (_repayment.getRepaymentType() != RepaymentType.LOSS) {
            throw new RuntimeException("Trying to process non-REPAY type repayment (" +
                    _repayment.getRepaymentType() + ") with repay process ");
        }

        LossRepaymentInfo repayment = (LossRepaymentInfo)_repayment;
        RepaymentManager.takeLoss(repayment.loanToWriteOff(), checkNotNull(poplavok).getTicker(), sourceLevel, repayment.getAmount(), new Date());
    }

    public void processRepay(Level sourceLevel, RepaymentInfo _repayment) {
        if (_repayment.getRepaymentType() != RepaymentType.REPAY) {
            throw new RuntimeException("Trying to process non-REPAY type repayment (" +
                    _repayment.getRepaymentType() + ") with repay process ");
        }

        RepayRepaymentInfo repayment = (RepayRepaymentInfo)_repayment;
        RepaymentManager.repay(repayment.getLoanToRepay(), sourceLevel, repayment.getAmount(), new Date(), "Repayment from Poplavok level");
    }

    public void closeLevel() {
        try {
            List<Level> selected = checkNotNull(levelsTable).getSelectionModel().getSelectedItems();
            if (selected == null || selected.size() != 1) return;

            Level lvl = selected.get(0);
            LevelState state = lvl.getState();

            if (nullToZero(lvl.getAvailableAmountBase()).compareTo(BigDecimal.ZERO) != 0 ||
                nullToZero(lvl.getAvailableAmountQuote()).compareTo(BigDecimal.ZERO) != 0 ||
                nullToZero(lvl.getLentAmountBase()).compareTo(BigDecimal.ZERO) != 0 ||
                nullToZero(lvl.getLentAmountQuote()).compareTo(BigDecimal.ZERO) != 0 ||
                nullToZero(lvl.getDebtBase()).compareTo(BigDecimal.ZERO) != 0 ||
                nullToZero(lvl.getDebtQuote()).compareTo(BigDecimal.ZERO) != 0) {
                showErrorMessage("Can't close / delete level with non-zero holding, lent amount or debt.");
                return;
            }

            if (state == LevelState.INCEPTION) {
                DBUtil.connectCommitAndClose(sess -> LevelDAO.delete(sess, lvl));
            } else if ((state == LevelState.TRADING || state == LevelState.FUNDING) && isLevelEmpty(lvl)) {
                lvl.setState(LevelState.CLOSED);
                DBUtil.connectCommitAndClose(sess -> LevelDAO.update(sess, lvl));
            } else if (!isLevelEmpty(lvl)) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Level is not empty. Can't close / delete.");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Can't close / delete level.");
                alert.showAndWait();
            }
            refreshContent();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error closing level: " + e, ButtonType.OK);
            LOGGER.error("Error closing level: ", e);
            alert.showAndWait();
        }
    }

    public void refreshPrice() {}

    public void closePoplavok() {
        // Close poplavok IFF - all its levels are closed
        try {
            DBUtil.connectCommitAndClose(sess -> {
                List<Level> openLevels = LevelDAO.findByPoplavokId(sess, poplavokId, false);
                if (!openLevels.isEmpty()) {
                    throw new RuntimeException("Cannot close poplavok. All levels must be closed.");
                }

                checkNotNull(poplavok).setActive(false);
                poplavok.setCloseDate(new Date());
                poplavok = PoplavokDAO.update(sess, poplavok);
            });

            refreshContent();

            showMessage("Poplavok closed.");
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error closing poplavok: " + e.getMessage(), ButtonType.OK);
            LOGGER.error("Error closing poplavok: ", e);
            alert.showAndWait();
        }
    }

    static class LevelHoldingsAndDebts {
        public final List<BigDecimal> levelDebts;
        public final List<BigDecimal> levelHoldings;

        public LevelHoldingsAndDebts(List<BigDecimal> levelDebts, List<BigDecimal> levelHoldings) {
            this.levelDebts = levelDebts;
            this.levelHoldings = levelHoldings;
        }
    }

    public LevelHoldingsAndDebts getLevelHoldingsAndDebts(List<Level> levels, Direction direction, boolean removeAvailableFromDebts) {
        List<BigDecimal> levelDebts = new ArrayList<>();
        List<BigDecimal> levelHoldings = new ArrayList<>();

        for (Level lvl : levels) {
            BigDecimal levelDebt;
            BigDecimal levelHoldingsAmount;
            if (direction == LONG) {
                levelDebt = nullToZero(lvl.getDebtQuote());
                if (removeAvailableFromDebts) {
                    levelDebt = levelDebt.subtract(nullToZero(lvl.getAvailableAmountQuote()));
                }

                levelHoldingsAmount = nullToZero(lvl.getAvailableAmountBase());
            } else { //if (direction == SHORT) {
                levelDebt = nullToZero(lvl.getDebtBase());
                if (removeAvailableFromDebts) {
                    levelDebt = levelDebt.subtract(nullToZero(lvl.getAvailableAmountBase()));
                }

                levelHoldingsAmount = nullToZero(lvl.getAvailableAmountQuote());
            }

            levelDebts.add(levelDebt);
            levelHoldings.add(levelHoldingsAmount);
        }

        return new LevelHoldingsAndDebts(levelDebts, levelHoldings);
    }

    public void averagingTrade() {
        try {
            MarketTicker ticker = checkNotNull(poplavok).getTicker();

            final List<Level> levels = checkNotNull(levelsTable).getSelectionModel().getSelectedItems();
            if (levels == null || levels.isEmpty()) {
                showErrorMessage("Please select 1 or more levels to average-trade.");
                return;
            }

            Direction direction = checkNotNull(checkNotNull(poplavok).getDirection());
            for (Level lvl : levels) {
                LevelState state = lvl.getState();
                if (state != LevelState.TRADING && state != LevelState.FUNDING) {
                    showErrorMessage("Levels in " + state + " state can't perform trades.");
                    return;
                }
            }

            BigDecimal fullAmountToTrade = checkNotNull(averagingPane).getAmountToTrade();
            BigDecimal fullDebtToRepay = checkNotNull(averagingPane).getDebtToRepay();
            BigDecimal averagingPrice = checkNotNull(averagingPane).getAveragingPrice();

            BigDecimal retainedDebt = checkNotNull(averagingPane).getRetainedDebt();
            BigDecimal retainedAmount = checkNotNull(averagingPane).getRetainedAmount();

            BigDecimal debtToRepay = fullDebtToRepay.subtract(retainedDebt);
            BigDecimal amountToTrade = fullAmountToTrade.subtract(retainedAmount);

            BigDecimal availableAmountBase;
            BigDecimal availableAmountQuote;
            if (direction == Direction.LONG) {
                availableAmountBase = amountToTrade;
                availableAmountQuote = BigDecimal.ZERO;
            } else if (direction == SHORT) {
                availableAmountQuote = amountToTrade;
                availableAmountBase = BigDecimal.ZERO;
            } else {
                throw new RuntimeException("Unknown Direction " + direction);
            }

            PerformTradeDialog performTradeDialog = new PerformTradeDialog(mainApp, availableAmountBase, availableAmountQuote, debtToRepay,
                    checkNotNull(poplavok).getTicker(), direction, averagingPrice);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.mainStage),
                    stage -> { performTradeDialog.setStage(stage); return performTradeDialog; },
                    "Perform Averaging Trade");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            Trade trade = performTradeDialog.getReturnTrade();
                            if (trade != null) {
                                BigDecimal baseIn = nullToZero(trade.getAmountBaseIn());
                                BigDecimal quoteIn = nullToZero(trade.getAmountQuoteIn());
                                BigDecimal baseOut = nullToZero(trade.getAmountBaseOut());
                                BigDecimal quoteOut = nullToZero(trade.getAmountQuoteOut());
                                BigDecimal baseCommission = nullToZero(trade.getCommissionBase());
                                BigDecimal quoteCommission = nullToZero(trade.getCommissionQuote());

                                BigDecimal amountIn;
                                BigDecimal amountOut;
                                if (direction == LONG) {
                                    amountIn = baseIn;
                                    amountOut = quoteOut;
                                } else { //if (direction == SHORT) {
                                    amountIn = quoteIn;
                                    amountOut = baseOut;
                                }
                                BigDecimal amountInForCheck = amountIn;
                                amountIn = amountIn.setScale(SCALE, RoundingMode.CEILING);
                                amountOut = amountOut.setScale(SCALE, RoundingMode.FLOOR);

                                // 1. Determine holdings and debts for each level
                                LevelHoldingsAndDebts levelHoldingsAndDebts = getLevelHoldingsAndDebts(levels, direction, true);

                                // 2. Create LevelTrades
                                List<LevelTrade> levelTrades = new ArrayList<>();

                                List<BigDecimal> levelAmountsIn = WithdrawalDistributor.distributeWithdrawal(levelHoldingsAndDebts.levelHoldings, amountIn, SCALE);
                                List<BigDecimal> levelAmountsOut = WithdrawalDistributor.distributeWithdrawal(levelHoldingsAndDebts.levelDebts, amountOut, SCALE, true);

                                BigDecimal amountInCheck = BigDecimal.ZERO;
                                BigDecimal amountOutCheck = BigDecimal.ZERO;
                                for (int i = 0; i < levels.size(); i++) {
                                    Level lvl = levels.get(i);

                                    BigDecimal levelAmountIn = levelAmountsIn.get(i);
                                    BigDecimal levelAmountOut = levelAmountsOut.get(i);

                                    amountInCheck = amountInCheck.add(levelAmountIn);
                                    amountOutCheck = amountOutCheck.add(levelAmountOut);

                                    LevelTrade levelTrade = new LevelTrade();
                                    levelTrade.setLevel(lvl);
                                    levelTrade.setTrade(trade);

                                    levelTrades.add(levelTrade);

                                    if (direction == LONG) {
                                        levelTrade.setAmountBaseIn(levelAmountIn);
                                        levelTrade.setAmountQuoteOut(levelAmountOut);

                                        lvl.setAvailableAmountBase(nullToZero(lvl.getAvailableAmountBase()).subtract(levelAmountIn));
                                        lvl.setAvailableAmountQuote(nullToZero(lvl.getAvailableAmountQuote()).add(levelAmountOut));
                                    } else { //if (direction == SHORT) {
                                        levelTrade.setAmountQuoteIn(levelAmountIn);
                                        levelTrade.setAmountBaseOut(levelAmountOut);

                                        lvl.setAvailableAmountQuote(nullToZero(lvl.getAvailableAmountQuote()).subtract(levelAmountIn));
                                        lvl.setAvailableAmountBase(nullToZero(lvl.getAvailableAmountBase()).add(levelAmountOut));
                                    }
                                }

                                // Make sure trade amounts match
                                // TODO: reinstate those checks after WithdrawalDistributor is fixed to properly handle rounding and scale
                                /*if (amountIn.compareTo(amountInCheck) != 0) {
                                    throw new RuntimeException("Trade AmountIn mismatch " + formatAmount(amountIn) + " != " + formatAmount(amountInCheck));
                                }
                                if (amountOut.compareTo(amountOutCheck) != 0) {
                                    throw new RuntimeException("Trade AmountOut mismatch " + formatAmount(amountOut) + " != " + formatAmount(amountOutCheck));
                                }*/

                                // 3. Create a new Level, containing retained amounts
                                if (retainedAmount.compareTo(BigDecimal.ZERO) > 0 || retainedDebt.compareTo(BigDecimal.ZERO) > 0) {
                                    // 3.1. Determine updated holdings and debts for each level
                                    levelHoldingsAndDebts = getLevelHoldingsAndDebts(levels, direction, false);

                                    // Make sure retained amount is within limits
                                    // TODO: SCALE is all over the place, need to properly handle it in all calculations and checks
                                    if (fullAmountToTrade.subtract(amountInForCheck).compareTo(retainedAmount) < 0) {
                                        throw new RuntimeException("RetainedAmount too large: FullAmount " + formatAmount(fullAmountToTrade) +
                                                "; TradedAmount " + formatAmount(amountIn) + "; RetainedAmount " + retainedAmount);
                                    }

                                    // 3.2 Create a new level for this poplavok to transfer retained debt and amount to
                                    Level newLevel = new Level();
                                    newLevel.setState(LevelState.INCEPTION);
                                    newLevel.setCreationDate(new Date());
                                    newLevel.setPoplavok(checkNotNull(poplavok));
                                    newLevel.setProjectedPrice(nullToZero(averagingPrice));

                                    if (direction == LONG) {
                                        // LONG - Retain BASE
                                        newLevel.setProjectedAmountBase(retainedAmount);
                                        newLevel.setProjectedAmountQuote(retainedDebt);
                                    } else { //if (direction == SHORT) {
                                        // SHORT - Retain QUOTE
                                        newLevel.setProjectedAmountQuote(retainedAmount);
                                        newLevel.setProjectedAmountBase(retainedDebt);
                                    }

                                    newLevel.setNotes("Proceeds of averaging trade");

                                    // 3.2 Loan Transfer to new level in the amount of retainedDebt (potentially from multiple input levels)
                                    List<BigDecimal> retainedLevelAmounts = WithdrawalDistributor.distributeWithdrawal(levelHoldingsAndDebts.levelHoldings, retainedAmount, SCALE);
                                    // TODO: situations are possible, in which levels don't have enough debt to transfer. We need to understand what to do in such cases.
                                    List<BigDecimal> retainedLevelDebts = WithdrawalDistributor.distributeWithdrawal(levelHoldingsAndDebts.levelDebts, retainedDebt, SCALE/*, true*/);

                                    BigDecimal transferredLoanAmount = BigDecimal.ZERO;
                                    List<LoanTransfer> loanTransfers = new ArrayList<>();
                                    for (int i = 0; i < levels.size(); i++) {
                                        BigDecimal retainDebtFromLevel = retainedLevelDebts.get(i);

                                        // Go through all levels to TransferLoan
                                        Level lvlFrom = levels.get(i);

                                        List<Loan> loansOnLvlFrom = DBUtil.connectGetResultAndClose(sess ->
                                                LoanDAO.findByDestinationLevel(sess, lvlFrom)
                                        );

                                        BigDecimal leftToTransferForLevel = retainDebtFromLevel;
                                        BigDecimal transferredAmountForLevelCheck = BigDecimal.ZERO;
                                        for (Loan loan : loansOnLvlFrom) {
                                            if (loan.getTransferableAmount().compareTo(BigDecimal.ZERO) > 0) {
                                                BigDecimal loanTransferAmount = leftToTransferForLevel.compareTo(loan.getTransferableAmount()) <= 0 ? leftToTransferForLevel : loan.getTransferableAmount();
                                                if (loanTransferAmount.compareTo(BigDecimal.ZERO) <= 0) {
                                                    break;
                                                }

                                                LoanTransfer loanTransfer = LoanTransferManager.transferLoan(loan,
                                                        lvlFrom, newLevel, loanTransferAmount, new Date());
                                                loanTransfers.add(loanTransfer);

                                                leftToTransferForLevel = leftToTransferForLevel.subtract(loanTransferAmount);
                                                transferredAmountForLevelCheck = transferredAmountForLevelCheck.add(loanTransferAmount);
                                            }
                                        }

                                        if (transferredAmountForLevelCheck.compareTo(retainDebtFromLevel) != 0) {
                                            throw new RuntimeException("Transferred amount for level doesn't match expected retainDebtFromLevel: " +
                                                    formatAmount(transferredAmountForLevelCheck) + " != " + formatAmount(retainDebtFromLevel));
                                        }

                                        transferredLoanAmount = transferredLoanAmount.add(transferredAmountForLevelCheck);
                                    }

                                    // TODO: reinstate those checks after WithdrawalDistributor is fixed to properly handle rounding and scale
                                    /*if (transferredLoanAmount.compareTo(retainedDebt) != 0) {
                                        throw new RuntimeException("Transferred loan amount doesn't match retained debt: " +
                                                formatAmount(transferredLoanAmount) + " != " + formatAmount(retainedDebt));
                                    }*/

                                    // 3.3 Take Profit to new level in the amount of retainedAmount (potentially from multiple input levels)
                                    //      Important to note here: normally TakeProfit requires absence of debt on src level, but in this case it's possible that
                                    //      debt is not fully covered. We allow to TakeProfit in this case still, because we transfer it alongside debt.
                                    BigDecimal transferredHoldingsAmount = BigDecimal.ZERO;
                                    List<Repayment> takeProfitTransfers = new ArrayList<>();
                                    for (int i = 0; i < levels.size(); i++) {
                                        BigDecimal retainHoldingsFromLevel = retainedLevelAmounts.get(i);

                                        // Go through all levels to TransferLoan
                                        Level lvlFrom = levels.get(i);

                                        String proceedsCurrency;
                                        if (direction == LONG) {
                                            proceedsCurrency = lvlFrom.getPoplavok().getTicker().getBase().getCurrency();
                                        } else { //if (direction == SHORT) {
                                            proceedsCurrency = lvlFrom.getPoplavok().getTicker().getQuote().getCurrency();
                                        }

                                        //TODO: error here
                                        Repayment dbRepayment = RepaymentManager.takeProfitToLevel(lvlFrom, ticker, newLevel, retainHoldingsFromLevel, proceedsCurrency, new Date());
                                        takeProfitTransfers.add(dbRepayment);

                                        transferredHoldingsAmount = transferredHoldingsAmount.add(retainHoldingsFromLevel);
                                    }

                                    // TODO: reinstate those checks after WithdrawalDistributor is fixed to properly handle rounding and scale
                                    /*if (transferredHoldingsAmount.compareTo(retainedAmount) != 0) {
                                        throw new RuntimeException("Transferred holdings amount doesn't match retained amount: " +
                                                formatAmount(transferredHoldingsAmount) + " != " + formatAmount(retainedAmount));
                                    }*/

                                    DBUtil.connectCommitAndClose(sess -> {
                                        TradeDAO.save(sess, trade);
                                        for (int i = 0; i < levelTrades.size(); i++) {
                                            LevelTradeDAO.save(sess, levelTrades.get(i));
                                        }
                                        for (int i = 0; i < levels.size(); i++) {
                                            LevelDAO.update(sess, levels.get(i));
                                        }
                                        LevelDAO.save(sess, newLevel);
                                        for (int i = 0; i < loanTransfers.size(); i++) {
                                            LoanTransfer loanTransfer = loanTransfers.get(i);
                                            RepaymentDAO.save(sess, checkNotNull(loanTransfer.getRepayment()));
                                            LoanDAO.save(sess, checkNotNull(loanTransfer.getLoanTo()));
                                            LoanTransferDAO.update(sess, loanTransfer);
                                        }
                                        for (int i = 0; i < takeProfitTransfers.size(); i++) {
                                            Repayment takeProfitTransfer = takeProfitTransfers.get(i);
                                            RepaymentDAO.save(sess, takeProfitTransfer);
                                        }
                                    });
                                } else {
                                    DBUtil.connectCommitAndClose(sess -> {
                                        TradeDAO.save(sess, trade);
                                        for (int i = 0; i < levelTrades.size(); i++) {
                                            LevelTradeDAO.save(sess, levelTrades.get(i));
                                        }
                                        for (int i = 0; i < levels.size(); i++) {
                                            LevelDAO.update(sess, levels.get(i));
                                        }
                                    });
                                }

                                refreshContent();
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error performing averaging trade: " + e, ButtonType.OK);
                            LOGGER.error("Error performing averaging trade: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error performing averaging trade: " + e, ButtonType.OK);
            LOGGER.error("Error performing averaging trade: ", e);
            alert.showAndWait();
        }
    }

    public void invertLevels() {
        try {
            List<Level> selected = checkNotNull(levelsTable).getSelectionModel().getSelectedItems();
            if (selected == null || selected.isEmpty()) {
                showErrorMessage("Please select 1 or more levels to trade.");
                return;
            }

            BigDecimal averagingPrice = checkNotNull(averagingPane).getAveragingPrice();
            BigDecimal fee = null;
            try {
                fee = nullToZero(fromString(checkNotNull(feeTextField).textProperty().get()));
            } catch (Exception e) {}

            CreateInverseLevelDialog createInverseLevelDialog = new CreateInverseLevelDialog(selected, checkNotNull(poplavok), fee, averagingPrice);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.mainStage),
                    stage -> { createInverseLevelDialog.setStage(stage); return createInverseLevelDialog; },
                    "Create Inverse Level");

            workspaceStage.setOnHidden(
                    ev -> {
                        PoplavokAndInverseLevel poplavokAndInverseLevel = createInverseLevelDialog.getReturnPoplavokAndInverseLevel();
                        if (poplavokAndInverseLevel != null) {
                            DBUtil.connectCommitAndClose(sess -> {
                                if (poplavokAndInverseLevel.isNewPoplavok()) {
                                    PoplavokDAO.save(sess, poplavokAndInverseLevel.poplavok());
                                } else {
                                    PoplavokDAO.update(sess, poplavokAndInverseLevel.poplavok());
                                }
                                LevelDAO.save(sess, poplavokAndInverseLevel.level());

                                for (Level initialSourceLevel : poplavokAndInverseLevel.initialLoanSourceLevels()) {
                                    LevelDAO.update(sess, initialSourceLevel);
                                }
                                for (Loan initialLoan : poplavokAndInverseLevel.initialLoans()) {
                                    LoanDAO.save(sess, initialLoan);
                                }
                            });

                            mainApp.openPoplavokTab(poplavokAndInverseLevel.poplavok().getId(), poplavokAndInverseLevel.poplavok().getName());
                        }

                        refreshContent();
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error inverting Levels: " + e, ButtonType.OK);
            LOGGER.error("Error inverting Levels: ", e);
            alert.showAndWait();
        }
    }

    public void performTrade() {
        try {
            List<Level> selected = checkNotNull(levelsTable).getSelectionModel().getSelectedItems();
            if (selected == null || selected.size() != 1) {
                showErrorMessage("Please select exactly 1 level to trade.");
                return;
            }

            Level lvl = selected.get(0);
            LevelState state = lvl.getState();

            if (state != LevelState.TRADING && state != LevelState.FUNDING) {
                showErrorMessage("Levels in " + state + " state can't perform trades.");
                return;
            }

            BigDecimal price = BigDecimalUtil.fromString(checkNotNull(priceTextField).textProperty().get());
            price = price == null ? lvl.getProjectedPrice() : price;
            PerformTradeDialog performTradeDialog = new PerformTradeDialog(mainApp, lvl.getAvailableAmountBase(), lvl.getAvailableAmountQuote(),
                    checkNotNull(poplavok).getTicker(),
                    checkNotNull(checkNotNull(poplavok).getDirection()), price);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.mainStage),
                    stage -> { performTradeDialog.setStage(stage); return performTradeDialog; },
                    "Perform Trade");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            Trade trade = performTradeDialog.getReturnTrade();
                            if (trade != null) {
                                BigDecimal baseIn = nullToZero(trade.getAmountBaseIn());
                                BigDecimal quoteIn = nullToZero(trade.getAmountQuoteIn());
                                BigDecimal baseOut = nullToZero(trade.getAmountBaseOut());
                                BigDecimal quoteOut = nullToZero(trade.getAmountQuoteOut());
                                BigDecimal baseCommission = nullToZero(trade.getCommissionBase());
                                BigDecimal quoteCommission = nullToZero(trade.getCommissionQuote());

                                BigDecimal lvlBase = nullToZero(lvl.getAvailableAmountBase());
                                BigDecimal lvlQuote = nullToZero(lvl.getAvailableAmountQuote());

                                if (lvlBase.compareTo(baseIn) < 0) {
                                    throw new RuntimeException("Not enough Base available in level for this trade");
                                }

                                if (lvlQuote.compareTo(quoteIn) < 0) {
                                    throw new RuntimeException("Not enough Quote available in level for this trade");
                                }

                                lvl.setState(LevelState.TRADING);

                                lvlBase = lvlBase.subtract(baseIn).add(baseOut);
                                lvlQuote = lvlQuote.subtract(quoteIn).add(quoteOut);

                                lvl.setAvailableAmountBase(lvlBase);
                                lvl.setAvailableAmountQuote(lvlQuote);

                                LevelTrade levelTrade = new LevelTrade();
                                levelTrade.setLevel(lvl);
                                levelTrade.setTrade(trade);

                                levelTrade.setAmountBaseIn(baseIn);
                                levelTrade.setAmountQuoteIn(quoteIn);
                                levelTrade.setAmountBaseOut(baseOut);
                                levelTrade.setAmountQuoteOut(quoteOut);

                                DBUtil.connectCommitAndClose(sess -> {
                                    sess.persist(trade);
                                    sess.persist(levelTrade);
                                    LevelDAO.update(sess, lvl);
                                });

                                refreshContent();
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error performing trade: " + e, ButtonType.OK);
                            LOGGER.error("Error performing trade: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error performing trade: " + e, ButtonType.OK);
            LOGGER.error("Error performing trade: ", e);
            alert.showAndWait();
        }
    }

    public void allocateFunds() {
        try {
            List<Level> selected = checkNotNull(levelsTable).getSelectionModel().getSelectedItems();
            if (selected == null || selected.isEmpty()) {
                showMessage("Choose a level to allocate funds.");
                return;
            }

            if (selected.size() != 1) {
                showMessage("Choose one level to allocate funds (not many levels)");
                return;
            }

            Level lvl = selected.get(0);
            LevelState state = lvl.getState();

            if (state == LevelState.CLOSED) {
                showMessage("Funds can't be allocated for CLOSED levels.");
                return;
            }

            Currency loanCurrency;
            BigDecimal defaultAmount;
            Direction poplavokDirection = checkNotNull(poplavok).getDirection();
            if (poplavokDirection == Direction.LONG) {
                loanCurrency = checkNotNull(poplavok).getTicker().getQuote();
                defaultAmount = lvl.getProjectedAmountQuote();
                defaultAmount = defaultAmount == null ? BigDecimal.ZERO : defaultAmount;

                // Debt assumes funds already allocated
                if (lvl.getDebtQuote() != null) {
                    if (defaultAmount.compareTo(lvl.getDebtQuote()) > 0) {
                        defaultAmount = defaultAmount.subtract(lvl.getDebtQuote());
                    } else {
                        defaultAmount = BigDecimal.ZERO;
                    }
                }
            } else {
                loanCurrency = checkNotNull(poplavok).getTicker().getBase();
                defaultAmount = lvl.getProjectedAmountBase();
                defaultAmount = defaultAmount == null ? BigDecimal.ZERO : defaultAmount;

                // Debt assumes funds already allocated
                if (lvl.getDebtBase() != null) {
                    if (defaultAmount.compareTo(lvl.getDebtBase()) > 0) {
                        defaultAmount = defaultAmount.subtract(lvl.getDebtBase());
                    } else {
                        defaultAmount = BigDecimal.ZERO;
                    }
                }
            }

            AllocateFundsDialog allocateFundsDialog = new AllocateFundsDialog(lvl, loanCurrency, defaultAmount);
            Stage workspaceStage = ModalWindow.showModal(checkNotNull(mainApp.mainStage),
                    stage -> { allocateFundsDialog.setStage(stage); return allocateFundsDialog; },
                    "Allocate Funds");

            workspaceStage.setOnHidden(
                    ev -> {
                        try {
                            Loan loan = allocateFundsDialog.getReturnLoan();
                            if (loan != null) {
                                if (loan == MOVE_TO_ZERO_FUNDING_STATUS) {
                                    if (lvl.getState() == LevelState.INCEPTION) {
                                        lvl.setState(LevelState.FUNDING);
                                        DBUtil.connectCommitAndClose(sess -> {
                                            LevelDAO.update(sess, lvl);
                                        });
                                        refreshContent();
                                    } else {
                                        showMessage("Can't move to FUNDING. Loan is not in INCEPTION status.");
                                    }
                                    return;
                                }

                                if (loan.getLoanType() != ACCOUNT_FUNDED && loan.getLoanType() != POPLAVOK_FUNDED
                                        && loan.getLoanType() != EXTERNAL_CROSS_MARGIN && loan.getLoanType() != EXTERNAL_ISOLATED_MARGIN) {
                                    showMessage("Unsupported loan type: " + loan.getLoanType());
                                    return;
                                }

                                if (loan.getAmount() == null || loan.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                                    showMessage("Loan must have a positive amount: > 0");
                                    return;
                                }

                                // Add loan funds to our level
                                if (poplavokDirection == Direction.LONG) {
                                    lvl.setAvailableAmountQuote(lvl.getAvailableAmountQuote() != null ? lvl.getAvailableAmountQuote().add(loan.getAmount()) : loan.getAmount());
                                    lvl.setDebtQuote(lvl.getDebtQuote() != null ? lvl.getDebtQuote().add(loan.getAmount()) : loan.getAmount());
                                } else {
                                    lvl.setAvailableAmountBase(lvl.getAvailableAmountBase() != null ? lvl.getAvailableAmountBase().add(loan.getAmount()) : loan.getAmount());
                                    lvl.setDebtBase(lvl.getDebtBase() != null ? lvl.getDebtBase().add(loan.getAmount()) : loan.getAmount());
                                }

                                if (lvl.getState() == LevelState.INCEPTION) {
                                    lvl.setState(LevelState.FUNDING);
                                }

                                if (loan.getLoanType() == ACCOUNT_FUNDED) {
                                    // Remove loan funds from source account
                                    Account sourceAccount = checkNotNull(loan.getSourceAccount());
                                    sourceAccount.setAvailableAmount(loan.getSourceAccount().getAvailableAmount().subtract(loan.getAmount()));
                                    sourceAccount.setLentAmount(sourceAccount.getLentAmount() != null ? sourceAccount.getLentAmount().add(loan.getAmount()) : loan.getAmount());

                                    // save new loan, update level and source account
                                    DBUtil.connectCommitAndClose(sess -> {
                                        AccountDAO.update(sess, sourceAccount);
                                        LoanDAO.save(sess, loan);
                                        LevelDAO.update(sess, lvl);
                                    });
                                }
                                if (loan.getLoanType() == POPLAVOK_FUNDED) {
                                    Level sourceLevel = checkNotNull(loan.getSourceLevel());
                                    Poplavok sourcePoplavok = sourceLevel.getPoplavok();
                                    Currency sourceBase = sourcePoplavok.getTicker().getBase();
                                    Currency sourceQuote = sourcePoplavok.getTicker().getQuote();

                                    if (loanCurrency.getCurrency().equals(sourceBase.getCurrency())) {
                                        sourceLevel.setAvailableAmountBase(checkNotNull(sourceLevel.getAvailableAmountBase()).subtract(loan.getAmount()));
                                        sourceLevel.setLentAmountBase(nullToZero(sourceLevel.getLentAmountBase()).add(loan.getAmount()));
                                    } else if (loanCurrency.getCurrency().equals(sourceQuote.getCurrency())) {
                                        sourceLevel.setAvailableAmountQuote(checkNotNull(sourceLevel.getAvailableAmountQuote()).subtract(loan.getAmount()));
                                        sourceLevel.setLentAmountQuote(nullToZero(sourceLevel.getLentAmountQuote()).add(loan.getAmount()));
                                    } else {
                                        showMessage("Source level currency doesn't match loan currency");
                                        return;
                                    }
                                    DBUtil.connectCommitAndClose(sess -> {
                                        LevelDAO.update(sess, sourceLevel);
                                        LoanDAO.save(sess, loan);
                                        LevelDAO.update(sess, lvl);
                                    });
                                }
                                if (loan.getLoanType() == EXTERNAL_CROSS_MARGIN || loan.getLoanType() == EXTERNAL_ISOLATED_MARGIN) {
                                    DBUtil.connectCommitAndClose(sess -> {
                                        LoanDAO.save(sess, loan);
                                        LevelDAO.update(sess, lvl);
                                    });
                                }

                                refreshContent();
                            }
                        } catch (Exception e) {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Error allocating funds: " + e, ButtonType.OK);
                            LOGGER.error("Error allocating funds: ", e);
                            alert.showAndWait();
                        }
                    }
            );
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error allocating funds: " + e, ButtonType.OK);
            LOGGER.error("Error allocating funds: ", e);
            alert.showAndWait();
        }
    }
}
