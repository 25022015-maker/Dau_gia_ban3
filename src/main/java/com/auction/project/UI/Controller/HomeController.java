package com.auction.project.UI.Controller;

import com.auction.project.UI.Interface.OnLogout;
import com.auction.project.UI.Interface.ViewLoader;
import com.auction.project.UI.service.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class HomeController extends MainController implements OnLogout, ViewLoader {

    @FXML protected VBox vboxSidebar;
    @FXML protected StackPane contentArea;

    private static HomeController instance;

    public void initialize() {
        instance = this;
        vboxSidebar.setVisible(false);
        vboxSidebar.setManaged(false);
        setView("/com/example/uinew/Dashboard.fxml");
    }

    public static HomeController getInstance() { return instance; }

    @FXML
    private void toggleSidebar(ActionEvent event) {
        if (vboxSidebar.isVisible()) {
            vboxSidebar.setVisible(false);
            vboxSidebar.setManaged(false);
        } else {
            vboxSidebar.setVisible(true);
            vboxSidebar.setManaged(true);
        }
    }

    @FXML void goToDashBoard(ActionEvent event)           { setView("/com/example/uinew/Dashboard.fxml"); }
    @FXML public void goToAccount(ActionEvent event)      { setView("/com/example/uinew/Account.fxml"); }
    @FXML public void goToNotification(ActionEvent event) { setView("/com/example/uinew/Notification.fxml"); }
    @FXML public void goToLanguage(ActionEvent event)     { setView("/com/example/uinew/Language.fxml"); }
    @FXML public void goToManagement(ActionEvent event)   { setView("/com/example/uinew/Management.fxml"); }
    @FXML public void goToProducts(ActionEvent event)     { setView("/com/example/uinew/ProductList.fxml"); }
    @FXML public void createAuction()                     { setView("/com/example/uinew/CreateBidding.fxml"); }

    public void onLogout(ActionEvent event) {
        try {
            SessionManager.logout();
            changeScene(event, "/com/example/uinew/LoginUI.fxml", "Đăng nhập");
            System.out.println("Đăng xuất thành công!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setView(String fxmlFile) {
        try {
            Node node = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setView(Node node) {
        contentArea.getChildren().setAll(node);
    }
}