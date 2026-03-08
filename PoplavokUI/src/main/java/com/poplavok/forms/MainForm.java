package com.poplavok.forms;

import com.poplavok.forms.TestForm;
import com.google.common.base.Preconditions;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import javax.annotation.Nullable;

public class MainForm {
    @Nullable Stage mainStage;
    @FXML @Nullable Label serverInfoLabel;
    @FXML @Nullable TabPane tabs;
    int testFormCount = 0;

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    public void setMainStage(@Nullable Stage mainStage) {
        this.mainStage = mainStage;
    }

    public void setStatusText(String text) {
        Preconditions.checkNotNull(serverInfoLabel).setText(text);
    }

    public void showTestForm() {
        TestForm testForm = new TestForm();
        final Tab tab = new Tab("Test " + (testFormCount++), testForm);
        tab.setClosable(true);

        addTab(tab);
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Poplavok v 0.0.1", ButtonType.OK);
        alert.showAndWait();
    }

    void addTab(Tab tab) {
        Preconditions.checkNotNull(tabs).getTabs().add(tab);
        tabs.getSelectionModel().select(tab);
    }

    public void quit() { Preconditions.checkNotNull(mainStage).close(); }

    public void closeAllTabs() {
        Preconditions.checkNotNull(tabs).getTabs().clear();
    }

    public void openCurrenciesTab() {
        CurrencyTab currencyTab = new CurrencyTab(/*this*/);
        final Tab tab = new Tab("Currencies", currencyTab);
        tab.setClosable(true);

        addTab(tab);
    }

    public void openMarketTickersTab() {
        MarketTickerTab marketTickerTab = new MarketTickerTab(/*this*/);
        final Tab tab = new Tab("MarketTickers", marketTickerTab);
        tab.setClosable(true);

        addTab(tab);
    }
}
