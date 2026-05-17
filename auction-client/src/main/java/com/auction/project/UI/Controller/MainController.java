package com.auction.project.UI.Controller;

import com.auction.project.Entities.Item;
import com.auction.project.Entities.User;
import com.auction.project.UI.Interface.SceneLoader;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public abstract class MainController implements SceneLoader {
    private static User currentUser;
    private static Item selectedItem;



    public static void setCurrentUser(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }

    public static void setSelectedItem(Item Item) { selectedItem = Item; }
    public static Item getSelectedItem() { return selectedItem; }

    //đổi toàn bộ cửa sổ (Login/Logout)
    @FXML
    public void changeScene(ActionEvent event, String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }
}