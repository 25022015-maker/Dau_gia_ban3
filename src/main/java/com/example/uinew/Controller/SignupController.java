package com.example.uinew.Controller;

import com.example.uinew.HandleLog;
import com.example.uinew.OnEnter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class SignupController extends MainController implements OnEnter, HandleLog {
    @FXML
    public void handleLogin(ActionEvent event) {
        System.out.println("CALLED"); //xem có chuyển tới handle login sau khi enter password không
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

    }

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
}
