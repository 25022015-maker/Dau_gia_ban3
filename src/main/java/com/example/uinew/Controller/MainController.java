package com.example.uinew.Controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class MainController {
    @FXML protected TextField txtUsername;
    @FXML protected PasswordField txtPassword;
    private static User currentUser;


    // Hàm tiện ích để chuyển trang (tránh viết lặp code)
        protected void changeScene(ActionEvent event, String fxmlFile, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            System.err.println("Không tìm thấy file: " + fxmlFile);
            e.printStackTrace();
        }
    }


     public static void setCurrentUser(User user){
            currentUser = user;

     }

}
