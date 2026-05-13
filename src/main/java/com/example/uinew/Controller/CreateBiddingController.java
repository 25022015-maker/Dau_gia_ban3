package com.example.uinew.Controller;

import com.example.uinew.Interface.ViewLoader;
import com.example.uinew.model.Product;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;

public class CreateBiddingController {
    @FXML
    private TextField txtName, txtStartPrice, txtDescription; //thong tin san pham
    @FXML
    private DatePicker end;
    @FXML
    private DatePicker start;
    @FXML
    Button cancel;


    @FXML
    void handleCreate(ActionEvent event) {

        String name = txtName.getText();

        double startPrice =
                Double.parseDouble(txtStartPrice.getText());

        LocalDate dateEnd = end.getValue();

        LocalDate dateStart = start.getValue();

        String description = txtDescription.getText();

        String productType = choiceBox.getValue();

        Product newProduct = new Product(
                name,
                startPrice,
                description,
                productType
        );

        MainController.setSelectedProduct(newProduct);

        System.out.println("Tạo phiên đấu giá thành công!");

        HomeController.getInstance().setView("/com/example/uinew/ThisBidding.fxml");
    }


    @FXML
    public void backToDashboard() {
        HomeController.getInstance().setView("/com/example/uinew/Dashboard.fxml");
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
            String productType = newValue;
        });
    }
    //gán newValue vào một biến toàn cục để lưu trữ



}