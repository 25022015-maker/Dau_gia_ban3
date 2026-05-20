package com.example.controller;

import com.example.service.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Controller dùng chung cho MainLayout (user) và AdminLayout (admin).
 * Quản lý sidebar navigation và content area.
 */
public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private Label     lblSidebarNotification;

    // Singleton để các controller con có thể gọi loadView()
    private static MainLayoutController instance;

    @FXML
    public void initialize() {
        instance = this;
        if (SessionManager.isAdmin()) {
            loadView("/com/example/admin/AdminAuctions.fxml");
        } else {
            loadView("/com/example/user/Dashboard.fxml");
        }
    }

    public static MainLayoutController getInstance() { return instance; }

    // ── Sidebar buttons ───────────────────────────────────────────────────────

    @FXML
    public void onAccountClick() {
        loadView("/com/example/user/Account.fxml");
    }

    @FXML
    public void onDashboardClick() {
        if (SessionManager.isAdmin()) {
            loadView("/com/example/admin/AdminUsers.fxml");
        } else {
            loadView("/com/example/user/Dashboard.fxml");
        }
    }

    @FXML
    public void onCreateAuctionClick() {
        if (SessionManager.isAdmin()) {
            loadView("/com/example/admin/AdminAuctions.fxml");
        } else if (SessionManager.isSeller()) {
            loadView("/com/example/user/CreateAuction.fxml");
        } else {
            loadView("/com/example/user/Account.fxml");
        }
    }

    @FXML
    public void onLogoutClick() {
        SessionManager.clear();
        try {
            Scene scene = new Scene(FXMLLoader.load(
                    getClass().getResource("/com/example/login/LoginRegister.fxml")));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setMaximized(false);
            stage.setWidth(800);
            stage.setHeight(600);
            stage.setTitle("Hệ thống Đấu giá");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Public API cho các controller con ────────────────────────────────────

    public void loadView(String fxmlPath) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setContent(Node node) {
        contentArea.getChildren().setAll(node);
    }

    public void setNotification(String msg) {
        if (lblSidebarNotification != null) lblSidebarNotification.setText(msg);
    }
}