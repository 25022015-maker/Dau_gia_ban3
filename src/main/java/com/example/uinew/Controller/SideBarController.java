package com.example.uinew.Controller;

import com.example.uinew.Interface.ToLogin;
import com.example.uinew.Interface.ToSignUp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class SideBarController extends MainController implements ToLogin, ToSignUp {

    // 4. Chuyển sang trang Về chúng tôi (About Us)
    @FXML
    void goToAboutUs(ActionEvent event) {
        changeScene(event, "com/example/uinew/AboutUs.FXML", "Thông tin về chúng tôi");
    }

    @FXML void goToDashBoard(ActionEvent event){
        changeScene(event, "/com/example/uinew/Dashboard.FXML", "Trang chu");
    }

    @FXML
    public void goToSignUp(ActionEvent event) {
        changeScene(event, "com/example/uinew/SignupUI.fxml", "Đăng ký");
    }

    public void goToLogin(ActionEvent event){
        changeScene(event, "/com/example/uinew/LoginUI.fxml", "Đăng nhập");
    }
}
