package com.example.uinew.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class NotificationController {

    @FXML private VBox notifContainer;
    @FXML private Label lblUnread;

    private final List<NotifItem> notifications = new ArrayList<>();

    @FXML
    public void initialize() {
        // Dữ liệu mẫu — production lấy từ server
        notifications.add(new NotifItem("🏆", "Bạn đã thắng phiên đấu giá!", "Laptop Dell XPS 15 — 6,000,000 ₫", "2 phút trước", true));
        notifications.add(new NotifItem("💰", "Có người đặt giá cao hơn bạn!", "Rolex Submariner — giá mới: 55,000,000 ₫", "15 phút trước", true));
        notifications.add(new NotifItem("🔔", "Phiên đấu giá mới bắt đầu!", "Toyota Camry 2023 — Bắt đầu ngay!", "1 giờ trước", true));
        notifications.add(new NotifItem("✅", "Đặt giá thành công", "Laptop Dell XPS 15 — 5,500,000 ₫", "2 giờ trước", false));
        notifications.add(new NotifItem("⏰", "Phiên đấu giá sắp kết thúc!", "Rolex Submariner — còn 5 phút", "3 giờ trước", false));
        notifications.add(new NotifItem("❌", "Bạn đã thua phiên đấu giá", "Iphone 15 Pro Max", "Hôm qua", false));

        renderNotifications();
        updateUnreadCount();
    }

    private void renderNotifications() {
        notifContainer.getChildren().clear();
        for (NotifItem item : notifications) {
            notifContainer.getChildren().add(buildNotifCard(item));
        }
    }

    private HBox buildNotifCard(NotifItem item) {
        // Icon
        Label icon = new Label(item.icon);
        icon.setStyle("-fx-font-size: 24px;");
        icon.setPrefWidth(50);

        // Content
        Label title = new Label(item.title);
        title.setStyle("-fx-text-fill: " + (item.unread ? "#f8f0ff" : "#7f8c9a") +
                "; -fx-font-size: 14px; -fx-font-weight: " + (item.unread ? "bold" : "normal") + ";");

        Label subtitle = new Label(item.subtitle);
        subtitle.setStyle("-fx-text-fill: #7f8c9a; -fx-font-size: 12px;");

        VBox content = new VBox(4, title, subtitle);
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);

        // Time
        Label time = new Label(item.time);
        time.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");

        // Unread dot
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: #6c3483; -fx-font-size: 10px;");
        dot.setVisible(item.unread);

        HBox card = new HBox(12, icon, content, time, dot);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 20, 14, 20));
        card.setStyle("-fx-background-color: " + (item.unread ? "#16213e" : "#1a1a2e") +
                "; -fx-border-color: #2d2d4e; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: #1e2a45; -fx-border-color: #2d2d4e; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + (item.unread ? "#16213e" : "#1a1a2e") +
                        "; -fx-border-color: #2d2d4e; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"));

        // Click → mark as read
        card.setOnMouseClicked(e -> {
            item.unread = false;
            renderNotifications();
            updateUnreadCount();
        });

        return card;
    }

    @FXML
    public void markAllRead(ActionEvent event) {
        notifications.forEach(n -> n.unread = false);
        renderNotifications();
        updateUnreadCount();
    }

    private void updateUnreadCount() {
        long count = notifications.stream().filter(n -> n.unread).count();
        lblUnread.setText(count > 0 ? count + " chưa đọc" : "Tất cả đã đọc");
    }

    private static class NotifItem {
        String icon, title, subtitle, time;
        boolean unread;
        NotifItem(String icon, String title, String subtitle, String time, boolean unread) {
            this.icon = icon; this.title = title; this.subtitle = subtitle;
            this.time = time; this.unread = unread;
        }
    }
}