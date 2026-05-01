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


    public void goToLogin(ActionEvent event){
        changeScene(event, "LoginUI.fxml", "Đăng nhập");
    }

    @FXML
    public void handleSignup(ActionEvent event) {
        // 1. Lấy dữ liệu từ các TextField trên giao diện Signup
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // 2. Gọi Service/Database để tạo tài khoản mới
        // Giả sử database.register trả về đối tượng User sau khi tạo xong ->ket noi voi database
        User newUser = database.register(username, password); //database backend

        if (newUser != null) {
            // 3. KẾT NỐI ĐÂY NÈ: Lưu user mới tạo vào MainController
            MainController.setCurrentUser(newUser);

            // 4. Chuyển thẳng vào Dashboard (không bắt user login lại lần nữa)
            changeScene(event, "/com/example/uinew/View/DashboardView.fxml", "Chào mừng bạn!");

            System.out.println("Đăng ký và đăng nhập thành công!");
        } else {
            // Hiển thị thông báo lỗi nếu trùng username/email
            lblMessage.setText("Đăng ký thất bại, vui lòng thử lại!");
        }
    }
}
