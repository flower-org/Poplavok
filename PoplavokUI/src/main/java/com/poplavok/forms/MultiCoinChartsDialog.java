package com.poplavok.forms;

import com.poplavok.mcc.MultiCoinChartsService;
import com.poplavok.mcc.MultiCoinChartsService.State;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javax.annotation.Nullable;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/** Start/stop the embedded MultiCoinCharts server and open it in the browser. */
public class MultiCoinChartsDialog extends VBox {
    final static String ERROR_STYLE = "-fx-text-fill: red;";
    final static String INVALID_FIELD_STYLE = "-fx-border-color: red;";

    final MultiCoinChartsService service = MultiCoinChartsService.getInstance();
    final HostServices hostServices;

    @FXML @Nullable Label statusLabel;
    @FXML @Nullable TextField portTextField;
    @FXML @Nullable CheckBox autoStartCheckBox;
    @FXML @Nullable Button startButton;
    @FXML @Nullable Button stopButton;
    @FXML @Nullable Button openInBrowserButton;

    public MultiCoinChartsDialog(HostServices hostServices) {
        this.hostServices = hostServices;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MultiCoinChartsDialog.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        checkNotNull(statusLabel).textProperty().bind(service.statusTextProperty());
        checkNotNull(portTextField).setText(Integer.toString(MultiCoinChartsService.savedPort()));
        portTextField.textProperty().addListener((observable, oldText, newText) -> portChanged(newText));
        checkNotNull(autoStartCheckBox).setSelected(MultiCoinChartsService.isAutoStart());
        service.stateProperty().addListener((observable, oldState, newState) -> updateControls());
        updateControls();
    }

    void portChanged(String text) {
        int port = MultiCoinChartsService.parsePort(text);
        if (port > 0) {
            MultiCoinChartsService.savePort(port);
        }
        updateControls();
    }

    public void autoStartChanged() {
        MultiCoinChartsService.saveAutoStart(checkNotNull(autoStartCheckBox).isSelected());
    }

    public void start() {
        int port = MultiCoinChartsService.parsePort(checkNotNull(portTextField).getText());
        if (port > 0) {
            service.start(port);
        }
    }

    public void stop() {
        service.stop();
    }

    public void openInBrowser() {
        hostServices.showDocument(service.url());
    }

    void updateControls() {
        State state = service.getState();
        boolean stopped = state == State.STOPPED || state == State.FAILED;
        boolean validPort = MultiCoinChartsService.parsePort(checkNotNull(portTextField).getText()) > 0;

        portTextField.setDisable(!stopped);
        portTextField.setStyle(validPort ? "" : INVALID_FIELD_STYLE);
        checkNotNull(startButton).setDisable(!stopped || !validPort);
        checkNotNull(stopButton).setDisable(state != State.STARTING && state != State.RUNNING);
        checkNotNull(openInBrowserButton).setDisable(state != State.RUNNING);
        checkNotNull(statusLabel).setStyle(state == State.FAILED ? ERROR_STYLE : "");
    }
}
