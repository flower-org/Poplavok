package com.poplavok.forms;

import com.flower.fxutils.ModalWindow;
import com.poplavok.data.model.Currency;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class MainForm {
    @Nullable Stage mainStage;

    @Nullable ApiSettingsDialog apiSettingsDialog;
    @Nullable Stage appSettingsStage;

    @FXML @Nullable Label serverInfoLabel;
    @FXML @Nullable TabPane tabs;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    public ApiSettingsDialog getApiSettingsDialog() {
        return checkNotNull(apiSettingsDialog);
    }

    public void init(Stage mainStage) {
        this.mainStage = mainStage;
        this.apiSettingsDialog = new ApiSettingsDialog(mainStage);
        Platform.runLater(this::showApiSettingsDialog);
    }

    public void setStatusText(String text) {
        checkNotNull(serverInfoLabel).setText(text);
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Poplavok v 1.0.9", ButtonType.OK);
        alert.showAndWait();
    }

    void addTab(Tab tab) {
        checkNotNull(tabs).getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
    }

    public void quit() { checkNotNull(mainStage).close(); }

    public void closeAllTabs() {
        checkNotNull(tabs).getTabs().clear();
    }

    public void openCurrenciesTab() {
        CurrencyTab currencyTab = new CurrencyTab(this);
        final Tab tab = new Tab("Currencies", currencyTab);
        tab.setClosable(true);

        addTab(tab);
    }

    public void createCurrencyDetailTab(Currency currency) {
        Currency_geTaJlu currency_geTaJlu = new Currency_geTaJlu(currency.getCurrency());
        final Tab tab = new Tab(currency.getCurrency(), currency_geTaJlu);
        tab.setClosable(true);

        addTab(tab);
    }

    public void openMarketTickersTab() {
        MarketTickerTab marketTickerTab = new MarketTickerTab(/*this*/);
        final Tab tab = new Tab("MarketTickers", marketTickerTab);
        tab.setClosable(true);

        addTab(tab);
    }

    public void openAccountsTab() {
        AccountListTab accountListTab = new AccountListTab(this);
        final Tab tab = new Tab("Accounts", accountListTab);
        tab.setClosable(true);

        addTab(tab);
    }

    public void openPoplavokListTab() {
        PoplavokListTab poplavokListTab = new PoplavokListTab(this);
        final Tab tab = new Tab("§§ Poplavoks", poplavokListTab);
        tab.setClosable(true);

        addTab(tab);
    }

    public void openPoplavokTab(Long poplavokId, @Nullable String poplavokName) {
        String finalPoplavokName = StringUtils.defaultIfBlank(poplavokName, poplavokId.toString());

        for (Tab existingTab : checkNotNull(tabs).getTabs()) {
            if (existingTab.getContent() instanceof PoplavokTab existingPoplavokTab) {
                if (existingPoplavokTab.poplavokId.equals(poplavokId)) {
                    tabs.getSelectionModel().select(existingTab);
                    existingPoplavokTab.refreshContent();
                    return;
                }
            }
        }

        PoplavokTab poplavokTab = new PoplavokTab(this, poplavokId);
        final Tab tab = new Tab("§ " + finalPoplavokName, poplavokTab);
        tab.setClosable(true);

        addTab(tab);
    }

    public void showApiSettingsDialog() {
        if (appSettingsStage == null) {
            appSettingsStage = ModalWindow.showModal(checkNotNull(mainStage),
                    stage -> apiSettingsDialog,
                    "API Settings");
        } else {
            appSettingsStage.show();
        }
    }
}
