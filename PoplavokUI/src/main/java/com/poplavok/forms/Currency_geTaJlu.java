package com.poplavok.forms;

import com.KyKu4.MogeJlb.response.CurrencyExtendedInfoResponse;
import com.KyKu4.MogeJlb.response.InfoRecord;
import com.flower.fxutils.ModalWindow;
import com.flower.fxutils.ProgressForm;
import com.flower.fxutils.Refreshable;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.poplavok.data.dao.CurrencyChainDAO;
import com.poplavok.data.dao.CurrencyDAO;
import com.poplavok.data.dao.CurrencyExtendedInfoDAO;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.CurrencyChainEx;
import com.poplavok.data.model.CurrencyExtendedInfo;
import com.poplavok.data.utils.DBUtil;
import com.poplavok.kucoin.EntityConverter;
import com.poplavok.kucoin.KucoinTool;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class Currency_geTaJlu extends AnchorPane implements Refreshable {
    final static Logger LOGGER = LoggerFactory.getLogger(Currency_geTaJlu.class);

    @Nullable @FXML TextField currencyId;
    @Nullable @FXML TextField fullName;
    @Nullable @FXML TextField currencyText;
    @Nullable @FXML TextField currencyName;
    @Nullable @FXML TextField precision;
    @Nullable @FXML TextField withdrawalMinSize;
    @Nullable @FXML TextField withdrawalMinFee;
    @Nullable @FXML TextField isWithdrawEnabled;
    @Nullable @FXML TextField isDepositEnabled;
    @Nullable @FXML TextField isMarginEnabled;
    @Nullable @FXML TextField isDebitEnabled;

    @Nullable @FXML TextField currencyExtendedInfoId;
    @Nullable @FXML TextField code;
    @Nullable @FXML TextField symbol;
    @Nullable @FXML TextField currencyExtendedInfoName;
    @Nullable @FXML TextField marketCap;
    @Nullable @FXML TextField circulatingSupply;
    @Nullable @FXML TextField totalSupply;
    @Nullable @FXML TextField maxSupply;
    @Nullable @FXML TextField website;
    @Nullable @FXML TextField whitepaper;
    @Nullable @FXML TextField issueDate;
    @Nullable @FXML TextField issuePrice;
    @Nullable @FXML TextField roi;

    @Nullable @FXML TextArea explorer;
    @Nullable @FXML TextArea introduceText;

    @Nullable @FXML TableView<CurrencyChainEx> currencyChainExTable;

    final long currencyIdLong;

    public Currency_geTaJlu(long currencyIdLong) {
        this.currencyIdLong = currencyIdLong;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Currency_geTaJlu.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        refreshContent();
    }

    public void retrieveExtendedData(ActionEvent event) {
        try {
            ModalWindow.showModal(event,
                    stage -> new ProgressForm(stage, this, progressCallback -> KucoinTool.retrieveCurrencyDetailsForCurrency(progressCallback, currencyIdLong)),
                    "Retrieve extended data",
                    StageStyle.UNDECORATED);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    @Override
    public void refreshContent() {
        try {
            Currency currency = DBUtil.connectGetResultAndClose(conn -> CurrencyDAO.findById(conn, currencyIdLong)).orElseThrow(() -> new RuntimeException("Currency not found"));
            CurrencyExtendedInfo currencyExtendedInfo = DBUtil.connectGetResultAndClose(conn -> CurrencyExtendedInfoDAO.findById(conn, currencyIdLong)).orElse(null);
            List<CurrencyChainEx> currencyChains = DBUtil.connectGetResultAndClose(conn -> CurrencyChainDAO.getForCurrencyEx(conn, currencyIdLong));

            updateCurrencyView(currency);
            updateCurrencyExtendedInfoView(currencyExtendedInfo);
            updateCurrencyChainsView(currencyChains);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    void updateCurrencyView(Currency currency) {
        Preconditions.checkNotNull(currencyId).setText(currency.getId().toString());
        Preconditions.checkNotNull(fullName).setText(currency.getFullName());
        Preconditions.checkNotNull(currencyText).setText(currency.getCurrency());
        Preconditions.checkNotNull(currencyName).setText(currency.getName());
        Preconditions.checkNotNull(precision).setText(currency.getPrecision() == null ? "-" : currency.getPrecision().toString());
        Preconditions.checkNotNull(withdrawalMinSize).setText(currency.getWithdrawalMinSize());
        Preconditions.checkNotNull(withdrawalMinFee).setText(currency.getWithdrawalMinFee());
        Preconditions.checkNotNull(isWithdrawEnabled).setText(currency.getIsWithdrawEnabled() == null ? "-" : currency.getIsWithdrawEnabled().toString());
        Preconditions.checkNotNull(isDepositEnabled).setText(currency.getIsDepositEnabled() == null ? "-" : currency.getIsDepositEnabled().toString());
        Preconditions.checkNotNull(isMarginEnabled).setText(currency.getIsMarginEnabled() == null ? "-" : currency.getIsMarginEnabled().toString());
        Preconditions.checkNotNull(isDebitEnabled).setText(currency.getIsDebitEnabled() == null ? "-" : currency.getIsDebitEnabled().toString());
    }

    void updateCurrencyExtendedInfoView(@Nullable CurrencyExtendedInfo currencyExtendedInfo) {
        if (currencyExtendedInfo == null) {
            Preconditions.checkNotNull(currencyExtendedInfoId).setText("-");
            Preconditions.checkNotNull(code).setText("-");
            Preconditions.checkNotNull(symbol).setText("-");
            Preconditions.checkNotNull(currencyExtendedInfoName).setText("-");
            Preconditions.checkNotNull(marketCap).setText("-");
            Preconditions.checkNotNull(circulatingSupply).setText("-");
            Preconditions.checkNotNull(totalSupply).setText("-");
            Preconditions.checkNotNull(maxSupply).setText("-");
            Preconditions.checkNotNull(website).setText("-");
            Preconditions.checkNotNull(whitepaper).setText("-");
            Preconditions.checkNotNull(issueDate).setText("-");
            Preconditions.checkNotNull(issuePrice).setText("-");
            Preconditions.checkNotNull(roi).setText("-");

            Preconditions.checkNotNull(explorer).setText("-");
            Preconditions.checkNotNull(introduceText).setText("-");
        } else {
            CurrencyExtendedInfoResponse info = EntityConverter.fromCurrencyExtendedInfo(currencyExtendedInfo);

            Preconditions.checkNotNull(currencyExtendedInfoId).setText(currencyExtendedInfo.currencyExtendedInfoId() == null ? "-" : currencyExtendedInfo.currencyExtendedInfoId().toString());
            Preconditions.checkNotNull(code).setText(info.code());
            Preconditions.checkNotNull(symbol).setText(currencyExtendedInfo.symbol());
            Preconditions.checkNotNull(currencyExtendedInfoName).setText(info.coinName());
            Preconditions.checkNotNull(marketCap).setText(getString(info.marketCap()));
            Preconditions.checkNotNull(circulatingSupply).setText(getString(info.circulatingSupply()));
            Preconditions.checkNotNull(totalSupply).setText("N/A");
            Preconditions.checkNotNull(maxSupply).setText(getString(info.maxSupply()));
            Preconditions.checkNotNull(website).setText(getFirstItemString(info.website()));
            Preconditions.checkNotNull(whitepaper).setText(getFirstItemString(info.whitePaperList(), InfoRecord::text));
            Preconditions.checkNotNull(issueDate).setText(info.dateAdded());
            Preconditions.checkNotNull(issuePrice).setText(getString(info.currentPrice()));
            Preconditions.checkNotNull(roi).setText("N/A");

            Preconditions.checkNotNull(explorer).setText(getAllItemStrings(info.explorer(), Object::toString, "---------------------------------------------------------"));
            Preconditions.checkNotNull(introduceText).setText(getAllItemStrings(info.currencyIntroduction(), InfoRecord::subText, "---------------------------------------------------------"));
        }
    }

    static <T> String getString(@Nullable T item) {
        return getString(item, Object::toString);
    }

    static <T> String getString(@Nullable T item, Function<T, String> toStringFunc) {
        return item != null ? StringUtils.defaultIfBlank(toStringFunc.apply(item), "") : "N/A";
    }

    static <T> String getFirstItemString(List<T> items) {
        return getFirstItemString(items, Object::toString);
    }

    static <T> String getFirstItemString(List<T> items, Function<T, String> toStringFunc) {
        return items != null && !items.isEmpty() ? StringUtils.defaultIfBlank(toStringFunc.apply(items.get(0)), "") : "N/A";
    }

    static <T> String getAllItemStrings(List<T> items, Function<T, String> toStringFunc, String separator) {
        StringBuilder builder = new StringBuilder();
        if (items != null) {
            boolean first = true;
            for (T item : items) {
                if (!first) {
                    builder.append(separator).append("\n");
                }
                builder.append(toStringFunc.apply(item)).append("\n");
                first = false;
            }
        }

        return builder.toString();
    }

    void updateCurrencyChainsView(@Nullable List<CurrencyChainEx> currencyChains) {
        if (currencyChains == null) {
            currencyChains = ImmutableList.of();
        }
        Preconditions.checkNotNull(currencyChainExTable).itemsProperty().set(FXCollections.observableArrayList(currencyChains));
        currencyChainExTable.refresh();
    }
}
