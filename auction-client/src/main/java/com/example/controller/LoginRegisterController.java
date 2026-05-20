package com.example.controller;

import com.example.service.ApiClient;
import com.example.service.SessionManager;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginRegisterController {

    @FXML private TextField     txtUsername;
    @FXML private TextField     txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Label         lblUsernameMessage;
    @FXML private Label         lblEmailMessage;
    @FXML private Label         lblPasswordMessage;
    @FXML private Label         lblLoginRegisterMessage;

    @FXML
    public void initialize() {
        hideAllMessages();
        if (cmbRole != null) {
            cmbRole.getItems().addAll("BIDDER", "SELLER");
            cmbRole.setValue("BIDDER");
        }
    }

    // ── Đăng nhập ─────────────────────────────────────────────────────────────

    @FXML
    public void handleLogin() {
        hideAllMessages();
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText();

        if (user.isEmpty()) { lblUsernameMessage.setText("Vui lòng nhập tài khoản"); return; }
        if (pass.isEmpty()) { lblPasswordMessage.setText("Vui lòng nhập mật khẩu");  return; }

        runAsync(() -> {
            try {
                JsonObject res = ApiClient.login(user, pass);
                String token    = res.get("token").getAsString();
                long   userId   = res.get("userId").getAsLong();
                String username = res.get("username").getAsString();
                String role     = res.get("role").getAsString();
                SessionManager.setSession(token, userId, username, role, 0);
                Platform.runLater(this::goToDashboard);
            } catch (Exception e) {
                Platform.runLater(() ->
                        lblLoginRegisterMessage.setText("Đăng nhập thất bại: " + e.getMessage()));
            }
        });
    }

    // ── Đăng ký ───────────────────────────────────────────────────────────────

    @FXML
    public void handleRegister() {
        hideAllMessages();
        String user  = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String pass  = txtPassword.getText();

        boolean ok = true;
        if (user.isEmpty())          { lblUsernameMessage.setText("Vui lòng nhập tài khoản"); ok = false; }
        if (email.isEmpty())         { lblEmailMessage.setText("Vui lòng nhập email");          ok = false; }
        if (pass.length() < 6)      { lblPasswordMessage.setText("Mật khẩu tối thiểu 6 ký tự"); ok = false; }
        if (!ok) return;

        String role = (cmbRole != null && cmbRole.getValue() != null) ? cmbRole.getValue() : "BIDDER";

        runAsync(() -> {
            try {
                JsonObject res = ApiClient.register(user, email, pass, role);
                String token    = res.get("token").getAsString();
                long   userId   = res.get("userId").getAsLong();
                String username = res.get("username").getAsString();
                String respRole = res.get("role").getAsString();
                SessionManager.setSession(token, userId, username, respRole, 0);
                Platform.runLater(this::goToDashboard);
            } catch (Exception e) {
                Platform.runLater(() ->
                        lblLoginRegisterMessage.setText("Đăng ký thất bại: " + e.getMessage()));
            }
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void goToDashboard() {
        try {
            String fxml = SessionManager.isAdmin()
                    ? "/com/example/admin/AdminLayout.fxml"
                    : "/com/example/user/MainLayout.fxml";
            Scene scene = new Scene(FXMLLoader.load(getClass().getResource(fxml)));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.setTitle("AUCTION SYSTEM — " + SessionManager.getUsername());
        } catch (Exception e) {
            lblLoginRegisterMessage.setText("Lỗi mở giao diện: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void hideAllMessages() {
        lblUsernameMessage.setText("");
        lblEmailMessage.setText("");
        lblPasswordMessage.setText("");
        lblLoginRegisterMessage.setText("");
    }

    private void runAsync(Runnable task) {
        new Thread(task, "ApiThread").start();
    }
}