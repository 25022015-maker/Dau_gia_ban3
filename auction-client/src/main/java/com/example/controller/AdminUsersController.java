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

public class AdminUsersController {

    @FXML private TableView<JsonObject> tableUsers;

    private final ObservableList<JsonObject> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadUsers();
    }

    private void setupTable() {
        if (tableUsers == null) return;

        TableColumn<JsonObject, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue(), "id")));
        colId.setPrefWidth(60);

        TableColumn<JsonObject, String> colName = new TableColumn<>("Tên đăng nhập");
        colName.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue(), "username")));
        colName.setPrefWidth(150);

        TableColumn<JsonObject, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue(), "email")));
        colEmail.setPrefWidth(180);

        TableColumn<JsonObject, String> colBalance = new TableColumn<>("Số dư");
        colBalance.setCellValueFactory(c -> new SimpleStringProperty(fmt(c.getValue(), "balance")));
        colBalance.setPrefWidth(120);

        TableColumn<JsonObject, String> colRole = new TableColumn<>("Vai trò");
        colRole.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue(), "role")));
        colRole.setPrefWidth(100);

        TableColumn<JsonObject, String> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(str(c.getValue(), "status")));
        colStatus.setPrefWidth(100);

        TableColumn<JsonObject, String> colAction = new TableColumn<>("Thao tác");
        colAction.setPrefWidth(130);
        colAction.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button();
            {
                btn.setOnAction(e -> {
                    JsonObject row = getTableView().getItems().get(getIndex());
                    long id = row.get("id").getAsLong();
                    String cur = str(row, "status");
                    String next = "ACTIVE".equals(cur) ? "BANNED" : "ACTIVE";
                    toggleStatus(id, next);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                JsonObject row = getTableView().getItems().get(getIndex());
                String s = str(row, "status");
                btn.setText("ACTIVE".equals(s) ? "Khóa" : "Mở khóa");
                btn.setStyle("ACTIVE".equals(s)
                        ? "-fx-background-color: #c0392b; -fx-text-fill: white;"
                        : "-fx-background-color: #27ae60; -fx-text-fill: white;");
                setGraphic(btn);
            }
        });

        tableUsers.getColumns().setAll(colId, colName, colEmail, colBalance, colRole, colStatus, colAction);
        tableUsers.setItems(data);
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                JsonArray arr = ApiClient.getAllUsers();
                Platform.runLater(() -> {
                    data.clear();
                    for (JsonElement el : arr) data.add(el.getAsJsonObject());
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.ERROR, "Lỗi: " + e.getMessage()).show());
            }
        }, "AdminUsersThread").start();
    }

    private void toggleStatus(long userId, String newStatus) {
        new Thread(() -> {
            try {
                ApiClient.updateUserStatus(userId, newStatus);
                Platform.runLater(this::loadUsers);
            } catch (Exception e) {
                Platform.runLater(() ->
                        new Alert(Alert.AlertType.ERROR, e.getMessage()).show());
            }
        }).start();
    }

    @FXML public void handleRefresh() { loadUsers(); }

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