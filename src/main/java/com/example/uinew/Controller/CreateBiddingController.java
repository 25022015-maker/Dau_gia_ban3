package com.example.uinew.Controller;

import com.auction.project.Client.SocketClient;
import com.auction.project.Packets.GsonFactory;
import com.example.uinew.service.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreateBiddingController {

    @FXML private TextField txtName;
    @FXML private TextField txtDescription;
    @FXML private TextField txtStartPrice;
    @FXML private ChoiceBox<String> choiceBox;
    @FXML private DatePicker dateStart;
    @FXML private DatePicker dateEnd;
    @FXML private Label lblMessage;

    private final Gson gson = GsonFactory.create();

    @FXML
    public void initialize() {
        choiceBox.getItems().addAll("Đồ điện tử", "Nghệ thuật", "Phương tiện");
        choiceBox.getSelectionModel().selectFirst();
    }

    @FXML
    public void handleCreate(ActionEvent event) {
        // Validate
        String name  = txtName.getText().trim();
        String price = txtStartPrice.getText().trim().replace(",", "");
        LocalDate start = dateStart.getValue();
        LocalDate end   = dateEnd.getValue();

        if (name.isEmpty()) {
            showMsg("⚠️ Vui lòng nhập tên sản phẩm!", false); return;
        }
        if (price.isEmpty()) {
            showMsg("⚠️ Vui lòng nhập giá khởi điểm!", false); return;
        }
        if (start == null || end == null) {
            showMsg("⚠️ Vui lòng chọn ngày bắt đầu và kết thúc!", false); return;
        }
        if (!end.isAfter(start)) {
            showMsg("⚠️ Ngày kết thúc phải sau ngày bắt đầu!", false); return;
        }

        double startPrice;
        try {
            startPrice = Double.parseDouble(price);
            if (startPrice <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showMsg("⚠️ Giá khởi điểm không hợp lệ!", false); return;
        }

        // Map loại sản phẩm
        String type = switch (choiceBox.getValue()) {
            case "Đồ điện tử" -> "ELECTRONICS";
            case "Nghệ thuật"  -> "ART";
            case "Phương tiện" -> "VEHICLE";
            default            -> "ELECTRONICS";
        };

        // Gửi lên server
        SocketClient client = SessionManager.getSocketClient();
        if (client == null || !client.isConnected()) {
            showMsg("⚠️ Chưa kết nối server!", false); return;
        }

        JsonObject req = new JsonObject();
        req.addProperty("action",      "CREATE_AUCTION");
        req.addProperty("itemName",    name);
        req.addProperty("itemType",    type);
        req.addProperty("description", txtDescription.getText().trim());
        req.addProperty("startPrice",  startPrice);
        req.addProperty("startTime",   LocalDateTime.of(start, java.time.LocalTime.of(8, 0)).toString());
        req.addProperty("endTime",     LocalDateTime.of(end,   java.time.LocalTime.of(20, 0)).toString());
        req.addProperty("seller",      SessionManager.getCurrentUser());

        client.sendRaw(gson.toJson(req));

        showMsg("✅ Đã tạo phiên đấu giá: " + name, true);
        System.out.println("[CreateBidding] Gửi tạo phiên: " + name);

        // Chuyển về Dashboard sau 1.5 giây
        new javafx.animation.Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1.5),
                e -> HomeController.getInstance().setView("/com/example/uinew/Dashboard.fxml")
        )).play();
    }

    @FXML
    public void backToDashboard() {
        HomeController.getInstance().setView("/com/example/uinew/Dashboard.fxml");
    }

    private void showMsg(String msg, boolean ok) {
        lblMessage.setText(msg);
        lblMessage.setStyle(ok
                ? "-fx-text-fill: #2ecc71; -fx-font-size: 13px;"
                : "-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);
    }
}