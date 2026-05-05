package com.example.uinew.Controller;

import com.example.uinew.model.Product;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class CreateBiddingController extends MainController {
    @FXML private TextField txtName, txtStartPrice, txtDescription; //thong tin san pham
    @FXML private DatePicker dateEnd;

    @FXML
    void handleCreate(ActionEvent event) { //tao san pham dau gia
        String name = txtName.getText();
        double price = Double.parseDouble(txtStartPrice.getText());

        Product newProduct = new Product(name, price, txtDescription.getText());

        //Lưu sản phẩm vào trạm trung chuyển để trang sau lấy ra dùng
        MainController.setSelectedProduct(newProduct);

        System.out.println("Tạo phiên đấu giá thành công!");
        //Chuyển sang trang chi tiết đấu giá ngay sau khi taoj'
        changeScene(event,"/com/example/uinew/View/ThisBidding.fxml", "Current Bidding");
    }
}