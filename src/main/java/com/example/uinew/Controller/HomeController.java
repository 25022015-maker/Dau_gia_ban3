package com.example.uinew.Controller;

import com.almasb.fxgl.core.View;
import com.example.uinew.Interface.OnLogout;
import com.example.uinew.Interface.SceneLoader;
import com.example.uinew.Interface.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

import javax.management.ValueExp;
import java.io.IOException;

//Dashboard controller de load view dong, chua sidebar va topbar
public class HomeController extends MainController implements OnLogout, ViewLoader {

    @FXML protected AnchorPane vboxSidebar;
    @FXML protected StackPane contentArea;
    public void initialize() {
        vboxSidebar.setVisible(false);
        vboxSidebar.setManaged(false);
        setView("/com/example/uinew/Dashboard.fxml");
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
           //MainController.setCurrentUser(null);

            // 2. Nếu có Socket đang chạy (cho việc đấu giá), hãy đóng nó
            // SocketManager.getInstance().disconnect();

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
