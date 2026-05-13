package com.example.uinew.Controller;

import com.example.uinew.Interface.ViewLoader;
import com.example.uinew.model.Product;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;

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

   // @FXML
   // Label lblError;


    @FXML
    void handleCreate(ActionEvent event) {

        String name = txtName.getText();
        double startPrice = 0;
        Boolean hasError = false;

        if (txtStartPrice == null || txtStartPrice.getText().trim().isEmpty()) {
            //lblError.isVisible();
            System.out.println("Vui lon nhập giá tiền");
        } else {
            try {
                startPrice = Double.parseDouble(txtStartPrice.getText());

            } catch (NumberFormatException e) {
                System.out.println("Lỗi: Giá tiền phải là một con số hợp lệ!");
                hasError = true;
            }
        }

        LocalDate dateEnd = end.getValue();
        LocalDate dateStart = start.getValue();
        String description = txtDescription.getText();
        String productType = choiceBox.getValue();

        if (productType == null) {
            hasError = true;
        }

        if (!hasError) {
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
            String productType = newValue;    //gán newValue vào một biến toàn cục để lưu trữ
        });
    }

}