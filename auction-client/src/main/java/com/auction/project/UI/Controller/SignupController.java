package com.auction.project.UI.Controller;

import com.auction.project.Client.NetworkClient;
import com.auction.project.Packets.ResponseType;
import com.auction.project.UI.Interface.ToLayOut;
import com.auction.project.UI.Interface.ToLogin;
import com.auction.project.UI.Interface.OnEnter;
import com.auction.project.UI.service.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class SignupController extends MainController implements OnEnter, ToLogin, ToLayOut {

    @FXML protected TextField txtUsername;
    @FXML protected PasswordField txtPassword;
    @FXML private Label lblMessage;

    private NetworkClient networkClient;

    @FXML
    public void initialize() {
        networkClient = SessionManager.getNetworkClient();
        if (networkClient == null || !networkClient.isConnected()) {
            connectToServer();
        } else {
            networkClient.addResponseListener(this::handleServerResponse);
        }
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                networkClient = new NetworkClient();
                networkClient.connect("localhost", 9090);
                SessionManager.setNetworkClient(networkClient);
                networkClient.addResponseListener(this::handleServerResponse);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if(lblMessage != null) {
                        lblMessage.setText("Không thể kết nối đến máy chủ!");
                        lblMessage.setVisible(true);
                    }
                });
            }
        }).start();
    }

    @FXML
    public void onPasswordEnter(ActionEvent event) { handleSignup(event); }

    @FXML
    public void onUsernameEnter(ActionEvent event) { txtPassword.requestFocus(); }

    @FXML
    public void goToLogin(ActionEvent event) {
        changeScene(event, "/com/example/uinew/LoginUI.fxml", "Đăng nhập");
    }

    @FXML
    public void goToMainLayout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/uinew/MainLayout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSignup(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            if(lblMessage != null) {
                lblMessage.setText("Vui lòng nhập đầy đủ thông tin!");
                lblMessage.setVisible(true);
            }
            return;
        }

        if (networkClient == null || !networkClient.isConnected()) {
            if(lblMessage != null) {
                lblMessage.setText("Chưa kết nối server!");
                lblMessage.setVisible(true);
            }
            return;
        }

        JsonObject req = new JsonObject();
        req.addProperty("action", "REGISTER");
        req.addProperty("username", username);
        req.addProperty("password", password);

        networkClient.sendRequest(new Gson().toJson(req));
    }

    private void handleServerResponse(com.auction.project.Packets.Response response) {
        if (response == null) return;

        Platform.runLater(() -> {
            if (response.getType() == ResponseType.LOGIN_SUCCESS) {
                SessionManager.setCurrentUser(txtUsername.getText().trim());
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/com/example/uinew/MainLayout.fxml"));
                    Stage stage = (Stage) txtUsername.getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Dashboard");
                    stage.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (response.getType() == ResponseType.ERROR) {
                if(lblMessage != null) {
                    lblMessage.setText(response.getMessage());
                    lblMessage.setVisible(true);
                }
            }
        });
    }
}