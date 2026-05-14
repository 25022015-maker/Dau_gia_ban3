package com.example.uinew.Controller;
import com.example.uinew.service.SessionManager;
import com.example.uinew.Interface.OnEnter;
import com.example.uinew.Interface.ToLayOut;
import com.example.uinew.Interface.ToSignUp;

// ── Thêm để kết nối socket ────────────────────────────────────────────────
import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.LoginRequest;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.auction.project.Client.SocketClient;
import com.google.gson.Gson;
// ─────────────────────────────────────────────────────────────────────────

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

    // ── Thêm ─────────────────────────────────────────────────────────────────
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;
    private SocketClient socketClient;
    private final Gson gson = GsonFactory.create();
    // ─────────────────────────────────────────────────────────────────────────

    @FXML protected TextField txtUsername;
    @FXML protected PasswordField txtPassword;
    @FXML private Label lblError;

    // ── Giữ nguyên ───────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        txtUsername.setOnKeyTyped(e -> lblError.setVisible(false));
        txtPassword.setOnKeyTyped(e -> lblError.setVisible(false));
        connectToServer(); // Thêm: kết nối server ngầm khi màn hình mở
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
        System.out.println("Đã nhấn Enter ở ô Password!");
    }

    // ── CHỈ SỬA method này ───────────────────────────────────────────────────

    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("LOGIN CALLED");
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        // Nếu chưa kết nối server → fallback hardcode để test UI
        if (socketClient == null || !socketClient.isConnected()) {
            System.out.println("[WARN] Chưa kết nối server, dùng hardcode tạm");
            if (user.equals("admin") && pass.equals("123")) {
                goToMainLayout(event);
            } else {
                lblError.setVisible(true);
                txtPassword.requestFocus();
            }
            return;
        }

        // Đã kết nối → gửi lên server thật
        lblError.setVisible(false);

        socketClient.setOnResponse(response -> {
            if (response.getType() == ResponseType.LOGIN_SUCCESS) {
                // Lưu session để các màn hình sau dùng
                String username = response.getData() != null
                        ? response.getData().toString() : user;
                SessionManager.setCurrentUser(username);
                SessionManager.setSocketClient(socketClient);
                System.out.println("Đăng nhập thành công: " + username);

                // Chuyển màn hình phải nằm trong Platform.runLater
                // vì đang ở listenerThread, không phải JavaFX thread
                Platform.runLater(() -> goToMainLayout(event));

            } else if (response.getType() == ResponseType.LOGIN_FAILURE) {
                Platform.runLater(() -> {
                    lblError.setVisible(true);
                    txtPassword.requestFocus();
                });

            } else if (response.getType() == ResponseType.ERROR) {
                Platform.runLater(() -> {
                    lblError.setVisible(true);
                });
            }
        });

        // Gửi LoginRequest lên server dưới dạng JSON
        socketClient.send(new LoginRequest(user, pass));
    }

    // ── Giữ nguyên ───────────────────────────────────────────────────────────

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

    // ── Thêm: kết nối server trong thread riêng ──────────────────────────────

    private void connectToServer() {
        new Thread(() -> {
            try {
                socketClient = new SocketClient();
                socketClient.connect(SERVER_HOST, SERVER_PORT);
                System.out.println("[Socket] Đã kết nối server!");
            } catch (Exception e) {
                System.out.println("[WARN] Không kết nối server: " + e.getMessage());
                socketClient = null;
            }
        }, "ConnectThread").start();
    }
}