package com.example.uinew;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent; // QUAN TRỌNG
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginUI extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(SignupUI.class.getResource("LoginUI.fxml")); //nạp file fxml
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("ĐĂNG NHẬP");
        stage.setScene(scene);
        stage.show();
    }

}