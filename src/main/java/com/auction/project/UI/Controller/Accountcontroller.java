package com.auction.project.UI.Controller;

import com.auction.project.Client.NetworkClient;
import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.ResponseType;
import com.auction.project.UI.Interface.ViewCleanup;
import com.auction.project.UI.service.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public class Accountcontroller implements ViewCleanup {

    @FXML private Button btnTabInfo;
    @FXML private Button btnTabHistory;
    @FXML private Button btnTabPassword;

    @FXML private VBox tabInfo;
    @FXML private VBox tabHistory;
    @FXML private VBox tabPassword;

    @FXML private Label lblAvatar;
    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblEmail;
    @FXML private Label lblId;

    @FXML private ListView<String> historyList;

    @FXML private PasswordField txtCurrentPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;
    @FXML private Label lblPassMessage;

    private final Gson gson = GsonFactory.create();

    // Biến lưu Listener
    private NetworkClient.ResponseListener serverListener;

    @FXML
    public void initialize() {
        loadUserInfo();
        loadBidHistory();
    }

    private void loadUserInfo() {
        String username = SessionManager.getCurrentUser();
        if (username == null) return;

        lblAvatar.setText(username.substring(0, 1).toUpperCase());
        lblUsername.setText(username);
        lblEmail.setText(username + "@auction.com");
        lblId.setText("#" + Math.abs(username.hashCode() % 10000));

        if (username.equals("admin")) {
            lblRole.setText("Admin");
            lblRole.setStyle("-fx-text-fill: #e74c3c; -fx-background-color: #2d1b4e; " +
                    "-fx-padding: 3 10; -fx-background-radius: 10;");
        } else if (username.equals("seller1")) {
            lblRole.setText("Seller");
            lblRole.setStyle("-fx-text-fill: #f39c12; -fx-background-color: #2d1b4e; " +
                    "-fx-padding: 3 10; -fx-background-radius: 10;");
        } else {
            lblRole.setText("Bidder");
        }
    }

    private void loadBidHistory() {
        historyList.getItems().clear();

        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null || !client.isConnected()) {
            historyList.getItems().add("⚠️ Chưa kết nối server");
            return;
        }

        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_BID_HISTORY");
        req.addProperty("username", SessionManager.getCurrentUser());
        client.sendRequest(gson.toJson(req));

        // Gán lambda vào biến
        serverListener = response -> {
            if (response == null) return;
            if (response.getType() == ResponseType.AUCTION_LIST) {
                Platform.runLater(() -> {
                    historyList.getItems().clear();
                    historyList.getItems().add("📋 Dữ liệu lịch sử sẽ hiện ở đây");
                });
            }
        };

        // Đăng ký listener
        client.addResponseListener(serverListener);

        historyList.getItems().addAll(
                "🔴 Laptop Dell XPS 15 — Đặt giá: 6,000,000 ₫ — Đang diễn ra",
                "✅ Rolex Submariner — Đặt giá: 55,000,000 ₫ — Đã thắng",
                "❌ Toyota Camry 2023 — Đặt giá: 850,000,000 ₫ — Thua"
        );
    }

    @FXML
    public void showTabInfo(ActionEvent event) { setActiveTab(0); }

    @FXML
    public void showTabHistory(ActionEvent event) { setActiveTab(1); }

    @FXML
    public void showTabPassword(ActionEvent event) { setActiveTab(2); }

    private void setActiveTab(int index) {
        String active = "-fx-background-color: #6c3483; -fx-text-fill: white; " +
                "-fx-font-size: 13px; -fx-background-radius: 0;";
        String normal = "-fx-background-color: #2d1b4e; -fx-text-fill: #c39bd3; " +
                "-fx-font-size: 13px; -fx-background-radius: 0;";

        btnTabInfo.setStyle(index == 0 ? active : normal);
        btnTabHistory.setStyle(index == 1 ? active : normal);
        btnTabPassword.setStyle(index == 2 ? active : normal);

        tabInfo.setVisible(index == 0);     tabInfo.setManaged(index == 0);
        tabHistory.setVisible(index == 1);  tabHistory.setManaged(index == 1);
        tabPassword.setVisible(index == 2); tabPassword.setManaged(index == 2);
    }

    @FXML
    public void handleChangePassword(ActionEvent event) {
        String current = txtCurrentPass.getText();
        String newPass  = txtNewPass.getText();
        String confirm  = txtConfirmPass.getText();

        if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
            showPassMessage("⚠️ Vui lòng điền đầy đủ thông tin.", false);
            return;
        }
        if (!newPass.equals(confirm)) {
            showPassMessage("❌ Mật khẩu mới không khớp.", false);
            return;
        }
        if (newPass.length() < 6) {
            showPassMessage("❌ Mật khẩu mới phải có ít nhất 6 ký tự.", false);
            return;
        }

        showPassMessage("✅ Đổi mật khẩu thành công!", true);
        txtCurrentPass.clear();
        txtNewPass.clear();
        txtConfirmPass.clear();
    }

    private void showPassMessage(String message, boolean success) {
        lblPassMessage.setText(message);
        lblPassMessage.setStyle(success
                ? "-fx-text-fill: #2ecc71; -fx-font-size: 13px;"
                : "-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        lblPassMessage.setVisible(true);
        lblPassMessage.setManaged(true);
    }

    // ── Dọn dẹp Listener khi rời khỏi trang ──────────────────────────────────
    @Override
    public void cleanup() {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client != null && serverListener != null) {
            client.removeResponseListener(serverListener);
            System.out.println("[Account] Đã gỡ Listener.");
        }
    }
}