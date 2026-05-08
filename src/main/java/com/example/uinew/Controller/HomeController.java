package com.example.uinew.Controller;

import com.example.uinew.Interface.OnLogout;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;

//Dashboard controller de load view dong, chua sidebar va topbar
public class HomeController extends MainController implements OnLogout {
    @FXML
    AnchorPane vboxSidebar;

    public void initialize() {

        vboxSidebar.setVisible(false);
        vboxSidebar.setManaged(false);
    }

    @FXML //hàm ẩn hiện sideBar
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

    //đổi nội dung vùng giữa
    @FXML
    public void setView(String fxmlFile) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().setAll(node);
        } catch (IOException e) { e.printStackTrace(); }
    }
}
