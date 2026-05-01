package com.example.uinew.Controller;

import com.example.uinew.Interface.OnLogout;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.stage.Stage;

import java.util.EventListener;

public class DashboardController extends MainController implements OnLogout {

    @FXML
    public void goToCurrentBidding(ActionEvent event) {
        changeScene(event, "ThisBidding.fxml", "Đăng nhập");
    }
    //vao lai san pham dang dau gia do

    public void onLogout(ActionEvent event) {
        try {
            // 1. Xóa thông tin người dùng hiện tại trong hệ thống
            // Giả sử bạn có một biến static lưu User ở MainController
            MainController.setCurrentUser(null);

            // 2. Nếu có Socket đang chạy (cho việc đấu giá), hãy đóng nó
            // SocketManager.getInstance().disconnect();

            // 3. Sử dụng hàm changeScene bạn đã viết để quay lại màn hình Login
            // Chú ý: Đường dẫn phải chuẩn xác để tránh lỗi "Location is not set"
            changeScene(event, "LoginUI.fxml", "Đăng nhập hệ thống");

            System.out.println("Đăng xuất thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            // Hiển thị thông báo lỗi nếu không chuyển được scene
        }

    }
    //tạo sản phẩm để đấu giá nếu là bidder
    //thông báo thông tin người tham gia


    //chọn sản phẩm để tham gia đấu giá nếu là seller
    //ặt bid hoặc dùng auto bid
}