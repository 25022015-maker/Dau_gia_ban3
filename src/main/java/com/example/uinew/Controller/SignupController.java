package com.example.uinew.Controller;

import com.example.uinew.Interface.ToLayOut;
import com.example.uinew.Interface.ToLogin;
import com.example.uinew.Interface.OnEnter;
import com.example.uinew.model.User;
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

    @FXML
    public void onPasswordEnter(ActionEvent event) {
        handleSignup(event);
        System.out.println("Đã nhấn Enter ở ô Password!"); //check xem nhấn enter có tự bật signup không
    }


    // 1. Nhấn Enter ở ô Username -> Nhảy xuống ô Password
    @FXML
    public void onUsernameEnter(ActionEvent event) {
        txtPassword.requestFocus();
    }

    @FXML
    public void goToLogin(ActionEvent event){
        changeScene(event, "/com/example/uinew/LoginView/LoginUI.fxml", "Đăng nhập");
    }

    @FXML
    public void goToMainLayout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/uinew/MainLayoutView/MainLayout.fxml")
            );
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
        // 1. Lấy dữ liệu từ các TextField trên giao diện Signup, đăng kí tạo 1 user mới
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        User newUSer = new User(username, password);
        if (username.trim().isEmpty() && password.trim().isEmpty()) {
            lblMessage.setVisible(true);
            System.out.println("Vui lòng nhập tên");}
        else{
            // 4. Chuyển thẳng vào Dashboard (không bắt user login lại lần nữa)
        goToMainLayout(event);
    }}

}



