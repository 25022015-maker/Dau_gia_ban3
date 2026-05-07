package com.example.uinew.Controller;

import com.example.uinew.Interface.ToLogin;
import com.example.uinew.Interface.OnEnter;
import com.example.uinew.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SignupController extends MainController implements OnEnter, ToLogin{
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
        changeScene(event, "/com/example/uinew/LoginUI.fxml", "Đăng nhập");
    }

    @FXML
    public void handleSignup(ActionEvent event) {
        // 1. Lấy dữ liệu từ các TextField trên giao diện Signup
        String username = txtUsername.getText();
        String password = txtPassword.getText();


            // 4. Chuyển thẳng vào Dashboard (không bắt user login lại lần nữa)
            changeScene(event, "/com/example/uinew/Dashboard.fxml", "Chào mừng bạn!");}

        }



