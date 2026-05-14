package com.example.uinew.Controller;

import com.example.uinew.Interface.OnEnter;
import com.example.uinew.Interface.ToLayOut;
import com.example.uinew.Interface.ToSignUp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController extends MainController implements OnEnter, ToSignUp, ToLayOut {

    @FXML protected TextField txtUsername;
    @FXML protected PasswordField txtPassword; //input thông tin người dùng

    @FXML private Label lblError; //hiển thị báo sai tài khoản mật khẩu

    @FXML
    public void goToSignUp(ActionEvent event) {
        changeScene(event, "/com/example/uinew/SignUpView/SignupUI.fxml", "Đăng ký");
    }

    // 2. Nhấn Enter ở ô Password hoặc nhấn nút Login -> Xử lý đăng nhập
    @FXML
    public void onPasswordEnter(ActionEvent event) {
        handleLogin(event);
        System.out.println("Đã nhấn Enter ở ô Password!"); //test xem nhấn enter có tự bật login không
    }

    //Nhấn Enter ở ô Username -> Nhảy xuống ô Password
    @FXML
    public void onUsernameEnter(ActionEvent event) {
        txtPassword.requestFocus();
    }



    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("LOGIN CALLED"); //xem có chuyển tới handle login sau khi enter password không
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.equals("admin") && pass.equals("123")) {
            System.out.println("Đăng nhập thành công!");
            // Chuyển sang trang Dashboard hoặc Home sau khi login
            goToMainLayout(event);
        } else {
            lblError.setVisible(true);
            txtPassword.requestFocus(); //quay lại focus về ô password để laanf sau nhập sai vẫn enter login được

        }
    }

    @FXML
    public void goToMainLayout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load( getClass().getResource("/MainLayoutView/MainLayout.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        txtUsername.setOnKeyTyped(e -> lblError.setVisible(false));
        txtPassword.setOnKeyTyped(e -> lblError.setVisible(false));
    }

}