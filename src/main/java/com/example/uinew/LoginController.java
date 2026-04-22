package com.example.uinew;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    // 1. Nhấn Enter ở ô Username -> Nhảy xuống ô Password
    @FXML
    void onUsernameEnter(ActionEvent event) {
        txtPassword.requestFocus();
    }

    // 2. Nhấn Enter ở ô Password hoặc nhấn nút Login -> Xử lý đăng nhập
    @FXML
    void onPasswordEnter(ActionEvent event) {
        handleLogin(event);
        System.out.println("Đã nhấn Enter ở ô Password!"); //check xem nhấn enter có tự bật login không
    }

    @FXML private Label lblError;

    @FXML
    void handleLogin(ActionEvent event) {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.equals("admin") && pass.equals("123")) {
            System.out.println("Đăng nhập thành công!");
            // Chuyển sang trang Dashboard hoặc Home sau khi login
            changeScene(event, "Dashboard.fxml", "Trang chủ");
        } else {
            lblError.setVisible(true);

        }
    }

    @FXML
    public void initialize() {
        txtUsername.setOnKeyTyped(e -> lblError.setVisible(false));
        txtPassword.setOnKeyTyped(e -> lblError.setVisible(false));
    }

    // 3. Chuyển sang trang Đăng ký (Sign Up)
    @FXML
    void goToSignUp(ActionEvent event) {
        changeScene(event, "SignupUI.fxml", "Đăng ký");
    }

    // 4. Chuyển sang trang Về chúng tôi (About Us)
    @FXML
    void goToAboutUs(ActionEvent event) {
        changeScene(event, "AboutUs.fxml", "Thông tin về chúng tôi");
    }

    // Hàm tiện ích để chuyển trang (tránh viết lặp code)
    private void changeScene(ActionEvent event, String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            System.err.println("Không tìm thấy file: " + fxmlFile);
            e.printStackTrace();
        }
    }

    @FXML
    private Button btnMenu;
    @FXML
    private VBox vboxSidebar; // Đảm bảo fx:id trong Scene Builder cũng là vboxSidebar

    @FXML
    private void toggleSidebar(ActionEvent event) {
        if (vboxSidebar.isVisible()) {
            vboxSidebar.setVisible(false);
            vboxSidebar.setManaged(false); // Thu hồi không gian
        } else {
            vboxSidebar.setVisible(true);
            vboxSidebar.setManaged(true);  // Hiển thị lại không gian
        }
        // 1. Kiểm tra xem biến có bị null không (nếu null là do chưa đặt fx:id)
        if (vboxSidebar == null) {
            System.out.println("LỖI: Chưa đặt fx:id cho vboxSidebar trong Scene Builder!");
            return;
        }
    }
}