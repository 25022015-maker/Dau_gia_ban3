package com.example.uinew.Controller;

import com.example.uinew.model.Product;
import com.example.uinew.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

public abstract class MainController {
    protected static User currentUser;
    protected static Product selectedProduct;

    @FXML protected TextField txtUsername;
    @FXML protected PasswordField txtPassword;

    @FXML protected StackPane contentArea; // fx:id của vùng chứa trong MainLayout

    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    public static void setSelectedProduct(Product product) { selectedProduct = product; }
    public static Product getSelectedProduct() { return selectedProduct; }

    //đổi toàn bộ cửa sổ (Login/Logout)
    @FXML
    protected void changeScene(ActionEvent event, String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    //đổi nội dung vùng giữa
    @FXML
    public void setView(String fxmlFile) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().setAll(node);
        } catch (IOException e) { e.printStackTrace(); }
    }

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

}