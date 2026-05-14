package com.auction.project.UI.Controller;

import com.auction.project.UI.Interface.ToLogin;
import com.auction.project.UI.Interface.ToSignUp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class SideBarController extends HomeController implements ToLogin, ToSignUp {

    @FXML protected VBox vboxSidebar;

    // 4. Chuyển sang trang Về chúng tôi (About Us)
    @FXML
    void goToAboutUs(ActionEvent event) {
        setView("com/example/uinew/AboutUs.FXML");
    }



    @FXML
    public void goToSignUp(ActionEvent event) {
        changeScene(event, "com/example/uinew/SignupUI.fxml", "Đăng ký");
    }

    public void goToLogin(ActionEvent event){
        changeScene(event, "/com/example/uinew/LoginUI.fxml", "Đăng nhập");
    }
}
