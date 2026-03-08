package com.poplavok.forms;

import com.flower.fxutils.ModalWindow;
import com.flower.fxutils.ProgressForm;
import com.google.common.base.Preconditions;
import com.flower.fxutils.Refreshable;
import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.utils.DBUtil;
import com.poplavok.data.dao.MarketTickerDAO;

import com.poplavok.kucoin.KucoinTool;
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
import javafx.scene.layout.AnchorPane;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Predicate;

public class MarketTickerTab extends AnchorPane implements Refreshable {
    final static Logger LOGGER = LoggerFactory.getLogger(MarketTickerTab.class);

    //final MainApp mainApp;

    @Nullable FilteredList<MarketTicker> marketTickers;

    @FXML
    @Nullable
    TableView<MarketTicker> marketTickersTable;

    @FXML
    @Nullable
    TextField filterTextField;

    public MarketTickerTab(/*MainApp mainApp*/) {
        //this.mainApp = mainApp;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MarketTicker_JlucT.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        refreshContent();
    }

    public void refreshContent() {
        try {
            ObservableList<MarketTicker> masterMarketTickers = FXCollections.observableList(DBUtil.connectGetResultAndClose(sf -> MarketTickerDAO.findAll(sf)));
            marketTickers = new FilteredList<>(masterMarketTickers);
            SortedList<MarketTicker> sortableMarketTickers = new SortedList<>(marketTickers);

            marketTickers.setPredicate(createFilterPredicate());
            Preconditions.checkNotNull(marketTickersTable).itemsProperty().set(sortableMarketTickers);
            sortableMarketTickers.comparatorProperty().bind(marketTickersTable.comparatorProperty());
            marketTickersTable.refresh();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    public void retrieveFromExchange(ActionEvent event) {
        try {
            ModalWindow.showModal(event,
                    stage -> new ProgressForm(stage, this, KucoinTool::retrieveMarketTickersFromExchange),
                    "Retrieve extended data",
                    StageStyle.UNDECORATED);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    private Predicate<MarketTicker> createFilterPredicate() {
        String searchText = Preconditions.checkNotNull(filterTextField).textProperty().get();
        return marketTicker -> {
            if (searchText == null || searchText.isEmpty()) return true;
            return (marketTicker.getSymbol().toLowerCase().contains(searchText.toLowerCase().trim()));
        };
    }

    public void filterTableView() {
        if (marketTickers != null) {
            marketTickers.setPredicate(createFilterPredicate());
        }
    }

    public void longCalc() {
        try {
            MarketTicker marketTicker = Preconditions.checkNotNull(marketTickersTable).getSelectionModel().getSelectedItem();
            //mainApp.createLongCalcTab(marketTicker);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }

    public void shortCalc() {
        try {
            MarketTicker marketTicker = Preconditions.checkNotNull(marketTickersTable).getSelectionModel().getSelectedItem();
            //mainApp.createShortCalcTab(marketTicker);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error: " + e, ButtonType.OK);
            LOGGER.error("Error:", e);
            alert.showAndWait();
        }
    }
}
