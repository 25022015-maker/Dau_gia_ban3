package com.example.uinew.Controller;

import com.example.uinew.service.SessionManager;
import com.example.uinew.Interface.OnEnter;
import com.example.uinew.Interface.ToLayOut;
import com.example.uinew.Interface.ToSignUp;
import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.LoginRequest;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.auction.project.Client.SocketClient;
import com.google.gson.Gson;
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

public class LoginController extends MainController implements OnEnter, ToSignUp, ToLayOut {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;
    private SocketClient socketClient;
    private final Gson gson = GsonFactory.create();

    @FXML protected TextField txtUsername;
    @FXML protected PasswordField txtPassword;
    @FXML private Label lblError;

    @FXML
    public void initialize() {
        txtUsername.setOnKeyTyped(e -> lblError.setVisible(false));
        txtPassword.setOnKeyTyped(e -> lblError.setVisible(false));
        connectToServer();
    }

    @FXML
    public void goToSignUp(ActionEvent event) {
        changeScene(event, "/com/example/uinew/SignupUI.fxml", "Đăng ký");
    }

    @FXML
    public void onUsernameEnter(ActionEvent event) {
        txtPassword.requestFocus();
    }

    @FXML
    public void onPasswordEnter(ActionEvent event) {
        handleLogin(event);
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("LOGIN CALLED");
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        // Fallback nếu chưa kết nối server
        if (socketClient == null || !socketClient.isConnected()) {
            System.out.println("[WARN] Chưa kết nối server, dùng hardcode tạm");
            if (user.equals("admin") && pass.equals("123")) {
                SessionManager.setCurrentUser("admin");
                goToMainLayout(event);
            } else {
                lblError.setVisible(true);
                txtPassword.requestFocus();
            }
            return;
        }

        lblError.setVisible(false);

        socketClient.setOnResponse(response -> {
            if (response.getType() == ResponseType.LOGIN_SUCCESS) {
                String username = response.getData() != null
                        ? response.getData().toString() : user;
                SessionManager.setCurrentUser(username);
                System.out.println("Đăng nhập thành công: " + username);

                // FIX: dùng txtUsername.getScene().getWindow() thay vì event.getSource()
                // vì event không còn valid khi chạy trong Platform.runLater lambda
                Platform.runLater(() -> {
                    try {
                        Parent root = FXMLLoader.load(
                                getClass().getResource("/com/example/uinew/MainLayout.fxml"));
                        Stage stage = (Stage) txtUsername.getScene().getWindow();
                        stage.setScene(new Scene(root));
                        stage.setTitle("Dashboard");
                        stage.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            } else if (response.getType() == ResponseType.LOGIN_FAILURE) {
                Platform.runLater(() -> {
                    lblError.setVisible(true);
                    txtPassword.requestFocus();
                });
            } else if (response.getType() == ResponseType.ERROR) {
                Platform.runLater(() -> lblError.setVisible(true));
            }
        });

        socketClient.send(new LoginRequest(user, pass));
    }

    @FXML
    public void goToMainLayout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/uinew/MainLayout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socketClient = new SocketClient();
                socketClient.connect(SERVER_HOST, SERVER_PORT);
                SessionManager.setSocketClient(socketClient);
                System.out.println("[Socket] Đã kết nối và lưu SessionManager!");
            } catch (Exception e) {
                System.out.println("[WARN] Không kết nối server: " + e.getMessage());
                socketClient = null;
            }
        }, "ConnectThread").start();
    }
}