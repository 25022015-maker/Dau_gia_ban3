package com.auction.project.UI.Controller;

import com.auction.project.Client.NetworkClient;
import com.auction.project.Client.SocketClient;
import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.ResponseType;
import com.auction.project.UI.service.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManagementController {

    @FXML private VBox createForm;
    @FXML private VBox auctionList;
    @FXML private TextField txtItemName;
    @FXML private TextField txtStartPrice;
    @FXML private TextField txtDuration;
    @FXML private Label lblCreateMsg;
    @FXML private Label statTotal;
    @FXML private Label statRunning;
    @FXML private Label statFinished;

    private final Gson gson = GsonFactory.create();
    private List<Map<String, Object>> auctions = new ArrayList<>();

    @FXML
    public void initialize() {
        loadAuctions();
    }

    // ── Load danh sách phiên ──────────────────────────────────────────────────

    private void loadAuctions() {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null || !client.isConnected()) {
            showSampleData();
            return;
        }

        client.addResponseListener(response -> {
            if (response != null && response.getType() == ResponseType.AUCTION_LIST) {
                Platform.runLater(() -> {
                    try {
                        auctions = gson.fromJson(gson.toJson(response.getData()),
                                new TypeToken<List<Map<String, Object>>>(){}.getType());
                        if (auctions == null) auctions = new ArrayList<>();
                        renderAuctions();
                        updateStats();
                    } catch (Exception e) {
                        showSampleData();
                    }
                });
            }
        });

        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUCTIONS");
        client.sendRequest(gson.toJson(req));
    }

    private void showSampleData() {
        auctionList.getChildren().clear();
        Label lbl = new Label("⚠️ Chưa kết nối server — hiển thị dữ liệu mẫu");
        lbl.setStyle("-fx-text-fill: #f39c12; -fx-font-size: 13px;");
        auctionList.getChildren().add(lbl);
        if (statTotal != null) { statTotal.setText("0"); statRunning.setText("0"); statFinished.setText("0"); }
    }

    private void renderAuctions() {
        auctionList.getChildren().clear();
        for (Map<String, Object> auction : auctions) {
            auctionList.getChildren().add(buildAuctionRow(auction));
        }
    }

    private HBox buildAuctionRow(Map<String, Object> auction) {
        String id       = String.valueOf(((Number) auction.get("id")).intValue());
        String name     = String.valueOf(auction.getOrDefault("itemName", "Sản phẩm"));
        double price    = auction.get("currentPrice") instanceof Number
                ? ((Number) auction.get("currentPrice")).doubleValue() : 0;
        String status   = String.valueOf(auction.getOrDefault("status", "OPEN"));
        String bidder   = String.valueOf(auction.getOrDefault("leadingBidder", "—"));

        // Badge
        String badgeColor = switch (status) {
            case "RUNNING"  -> "#2ecc71";
            case "FINISHED" -> "#e74c3c";
            case "CANCELED" -> "#95a5a6";
            default         -> "#f39c12";
        };
        String badgeText = switch (status) {
            case "RUNNING"  -> "🟢 Đang diễn ra";
            case "FINISHED" -> "🔴 Kết thúc";
            case "CANCELED" -> "⚫ Đã hủy";
            default         -> "🟡 Chờ bắt đầu";
        };

        Label lblId     = new Label("#" + id);
        lblId.setStyle("-fx-text-fill: #555; -fx-font-size: 11px; -fx-pref-width: 40;");

        Label lblName   = new Label(name);
        lblName.setStyle("-fx-text-fill: #f8f0ff; -fx-font-size: 14px; -fx-font-weight: bold;");
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Label lblPrice  = new Label(String.format("%,.0f ₫", price));
        lblPrice.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 13px; -fx-pref-width: 130;");

        Label lblBidder = new Label("👤 " + bidder);
        lblBidder.setStyle("-fx-text-fill: #c39bd3; -fx-font-size: 12px; -fx-pref-width: 100;");

        Label lblStatus = new Label(badgeText);
        lblStatus.setStyle("-fx-text-fill: " + badgeColor + "; -fx-font-size: 12px; -fx-pref-width: 130;");

        // Nút hành động (chỉ Admin/Seller mới thấy ý nghĩa)
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;" +
                "-fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 4 10;");
        btnCancel.setVisible("RUNNING".equals(status) || "OPEN".equals(status));
        btnCancel.setOnAction(e -> handleCancel(id, btnCancel));

        HBox row = new HBox(10, lblId, lblName, lblPrice, lblBidder, lblStatus, btnCancel);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color: #16213e; -fx-background-radius: 8;" +
                "-fx-border-color: #2d2d4e; -fx-border-radius: 8; -fx-border-width: 1;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #1e2a45; -fx-background-radius: 8;" +
                "-fx-border-color: #6c3483; -fx-border-radius: 8; -fx-border-width: 1;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: #16213e; -fx-background-radius: 8;" +
                "-fx-border-color: #2d2d4e; -fx-border-radius: 8; -fx-border-width: 1;"));

        return row;
    }

    private void handleCancel(String auctionId, Button btn) {
        btn.setDisable(true);
        btn.setText("Đã hủy");
        // TODO: gửi CANCEL_AUCTION lên server
        System.out.println("[Management] Hủy phiên: " + auctionId);
    }

    private void updateStats() {
        long running  = auctions.stream().filter(a -> "RUNNING".equals(String.valueOf(a.get("status")))).count();
        long finished = auctions.stream().filter(a -> "FINISHED".equals(String.valueOf(a.get("status")))).count();
        if (statTotal != null)    statTotal.setText(String.valueOf(auctions.size()));
        if (statRunning != null)  statRunning.setText(String.valueOf(running));
        if (statFinished != null) statFinished.setText(String.valueOf(finished));
    }

    // ── Create form ───────────────────────────────────────────────────────────

    @FXML
    public void showCreateForm(ActionEvent event) {
        createForm.setVisible(true);
        createForm.setManaged(true);
        txtItemName.requestFocus();
    }

    @FXML
    public void hideCreateForm(ActionEvent event) {
        createForm.setVisible(false);
        createForm.setManaged(false);
        txtItemName.clear(); txtStartPrice.clear(); txtDuration.clear();
        lblCreateMsg.setVisible(false); lblCreateMsg.setManaged(false);
    }

    @FXML
    public void handleCreate(ActionEvent event) {
        String name  = txtItemName.getText().trim();
        String price = txtStartPrice.getText().trim().replace(",", "");
        String hours = txtDuration.getText().trim();

        if (name.isEmpty() || price.isEmpty() || hours.isEmpty()) {
            showCreateMsg("⚠️ Vui lòng điền đầy đủ thông tin.", false);
            return;
        }
        try {
            double startPrice = Double.parseDouble(price);
            int duration      = Integer.parseInt(hours);
            if (startPrice <= 0 || duration <= 0) throw new NumberFormatException();

            // TODO: gửi CREATE_AUCTION lên server
            System.out.println("[Management] Tạo phiên: " + name + " | " + startPrice + " | " + duration + "h");
            showCreateMsg("✅ Đã tạo phiên đấu giá: " + name, true);
            txtItemName.clear(); txtStartPrice.clear(); txtDuration.clear();

        } catch (NumberFormatException e) {
            showCreateMsg("❌ Giá và thời gian phải là số hợp lệ.", false);
        }
    }

    private void showCreateMsg(String msg, boolean ok) {
        lblCreateMsg.setText(msg);
        lblCreateMsg.setStyle(ok ? "-fx-text-fill: #2ecc71; -fx-font-size: 13px;"
                : "-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        lblCreateMsg.setVisible(true);
        lblCreateMsg.setManaged(true);
    }
}