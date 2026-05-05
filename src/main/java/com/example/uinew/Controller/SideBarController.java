package com.example.uinew.Controller;

import com.example.uinew.Interface.ToLogin;
import com.example.uinew.Interface.ToSignUp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

public class SideBarController extends MainController implements ToLogin, ToSignUp {
    @FXML
    VBox vboxSidebar;
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
            ;
        }
    }

    // 4. Chuyển sang trang Về chúng tôi (About Us)
    @FXML
    void goToAboutUs(ActionEvent event) {
        changeScene(event, "AboutUs.FXML", "Thông tin về chúng tôi");
    }

    @FXML void goToDashBoard(ActionEvent event){
        changeScene(event, "DashBoard.FXML", "Trang chu");
    }

    @FXML
    public void goToSignUp(ActionEvent event) {
        changeScene(event, "SignupUI.fxml", "Đăng ký");
    }

    public void goToLogin(ActionEvent event){
        changeScene(event, "LoginUI.fxml", "Đăng nhập");
    }
}
