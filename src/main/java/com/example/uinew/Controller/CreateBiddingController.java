package com.example.uinew.Controller;

import com.example.uinew.Interface.ShowError;
import com.example.uinew.model.Product;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;

public class CreateBiddingController implements ShowError {
    @FXML
    private TextField txtName, txtStartPrice, txtDescription; //thong tin san pham
    @FXML
    private DatePicker end;
    @FXML
    private Spinner<Integer> hourStart, minStart, hourEnd, minEnd;
    @FXML
    private DatePicker start;
    @FXML
    Button cancel;

    @FXML TextField txtMinBid;

   @FXML
   Label lblError;


    @FXML
    void handleCreate(ActionEvent event) {

        lblError.setText("");
        lblError.setTextFill(Color.RED);

        String name = txtName.getText();
        String description = txtDescription.getText();
        String productType = choiceBox.getValue();
        double startPrice = 0;
        Boolean hasError = false;
        LocalDate dateEnd = end.getValue();

        Integer hS = hourStart.getValue();
        Integer mS = minStart.getValue();

        LocalDate dateStart = start.getValue();

        Integer hE = hourEnd.getValue();
        Integer mE = minEnd.getValue();

        if (txtStartPrice.getText().trim().isEmpty()) {
            //lblError.isVisible();
            showError("Lỗi: Vui lòng nhập giá khởi điểm!");
            System.out.println("Vui long nhập giá tiền");
        } else {
            try {
                startPrice = Double.parseDouble(txtStartPrice.getText());

            } catch (NumberFormatException e) {
                System.out.println("Lỗi: Giá tiền phải là một con số hợp lệ!");
                showError("Lỗi: Giá tiền phải là số dương hợp lệ!");
                hasError = true;
            }
        }

        if (dateStart == null || dateEnd == null) {
            System.out.println("Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc");
            showError("Lỗi: Vui lòng chọn cả ngày bắt đầu và kết thúc!");
            hasError = true;
        }

        if (productType.isEmpty()) {
            showError("Lỗi: Vui lon chọn loại sản phẩm");
            hasError = true;
        }

        if (name.isEmpty()) {
            showError("Lỗi: Tên sản phẩm không được để trống!");
            hasError = true ;
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
            showError("Tạo phiên đấu giá thành công!");

            HomeController.getInstance().setView("/com/example/uinew/CurrentBiddingView/ThisBidding.fxml");
        }
    }


    @FXML
    public void backToDashboard() {
        HomeController.getInstance().setView("/com/example/uinew/Dashboard.fxml");
    }

    public void showError(String message) {
        lblError.setText(message);
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
        hourStart.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0));
        minStart.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        hourEnd.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 23));
        minEnd.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 59));
    }

}