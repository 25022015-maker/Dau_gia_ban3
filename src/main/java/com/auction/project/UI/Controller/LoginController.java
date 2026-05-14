package com.auction.project.UI.Controller;
import com.auction.project.Client.NetworkClient;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;

import com.auction.project.UI.Controller.MainController;
import com.auction.project.UI.Interface.OnEnter;
import com.auction.project.UI.Interface.ToLayOut;
import com.auction.project.UI.Interface.ToSignUp;
import com.auction.project.UI.service.SessionManager;
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

    // Sử dụng NetworkClient thay vì SocketClient thô
    private NetworkClient networkClient;

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
        if (networkClient == null || !networkClient.isConnected()) {
            System.out.println("[WARN] Chưa kết nối server, dùng hardcode tạm");
            if (user.equals("admin") && pass.equals("123")) {
                SessionManager.setCurrentUser("admin");
                goToMainLayout(event);
            } else {
                lblError.setText("Chưa kết nối server!");
                lblError.setVisible(true);
                txtPassword.requestFocus();
            }
            return;
        }

        lblError.setVisible(false);

        // Gửi yêu cầu đăng nhập cực kỳ gọn gàng qua NetworkClient
        networkClient.sendLogin(user, pass);
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
                networkClient = new NetworkClient();
                networkClient.connect(SERVER_HOST, SERVER_PORT);

                // Lưu NetworkClient vào SessionManager để các màn hình khác dùng chung
                SessionManager.setNetworkClient(networkClient);
                System.out.println("[Network] Đã kết nối và lưu SessionManager!");

                // Đăng ký lắng nghe phản hồi từ Server
                networkClient.addResponseListener(this::handleServerResponse);

            } catch (Exception e) {
                System.out.println("[WARN] Không kết nối server: " + e.getMessage());
                networkClient = null;
            }
        }, "ConnectThread").start();
    }

    /**
     * Xử lý phản hồi từ Server.
     * Chú ý: Phải dùng Platform.runLater() vì luồng mạng không được phép cập nhật UI.
     */
    private void handleServerResponse(Response response) {
        if (response == null) return;

        if (response.getType() == ResponseType.LOGIN_SUCCESS) {
            String username = response.getData() != null ? response.getData().toString() : txtUsername.getText();
            SessionManager.setCurrentUser(username);
            System.out.println("Đăng nhập thành công: " + username);

            Platform.runLater(() -> {
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/com/example/uinew/MainLayout.fxml"));
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
                lblError.setText(response.getMessage());
                lblError.setVisible(true);
                txtPassword.requestFocus();
            });
        } else if (response.getType() == ResponseType.ERROR) {
            Platform.runLater(() -> {
                lblError.setText("Lỗi máy chủ: " + response.getMessage());
                lblError.setVisible(true);
            });
        }
    }
}