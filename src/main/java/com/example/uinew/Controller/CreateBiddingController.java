package com.example.uinew.Controller;

import com.example.uinew.model.Product;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class CreateBiddingController extends HomeController{
    @FXML private TextField txtName, txtStartPrice, txtDescription, txtProductType; //thong tin san pham
    @FXML private DatePicker dateEnd;
    @FXML private DatePicker dateStart;
    @FXML Button ok;
    @FXML Button cancel;

    @FXML
    void handleCreate(ActionEvent event) { //tao san pham dau gia

        System.out.println("Tạo phiên đấu giá thành công!");
        //Chuyển sang trang chi tiết đấu giá ngay sau khi taoj'
        changeScene(event,"/com/example/uinew/View/ThisBidding.fxml", "Current Bidding");

    }

    @FXML public void backToDashboard(){
        setView("DashBoard.fxml");
    }

   @FXML
    ChoiceBox<String> choiceBox;

    public void initialize() {
        choiceBox.getItems().addAll("Đồ điện tử", "Nghệ thuật", "Phương tiện");
        choiceBox.getSelectionModel().selectFirst(); //chọn giá trị hiển thị mặc định của choicebox

        choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            // oldValue : giá trị trước khi đổi
            // newValue : giá trị mới client vừa chọn

            System.out.println("Client vừa đổi từ: " + oldValue + " sang " + newValue);

            //gán newValue vào một biến toàn cục để lưu trữ
            String productType = newValue;

            Product newProduct = new Product(txtName.getText(), Double.parseDouble(txtStartPrice.getText()), txtDescription.getText(), productType);//tạo sản phẩm dựa trên input
            MainController.setSelectedProduct(newProduct);//lưu thông tin sản phẩm
        });
    }
}