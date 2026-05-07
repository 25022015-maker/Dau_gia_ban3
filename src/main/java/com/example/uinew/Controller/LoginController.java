package com.example.uinew.Controller;

import com.example.uinew.Interface.Initialize;
import com.example.uinew.Interface.OnEnter;
import com.example.uinew.Interface.ToSignUp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class LoginController extends MainController implements OnEnter, ToSignUp {
    @FXML
    private Button signUp;

    @FXML
    public void goToSignUp(ActionEvent event) {
        changeScene(event, "/com/example/uinew/SignupUI.fxml", "Đăng ký");
    }

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

    @FXML private Label lblError; //hiển thị báo sai tài khoản mật khẩu

    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("LOGIN CALLED"); //xem có chuyển tới handle login sau khi enter password không
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.equals("admin") && pass.equals("123")) {
            System.out.println("Đăng nhập thành công!");
            // Chuyển sang trang Dashboard hoặc Home sau khi login
            changeScene(event, "/com/example/uinew/MainLayout.fxml", "Trang chủ");
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


    @FXML
    private Button btnMenu;

}