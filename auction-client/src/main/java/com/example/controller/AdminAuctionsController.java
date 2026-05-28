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
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

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
                new SimpleStringProperty(statusLabel(str(c.getValue(), "status"))));

        if (colAction != null) {
            colAction.setCellFactory(col -> new TableCell<>() {
                final Button btnApprove = new Button("✔ Duyệt");
                final Button btnEnter   = new Button("▶ Vào phòng");
                final Button btnCancel  = new Button("✖ Hủy");
                final HBox box = new HBox(6, btnApprove, btnEnter, btnCancel);
                {
                    btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11; -fx-cursor: hand;");
                    btnEnter  .setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-size: 11; -fx-cursor: hand;");
                    btnCancel .setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11; -fx-cursor: hand;");

                    btnApprove.setOnAction(e -> {
                        JsonObject row = getTableView().getItems().get(getIndex());
                        approveAuction(row.get("id").getAsLong());
                    });
                    btnEnter.setOnAction(e -> {
                        JsonObject row = getTableView().getItems().get(getIndex());
                        openAuctionRoom(row.get("id").getAsLong());
                    });
                    btnCancel.setOnAction(e -> {
                        JsonObject row = getTableView().getItems().get(getIndex());
                        cancelAuction(row.get("id").getAsLong());
                    });
                }

                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) { setGraphic(null); return; }
                    JsonObject row = getTableView().getItems().get(getIndex());
                    String status = str(row, "status");
                    btnApprove.setVisible("WAITING_APPROVAL".equals(status));
                    btnApprove.setManaged("WAITING_APPROVAL".equals(status));
                    btnCancel.setVisible("WAITING_APPROVAL".equals(status)
                            || "PENDING".equals(status) || "RUNNING".equals(status));
                    btnCancel.setManaged(btnCancel.isVisible());
                    btnEnter.setVisible(!"WAITING_APPROVAL".equals(status));
                    btnEnter.setManaged(btnEnter.isVisible());
                    setGraphic(box);
                }
            });
        }

        if (tableAuctions != null) tableAuctions.setItems(data);
    }

    private void loadAuctions() {
        new Thread(() -> {
            try {
                JsonArray arr = ApiClient.getAdminAuctions();
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

    private void approveAuction(long id) {
        new Thread(() -> {
            try {
                ApiClient.approveAuction(id);
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.INFORMATION, "Đã duyệt phiên #" + id).show();
                    loadAuctions();
                });
            } catch (Exception e) {
                Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, e.getMessage()).show());
            }
        }).start();
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

    private void openAuctionRoom(long auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/user/AuctionRoom.fxml"));
            Node view = loader.load();
            AuctionRoomController ctrl = loader.getController();
            ctrl.setAuctionId(auctionId);
            MainLayoutController main = MainLayoutController.getInstance();
            if (main != null) main.setContent(view);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Lỗi mở phòng: " + e.getMessage()).show();
        }
    }

    @FXML public void handleRefresh() { loadAuctions(); }

    private String statusLabel(String s) {
        return switch (s) {
            case "WAITING_APPROVAL" -> "⏳ Chờ duyệt";
            case "PENDING"          -> "🕐 Sắp diễn ra";
            case "RUNNING"          -> "🔴 Đang diễn ra";
            case "FINISHED"         -> "✅ Đã kết thúc";
            case "CANCELED"         -> "❌ Đã hủy";
            default -> s;
        };
    }

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