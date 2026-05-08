package com.example.uinew.Controller;

import com.example.uinew.Interface.OnLogout;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class DashboardController extends HomeController{

    @FXML
    public void goToCurrentBidding(ActionEvent event) {
        changeScene(event, "ThisBidding.fxml", "Đăng nhập");
    }
    //vao lai san pham dang dau gia do



    @FXML
    Button createAuction; //nút đẻ tạo auction/bidding
    
    @FXML
    public void setCreateAuction(){
        setView("CreateBidding.fxml");
    }
}