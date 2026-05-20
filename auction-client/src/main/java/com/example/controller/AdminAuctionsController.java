package com.example.controller;

import com.example.service.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AdminAuctionsController {

    @FXML private TableView<JsonObject>          tableAuctions;
    @FXML private TableColumn<JsonObject,String> colId;
    @FXML private TableColumn<JsonObject,String> colName;
    @FXML private TableColumn<JsonObject,String> colUser;
    @FXML private TableColumn<JsonObject,String> colPrice;
    @FXML private TableColumn<JsonObject,String> colStatus;
    @FXML private TableColumn<JsonObject,String> colAction;

    private final ObservableList<JsonObject> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadAuctions();
    }

    private void setupColumns() {
        if (colId     != null) colId.setCellValueFactory(c ->
                new SimpleStringProperty(str(c.getValue(), "id")));
        if (colName   != null) colName.setCellValueFactory(c ->
                new SimpleStringProperty(str(c.getValue(), "itemName")));
        if (colUser   != null) colUser.setCellValueFactory(c ->
                new SimpleStringProperty(str(c.getValue(), "sellerUsername")));
        if (colPrice  != null) colPrice.setCellValueFactory(c ->
                new SimpleStringProperty(fmt(c.getValue(), "currentPrice")));
        if (colStatus != null) colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(str(c.getValue(), "status")));

        // Nút hủy trong cột Action
        if (colAction != null) {
            colAction.setCellFactory(col -> new TableCell<>() {
                final Button btn = new Button("Hủy phiên");
                {
                    btn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    btn.setOnAction(e -> {
                        JsonObject row = getTableView().getItems().get(getIndex());
                        long id = row.get("id").getAsLong();
                        cancelAuction(id);
                    });
                }
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }

        if (tableAuctions != null) tableAuctions.setItems(data);
    }

    private void loadAuctions() {
        new Thread(() -> {
            try {
                JsonArray arr = ApiClient.getAuctions();
                Platform.runLater(() -> {
                    data.clear();
                    for (JsonElement el : arr) data.add(el.getAsJsonObject());
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu: " + e.getMessage()).show());
            }
        }, "AdminLoadAuctionsThread").start();
    }

    private void cancelAuction(long id) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hủy phiên đấu giá #" + id + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        ApiClient.adminCancelAuction(id);
                        Platform.runLater(this::loadAuctions);
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                new Alert(Alert.AlertType.ERROR, e.getMessage()).show());
                    }
                }).start();
            }
        });
    }

    @FXML public void handleRefresh() { loadAuctions(); }

    private String str(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return (el == null || el.isJsonNull()) ? "" : el.getAsString();
    }

    private String fmt(JsonObject o, String key) {
        JsonElement el = o.get(key);
        if (el == null || el.isJsonNull()) return "0 ₫";
        return String.format("%,d ₫", el.getAsLong()).replace(",", ".");
    }
}