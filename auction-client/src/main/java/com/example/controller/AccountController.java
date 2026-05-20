package com.example.controller;

import com.example.service.ApiClient;
import com.example.service.SessionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class AccountController {

    @FXML private Label    lblUsername;
    @FXML private Label    lblEmail;
    @FXML private Label    lblRole;
    @FXML private Label    lblBalance;
    @FXML private Label    lblStatus;
    @FXML private Label    lblCreatedAt;

    @FXML private TextField  txtTopUpAmount;
    @FXML private Label      lblTopUpMessage;

    @FXML private ListView<String> listNotifications;
    @FXML private Label           lblNotifCount;

    @FXML
    public void initialize() {
        loadProfile();
        loadNotifications();
    }

    private void loadProfile() {
        new Thread(() -> {
            try {
                JsonObject p = ApiClient.getProfile();
                Platform.runLater(() -> {
                    if (lblUsername != null) lblUsername.setText(getString(p, "username", ""));
                    if (lblEmail    != null) lblEmail.setText(getString(p, "email", ""));
                    if (lblRole     != null) lblRole.setText(getString(p, "role", ""));
                    long bal = p.has("balance") ? p.get("balance").getAsLong() : 0L;
                    SessionManager.setBalance(bal);
                    if (lblBalance  != null) lblBalance.setText(formatPrice(bal));
                    if (lblStatus   != null) lblStatus.setText(getString(p, "status", "ACTIVE"));
                    if (lblCreatedAt!= null) {
                        String ca = getString(p, "createdAt", "");
                        lblCreatedAt.setText(ca.length() > 10 ? ca.substring(0, 10) : ca);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblBalance != null) lblBalance.setText("Lỗi tải dữ liệu: " + e.getMessage());
                });
            }
        }, "ProfileThread").start();
    }

    @FXML
    public void handleTopUp() {
        if (lblTopUpMessage != null) lblTopUpMessage.setText("");
        String text = txtTopUpAmount.getText().trim().replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            if (lblTopUpMessage != null) lblTopUpMessage.setText("Nhập số tiền cần nạp");
            return;
        }
        long amount = Long.parseLong(text);
        if (amount <= 0) {
            if (lblTopUpMessage != null) lblTopUpMessage.setText("Số tiền phải lớn hơn 0");
            return;
        }
        new Thread(() -> {
            try {
                JsonObject res = ApiClient.topUp(amount);
                long newBal = res.has("balance") ? res.get("balance").getAsLong() : 0L;
                SessionManager.setBalance(newBal);
                Platform.runLater(() -> {
                    if (lblBalance != null) lblBalance.setText(formatPrice(newBal));
                    if (lblTopUpMessage != null) {
                        lblTopUpMessage.setText("✅ Nạp tiền thành công!");
                        lblTopUpMessage.setStyle("-fx-text-fill: #2ecc71;");
                    }
                    txtTopUpAmount.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblTopUpMessage != null) {
                        lblTopUpMessage.setText(e.getMessage());
                        lblTopUpMessage.setStyle("-fx-text-fill: #e74c3c;");
                    }
                });
            }
        }, "TopUpThread").start();
    }

    private void loadNotifications() {
        new Thread(() -> {
            try {
                JsonArray arr = ApiClient.getNotifications();
                Platform.runLater(() -> {
                    if (listNotifications != null) {
                        listNotifications.getItems().clear();
                        for (JsonElement el : arr) {
                            JsonObject n = el.getAsJsonObject();
                            String title   = getString(n, "title", "");
                            String content = getString(n, "content", "");
                            boolean read   = n.has("isRead") && n.get("isRead").getAsBoolean();
                            listNotifications.getItems().add((read ? "✅ " : "🔔 ") + title + ": " + content);
                        }
                    }
                    if (lblNotifCount != null) lblNotifCount.setText("Thông báo: " + arr.size());
                    markRead();
                });
            } catch (Exception ignored) {}
        }, "NotifThread").start();
    }

    private void markRead() {
        new Thread(() -> {
            try { ApiClient.markNotificationsRead(); } catch (Exception ignored) {}
        }).start();
    }

    private String formatPrice(long p) {
        return String.format("%,d ₫", p).replace(",", ".");
    }

    private String getString(JsonObject o, String key, String def) {
        JsonElement el = o.get(key);
        return (el == null || el.isJsonNull()) ? def : el.getAsString();
    }
}