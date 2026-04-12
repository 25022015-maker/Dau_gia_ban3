package com.auction.project;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.auction.project.entitiesclasses.User;

public class LoginUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        TextField username = new TextField(); //user nhập
        username.setPromptText("Username"); //hiển thị gợi ý nhập

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Button loginBtn = new Button("Login"); //nút login

        Label message = new Label();

        loginBtn.setOnAction(e -> {if (username.getText().equals("admin") && password.getText().equals("123")) { message.setText("Đăng nhập thành công"); }//test
        else { message.setText("Sai tài khoản hoặc mật khẩu"); } });
    }
}
