package com.example.uinew.Controller;

import com.example.uinew.HandleLog;
import com.example.uinew.Initialize;
import com.example.uinew.OnEnter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class LoginController extends MainController implements OnEnter, HandleLog, Initialize {




    // 2. Nhấn Enter ở ô Password hoặc nhấn nút Login -> Xử lý đăng nhập
    @FXML
    public void onPasswordEnter(ActionEvent event) {
        handleLogin(event);
        System.out.println("Đã nhấn Enter ở ô Password!"); //check xem nhấn enter có tự bật login không
    }

    // 1. Nhấn Enter ở ô Username -> Nhảy xuống ô Password
    @FXML
    public void onUsernameEnter(ActionEvent event) {
        txtPassword.requestFocus();
    }

    @FXML private Label lblError;

    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("LOGIN CALLED"); //xem có chuyển tới handle login sau khi enter password không
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.equals("admin") && pass.equals("123")) {
            System.out.println("Đăng nhập thành công!");
            // Chuyển sang trang Dashboard hoặc Home sau khi login
            changeScene(event, "Dashboard.fxml", "Trang chủ");
        } else {
            lblError.setVisible(true);
            txtPassword.requestFocus(); //quay lại focus về ô password để laanf sau nhập sai vẫn enter login được

        }
    }

    @FXML
    public void initialize() {
        txtUsername.setOnKeyTyped(e -> lblError.setVisible(false));
        txtPassword.setOnKeyTyped(e -> lblError.setVisible(false));
    }


    // 4. Chuyển sang trang Về chúng tôi (About Us)
    @FXML
    void goToAboutUs(ActionEvent event) {
        changeScene(event, "AboutUs.fxml", "Thông tin về chúng tôi");
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