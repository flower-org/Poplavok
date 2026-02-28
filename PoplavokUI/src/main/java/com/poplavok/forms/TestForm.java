package com.poplavok.forms;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

public class TestForm extends AnchorPane {
    public TestForm() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TestForm.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
