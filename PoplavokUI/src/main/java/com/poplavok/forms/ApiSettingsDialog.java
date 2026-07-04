package com.poplavok.forms;

import com.flower.crypt.keys.forms.MultiKeyProvider;
import com.flower.crypt.keys.forms.RsaFileKeyProvider;
import com.flower.crypt.keys.forms.RsaPkcs11KeyProvider;
import com.flower.crypt.keys.forms.RsaRawKeyProvider;
import com.flower.crypt.keys.forms.TabKeyProvider;
import com.flower.fxutils.JavaFxUtils;
import com.poplavok.api.kucoin.auth.KucoinCredentialsProvider;
import com.poplavok.kucoin.EncryptedCredentialsProvider;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;

import static com.flower.crypt.keys.UserPreferencesManager.getUserPreference;
import static com.flower.crypt.keys.UserPreferencesManager.updateUserPreference;
import static com.google.common.base.Preconditions.checkNotNull;

public class ApiSettingsDialog extends VBox {
    final static Logger LOGGER = LoggerFactory.getLogger(ApiSettingsDialog.class);

    final static String SETTINGS_FILE = "Poplavok_ApiSettingsFile";
    @Nullable protected KucoinCredentialsProvider credentialsProvider;

    protected static void updateSettingsFileUserPreferences(String settingsFile) {
        Preferences userPreferences = Preferences.userRoot();
        updateUserPreference(userPreferences, SETTINGS_FILE, settingsFile);
    }

    protected static String settingsFile() { return getUserPreference(SETTINGS_FILE); }

    @Nullable Stage stage;
    @Nullable TabKeyProvider keyProvider;
    @FXML @Nullable AnchorPane topPane;
    @FXML @Nullable TextField settingsFileTextField;

    public ApiSettingsDialog(Stage stage) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ApiSettingsDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        init(stage);

        checkNotNull(settingsFileTextField).textProperty().setValue(settingsFile());
        checkNotNull(settingsFileTextField).textProperty().addListener(this::settingsFileTextFieldTextChanged);
    }

    protected void init(Stage stage) {
        this.stage = stage;

        keyProvider = buildMainKeyProvider(stage);
        AnchorPane keyProviderForm = keyProvider.tabContent();
        checkNotNull(topPane).getChildren().add(keyProviderForm);
        AnchorPane.setTopAnchor(keyProviderForm, 0.0);
        AnchorPane.setBottomAnchor(keyProviderForm, 0.0);
        AnchorPane.setLeftAnchor(keyProviderForm, 0.0);
        AnchorPane.setRightAnchor(keyProviderForm, 0.0);

        keyProvider.initPreferences();
    }

    protected static TabKeyProvider buildMainKeyProvider(Stage mainStage) {
        RsaPkcs11KeyProvider rsaPkcs11KeyProvider = new RsaPkcs11KeyProvider(mainStage);
        RsaFileKeyProvider rsaFileKeyProvider = new RsaFileKeyProvider(mainStage);
        RsaRawKeyProvider rsaRawKeyProvider = new RsaRawKeyProvider();
        return new MultiKeyProvider(mainStage, "RSA-2048",
                List.of(rsaPkcs11KeyProvider, rsaFileKeyProvider, rsaRawKeyProvider));
    }

    public void openApiSettingsFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("API Settings (*.yaml.crp)", "*.yaml.crp"));
        fileChooser.setTitle("Load API Settings");

        File settingsFile = fileChooser.showOpenDialog(checkNotNull(stage));
        if (settingsFile != null && settingsFile.exists()) {
            checkNotNull(settingsFileTextField).setText(settingsFile.getAbsolutePath());
        }
    }

    public void settingsFileTextFieldTextChanged(ObservableValue<? extends String> observable, String _old, String _new) {
        updateSettingsFileUserPreferences(checkNotNull(settingsFileTextField).getText());
    }

    public void testSettingsLoad() {
        try {
            KucoinCredentialsProvider credentialsProvider = loadProvider();
            if (credentialsProvider == null) { return; }

            if (StringUtils.isBlank(credentialsProvider.getApiKey())) {
                JavaFxUtils.showMessage("Credentials Provider Test failed.");
            } else {
                JavaFxUtils.showMessage("Credentials Provider Test succeeded.");
            }
        } catch (Exception e) {
            LOGGER.error("Error testing credentials provider", e);
            JavaFxUtils.showErrorMessage("Error testing credentials provider: " + e.getMessage());
        }
    }

    public void done() {
        try {
            this.credentialsProvider = loadProvider();
            if (credentialsProvider == null) { return; }

            if (getScene() != null && getScene().getWindow() != null) {
                getScene().getWindow().hide();
            }
        } catch (Exception e) {
            LOGGER.error("Error creating credentials provider", e);
            JavaFxUtils.showErrorMessage("Error creating credentials provider: " + e.getMessage());
        }
    }

    protected @Nullable KucoinCredentialsProvider loadProvider() throws IOException {
        File settingsFile = new File(checkNotNull(settingsFileTextField).getText());
        if (!settingsFile.exists()) {
            JavaFxUtils.showErrorMessage("Settings file does not exist: " + settingsFile.getAbsolutePath());
            return null;
        }
        return new EncryptedCredentialsProvider(checkNotNull(keyProvider), settingsFile);
    }
}
