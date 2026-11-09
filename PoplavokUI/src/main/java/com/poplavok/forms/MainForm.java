package com.poplavok.forms;

import com.flower.fxutils.ModalWindow;
import com.poplavok.data.model.Currency;
import com.poplavok.mcc.MultiCoinChartsService;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static com.flower.crypt.keys.UserPreferencesManager.getUserPreference;
import static com.flower.crypt.keys.UserPreferencesManager.updateUserPreference;
import static com.flower.fxutils.JavaFxUtils.YesNo.YES;
import static com.flower.fxutils.JavaFxUtils.createDecimalTextFormatter;
import static com.flower.fxutils.JavaFxUtils.showYesNoDialog;
import static com.google.common.base.Preconditions.checkNotNull;

public class MainForm {
    @Nullable Stage mainStage;

    @Nullable ApiSettingsDialog apiSettingsDialog;
    @Nullable Stage appSettingsStage;
    @Nullable HostServices hostServices;
    @Nullable Stage multiCoinChartsStage;

    @FXML @Nullable Label serverInfoLabel;
    @FXML @Nullable Label mccStatusLabel;
    @FXML @Nullable TabPane tabs;
    @FXML @Nullable RadioMenuItem defaultProfitInQuoteMenuItem;
    @FXML @Nullable RadioMenuItem defaultProfitInBaseMenuItem;
    @FXML @Nullable TextField defaultCommsCountTextField;
    @FXML @Nullable Slider defaultCommsCountSlider;
    @FXML @Nullable TextField defaultProfitPercentTextField;

    final static String DEFAULT_PROFIT_IN = "Poplavok_DefaultProfitIn";
    final static String PROFIT_IN_QUOTE = "QUOTE";
    final static String PROFIT_IN_BASE = "BASE";
    final static String DEFAULT_COMMS_COUNT = "Poplavok_DefaultCommsCount";
    final static String DEFAULT_PROFIT_PERCENT = "Poplavok_DefaultProfitPercent";
    final static int DEFAULT_COMMS_COUNT_FALLBACK = 1;
    final static String DEFAULT_PROFIT_PERCENT_FALLBACK = "1";
    final static int MAX_COMMS_COUNT = 10;

    /** Default "Profit in" for new Poplavok tabs; QUOTE unless BASE was saved. */
    public static boolean isDefaultProfitInQuote() {
        return !PROFIT_IN_BASE.equals(getUserPreference(DEFAULT_PROFIT_IN));
    }

    /** Default "+ Comms" count for new Poplavok tabs; 1 unless a valid value (0..10) was saved. */
    public static int getDefaultCommsCount() {
        try {
            int count = Integer.parseInt(getUserPreference(DEFAULT_COMMS_COUNT).trim());
            if (count >= 0 && count <= MAX_COMMS_COUNT) {
                return count;
            }
        } catch (NumberFormatException ignored) {
            // fall through to default
        }
        return DEFAULT_COMMS_COUNT_FALLBACK;
    }

    /** Default "+ %" text for new Poplavok tabs; 1 if nothing was ever saved (an explicitly saved empty value is kept). */
    public static String getDefaultProfitPercent() {
        String saved = Preferences.userRoot().get(DEFAULT_PROFIT_PERCENT, null);
        return saved == null ? DEFAULT_PROFIT_PERCENT_FALLBACK : saved.trim();
    }

    public MainForm() {
        //This form is created automatically.
        //No need to load fxml explicitly
    }

    @Nullable protected static MainForm INSTANCE;

    @Nullable public static MainForm getInstance() {
        return checkNotNull(INSTANCE);
    }

    public ApiSettingsDialog getApiSettingsDialog() {
        return checkNotNull(apiSettingsDialog);
    }

    public void init(Stage mainStage, HostServices hostServices) {
        INSTANCE = this;
        this.mainStage = mainStage;
        this.hostServices = hostServices;
        this.apiSettingsDialog = new ApiSettingsDialog(mainStage);
        checkNotNull(tabs).getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    for (Tab removed : change.getRemoved()) {
                        if (removed.getContent() instanceof PoplavokTab poplavokTab) {
                            poplavokTab.dispose();
                        }
                    }
                }
            }
        });
        boolean profitInQuote = isDefaultProfitInQuote();
        checkNotNull(defaultProfitInQuoteMenuItem).setSelected(profitInQuote);
        checkNotNull(defaultProfitInBaseMenuItem).setSelected(!profitInQuote);

        checkNotNull(defaultCommsCountSlider).valueProperty().addListener((observable, oldValue, newValue) ->
                checkNotNull(defaultCommsCountTextField).setText(String.valueOf(newValue.intValue())));
        defaultCommsCountSlider.setValue(getDefaultCommsCount());
        checkNotNull(defaultCommsCountTextField).setText(String.valueOf(getDefaultCommsCount()));
        checkNotNull(defaultProfitPercentTextField).setTextFormatter(createDecimalTextFormatter());
        defaultProfitPercentTextField.setText(getDefaultProfitPercent());

        MultiCoinChartsService multiCoinChartsService = MultiCoinChartsService.getInstance();
        checkNotNull(mccStatusLabel).textProperty().bind(
                Bindings.concat("MultiCoinCharts: ", multiCoinChartsService.statusTextProperty()));
        if (MultiCoinChartsService.isAutoStart()) {
            multiCoinChartsService.start(MultiCoinChartsService.savedPort());
        }

        Platform.runLater(this::showApiSettingsDialog);
    }

    public void onDefaultProfitInChanged() {
        boolean profitInQuote = checkNotNull(defaultProfitInQuoteMenuItem).isSelected();
        if (profitInQuote == isDefaultProfitInQuote()) {
            return;
        }
        updateUserPreference(Preferences.userRoot(), DEFAULT_PROFIT_IN, profitInQuote ? PROFIT_IN_QUOTE : PROFIT_IN_BASE);

        List<PoplavokTab> openPoplavokTabs = getOpenPoplavokTabs();
        if (!openPoplavokTabs.isEmpty() && YES == showYesNoDialog("Default Profit In",
                "Set \"Profit in\" to " + (profitInQuote ? PROFIT_IN_QUOTE : PROFIT_IN_BASE)
                        + " for all " + openPoplavokTabs.size() + " open Poplavok tab(s)?")) {
            openPoplavokTabs.forEach(poplavokTab -> poplavokTab.setProfitInQuote(profitInQuote));
        }
    }

    /** Saves the "+ Comms" / "+ %" defaults edited inside the Trade menu once it closes, offering to apply them to open tabs. */
    public void onTradeMenuHidden() {
        int commsCount = (int) checkNotNull(defaultCommsCountSlider).getValue();
        String profitPercent = StringUtils.trimToEmpty(checkNotNull(defaultProfitPercentTextField).getText());

        boolean commsChanged = commsCount != getDefaultCommsCount();
        boolean percentChanged = !profitPercent.equals(getDefaultProfitPercent());
        if (!commsChanged && !percentChanged) {
            return;
        }
        if (commsChanged) {
            updateUserPreference(Preferences.userRoot(), DEFAULT_COMMS_COUNT, String.valueOf(commsCount));
        }
        if (percentChanged) {
            // Direct put: updateUserPreference skips "" when the key is absent, which would revert to the "1" fallback
            Preferences.userRoot().put(DEFAULT_PROFIT_PERCENT, profitPercent);
        }

        List<PoplavokTab> openPoplavokTabs = getOpenPoplavokTabs();
        if (openPoplavokTabs.isEmpty()) {
            return;
        }
        List<String> changes = new ArrayList<>();
        if (commsChanged) {
            changes.add("\"+ Comms\" to " + commsCount);
        }
        if (percentChanged) {
            changes.add("\"+ %\" to " + (profitPercent.isEmpty() ? "(empty)" : profitPercent));
        }
        // Defer so the prompt doesn't appear while the menu is still closing
        Platform.runLater(() -> {
            if (YES == showYesNoDialog("Averaging Defaults",
                    "Set " + String.join(" and ", changes) + " for all " + openPoplavokTabs.size() + " open Poplavok tab(s)?")) {
                openPoplavokTabs.forEach(poplavokTab -> {
                    if (commsChanged) {
                        poplavokTab.setCommsCount(commsCount);
                    }
                    if (percentChanged) {
                        poplavokTab.setProfitPercent(profitPercent);
                    }
                });
            }
        });
    }

    protected List<PoplavokTab> getOpenPoplavokTabs() {
        return checkNotNull(tabs).getTabs().stream()
                .filter(tab -> tab.getContent() instanceof PoplavokTab)
                .map(tab -> (PoplavokTab) tab.getContent())
                .toList();
    }

    public void setStatusText(String text) {
        checkNotNull(serverInfoLabel).setText(text);
    }

    public void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Poplavok v 1.2.3", ButtonType.OK);
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

    public void showMultiCoinChartsDialog() {
        if (multiCoinChartsStage == null) {
            // Not modal: the dialog can stay open while working in Poplavok
            Stage stage = new Stage();
            stage.initOwner(checkNotNull(mainStage));
            stage.initModality(Modality.NONE);
            stage.setTitle("MultiCoinCharts");
            stage.setResizable(false);
            stage.setScene(new Scene(new MultiCoinChartsDialog(checkNotNull(hostServices))));
            multiCoinChartsStage = stage;
        }
        multiCoinChartsStage.show();
        multiCoinChartsStage.toFront();
    }
}
