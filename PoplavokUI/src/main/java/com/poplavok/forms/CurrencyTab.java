package com.poplavok.forms;

import com.flower.fxutils.ModalWindow;
import com.flower.fxutils.ProgressForm;
import com.google.common.base.Preconditions;
import com.flower.fxutils.Refreshable;
import com.poplavok.data.model.Currency;
import com.poplavok.data.utils.DBUtil;
import com.poplavok.data.dao.CurrencyDAO;
import com.poplavok.data.utils.HibernateUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Predicate;

public class CurrencyTab extends AnchorPane implements Refreshable {
    final static Logger LOGGER = LoggerFactory.getLogger(CurrencyTab.class);

    //final MainApp mainApp;

    @Nullable FilteredList<Currency> currencies;

    @FXML
    @Nullable
    TableView<Currency> currenciesTable;

    @FXML
    @Nullable
    TextField filterTextField;

    public CurrencyTab(/*MainApp mainApp*/) {
        //this.mainApp = mainApp;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("CurrencyTab.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        refreshContent();
    }

    @Override
    public void refreshContent() {
        try {
            ObservableList<Currency> masterCurrencies = FXCollections.observableList(DBUtil.connectGetResultAndClose(sf -> CurrencyDAO.findAll(sf)));
            currencies = new FilteredList<>(masterCurrencies);
            SortedList<Currency> sortableCurrencies = new SortedList<>(currencies);

            currencies.setPredicate(createFilterPredicate());

            Preconditions.checkNotNull(currenciesTable).itemsProperty().set(sortableCurrencies);
            sortableCurrencies.comparatorProperty().bind(currenciesTable.comparatorProperty());
            currenciesTable.refresh();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    public void retrieveFromExchange(ActionEvent event) {
        try {
            ModalWindow.showModal(event,
                    stage -> new ProgressForm(stage, this, KucoinTool::retrieveCurrencyFromExchange),
                    "Retrieve (from exchange)",
                    StageStyle.UNDECORATED);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    public void retrieveExtendedData(ActionEvent event) {
        try {
            ModalWindow.showModal(event,
                    stage -> new ProgressForm(stage, this, KucoinTool::retrieveCurrencyDetailsFromExchange),
                    "Retrieve extended data",
                    StageStyle.UNDECORATED);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    public void showCurrencyDetails() {
        try {
            Currency currency = Preconditions.checkNotNull(currenciesTable).getSelectionModel().getSelectedItem();
            //mainApp.createCurrencyDetailTab(currency);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    public void mouseClick(MouseEvent mouseEvent) {
        if(mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if(mouseEvent.getClickCount() == 2) {
                showCurrencyDetails();
            }
        }
    }

    private Predicate<Currency> createFilterPredicate() {
        String searchText = Preconditions.checkNotNull(filterTextField).textProperty().get();
        return currency -> {
            if (searchText == null || searchText.isEmpty()) return true;
            return (currency.getCurrency().toLowerCase().contains(searchText.toLowerCase().trim()) ||
                    (currency.getFullName() != null && currency.getFullName().toLowerCase().contains(searchText.toLowerCase().trim()))
            );
        };
    }

    public void filterTableView() {
        if (currencies != null) {
            currencies.setPredicate(createFilterPredicate());
        }
    }
}
