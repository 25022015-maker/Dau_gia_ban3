package com.example.controller;

import com.example.service.ApiClient;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class CreateAuctionController {

    @FXML private ComboBox<String> cmbItemType;
    @FXML private TextField  txtItemName;
    @FXML private TextArea   txtDescription;
    @FXML private TextField  txtStartPrice;
    @FXML private TextField  txtMinBid;
    @FXML private DatePicker dpStartDate;
    @FXML private TextField  txtStartHour;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField  txtEndHour;
    @FXML private TextField  txtAntiSnipe;

    // Electronics
    @FXML private TextField  txtBrand;
    @FXML private TextField  txtWarranty;
    // Art
    @FXML private TextField  txtArtist;
    @FXML private TextField  txtYearCreated;
    // Vehicle
    @FXML private TextField  txtVehicleBrand;
    @FXML private TextField  txtManufactureYear;
    @FXML private TextField  txtMileage;

    @FXML private TextField  txtImageUrl;
    @FXML private ImageView  imgPreview;
    @FXML private Label      lblImageStatus;
    @FXML private Label      lblMessage;
    @FXML private Button     btnCreate;

    private String imageBase64    = null;
    private String pendingImageUrl = null;

    @FXML
    public void initialize() {
        if (cmbItemType != null) {
            cmbItemType.getItems().setAll("ELECTRONICS", "ART", "VEHICLE");
            cmbItemType.setValue("ELECTRONICS");
            cmbItemType.setOnAction(e -> updateTypeFields());
            updateTypeFields();
        }
        // Gợi ý ngày mặc định
        if (dpStartDate != null) dpStartDate.setValue(LocalDate.now());
        if (dpEndDate   != null) dpEndDate.setValue(LocalDate.now().plusDays(1));
        if (txtStartHour != null) txtStartHour.setText(
                LocalDateTime.now().plusMinutes(10).format(DateTimeFormatter.ofPattern("HH:mm")));
        if (txtEndHour != null) txtEndHour.setText("18:00");
    }

    @FXML
    public void handleCreate() {
        if (lblMessage != null) lblMessage.setText("");
        try {
            JsonObject req = buildRequest();
            if (btnCreate != null) btnCreate.setDisable(true);
            new Thread(() -> {
                try {
                    ApiClient.createAuction(req);
                    Platform.runLater(() -> {
                        if (lblMessage != null) {
                            lblMessage.setText("✅ Tạo phiên đấu giá thành công!");
                            lblMessage.setStyle("-fx-text-fill: #2ecc71;");
                        }
                        clearForm();
                        MainLayoutController main = MainLayoutController.getInstance();
                        if (main != null) main.loadView("/com/example/user/Dashboard.fxml");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (lblMessage != null) {
                            lblMessage.setText(e.getMessage());
                            lblMessage.setStyle("-fx-text-fill: #e74c3c;");
                        }
                        if (btnCreate != null) btnCreate.setDisable(false);
                    });
                }
            }, "CreateAuctionThread").start();
        } catch (IllegalArgumentException e) {
            if (lblMessage != null) {
                lblMessage.setText(e.getMessage());
                lblMessage.setStyle("-fx-text-fill: #e74c3c;");
            }
        }
    }

    @FXML
    public void handlePreviewImage() {
        if (txtImageUrl == null || txtImageUrl.getText().isBlank()) return;
        String url = txtImageUrl.getText().trim();
        pendingImageUrl = url;
        imageBase64 = null;

        if (lblImageStatus != null) {
            lblImageStatus.setText("Đang tải...");
            lblImageStatus.setStyle("-fx-text-fill: #7f8c8d;");
        }

        // JavaFX Image tải URL trực tiếp, background=true không chặn UI
        Image img = new Image(url, 200, 150, true, true, true);

        img.errorProperty().addListener((obs, wasErr, isErr) -> {
            if (isErr) Platform.runLater(() -> {
                if (lblImageStatus != null) {
                    lblImageStatus.setText("❌ Không tải được. Link phải là link ảnh trực tiếp (.jpg/.png)");
                    lblImageStatus.setStyle("-fx-text-fill: #e74c3c;");
                }
                pendingImageUrl = null;
            });
        });

        img.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0 && !img.isError()) {
                Platform.runLater(() -> {
                    if (imgPreview != null) {
                        imgPreview.setImage(img);
                        imgPreview.setVisible(true);
                    }
                    if (lblImageStatus != null) {
                        lblImageStatus.setText("✅ Ảnh đã tải — sẽ lưu kèm khi tạo phiên");
                        lblImageStatus.setStyle("-fx-text-fill: #2ecc71;");
                    }
                });
            }
        });
    }

    @FXML
    public void handleCancel() {
        MainLayoutController main = MainLayoutController.getInstance();
        if (main != null) main.loadView("/com/example/user/Dashboard.fxml");
    }

    // ── Build JSON request ────────────────────────────────────────────────────

    private JsonObject buildRequest() {
        String itemName = require(txtItemName, "Tên sản phẩm");
        String startPriceStr = require(txtStartPrice, "Giá sàn");
        String startTime = buildDateTime(dpStartDate, txtStartHour, "Ngày bắt đầu");
        String endTime   = buildDateTime(dpEndDate,   txtEndHour,   "Ngày kết thúc");

        long startPrice = parseLong(startPriceStr, "Giá sàn phải là số");
        long minBid     = txtMinBid != null && !txtMinBid.getText().isBlank()
                ? parseLong(txtMinBid.getText().trim(), "Bước giá phải là số") : 1000L;

        String itemType = cmbItemType != null ? cmbItemType.getValue() : "ELECTRONICS";

        JsonObject req = new JsonObject();
        req.addProperty("itemName",    itemName);
        req.addProperty("itemType",    itemType);
        req.addProperty("startPrice",  startPrice);
        req.addProperty("minBid",      minBid);
        req.addProperty("startTime",   startTime);
        req.addProperty("endTime",     endTime);
        req.addProperty("antiSnipeExtSeconds",
                txtAntiSnipe != null && !txtAntiSnipe.getText().isBlank()
                        ? Integer.parseInt(txtAntiSnipe.getText().trim()) : 60);
        if (txtDescription != null) req.addProperty("description", txtDescription.getText());

        // Tải bytes từ URL để encode Base64 gửi server
        if (pendingImageUrl != null) {
            try {
                HttpClient http = HttpClient.newHttpClient();
                HttpRequest imgReq = HttpRequest.newBuilder()
                        .uri(URI.create(pendingImageUrl))
                        .header("User-Agent", "Mozilla/5.0")
                        .build();
                byte[] bytes = http.send(imgReq, HttpResponse.BodyHandlers.ofByteArray()).body();
                req.addProperty("imageBase64", Base64.getEncoder().encodeToString(bytes));
            } catch (Exception ignored) {}
        }

        switch (itemType) {
            case "ART" -> {
                req.addProperty("artist",      val(txtArtist));
                req.addProperty("yearCreated", num(txtYearCreated));
            }
            case "VEHICLE" -> {
                req.addProperty("vehicleBrand",    val(txtVehicleBrand));
                req.addProperty("manufactureYear", num(txtManufactureYear));
                req.addProperty("mileageKm",       num(txtMileage));
            }
            default -> {
                req.addProperty("brand",          val(txtBrand));
                req.addProperty("warrantyMonths", num(txtWarranty));
            }
        }
        return req;
    }

    private void updateTypeFields() {
        String type = cmbItemType.getValue();
        setVisible(txtBrand,         "ELECTRONICS".equals(type));
        setVisible(txtWarranty,      "ELECTRONICS".equals(type));
        setVisible(txtArtist,        "ART".equals(type));
        setVisible(txtYearCreated,   "ART".equals(type));
        setVisible(txtVehicleBrand,  "VEHICLE".equals(type));
        setVisible(txtManufactureYear,"VEHICLE".equals(type));
        setVisible(txtMileage,       "VEHICLE".equals(type));
    }

    private void clearForm() {
        if (txtItemName   != null) txtItemName.clear();
        if (txtDescription!= null) txtDescription.clear();
        if (txtStartPrice != null) txtStartPrice.clear();
        if (txtMinBid     != null) txtMinBid.clear();
        if (txtStartHour  != null) txtStartHour.clear();
        if (txtEndHour    != null) txtEndHour.clear();
        if (btnCreate     != null) btnCreate.setDisable(false);
    }

    private String buildDateTime(DatePicker dp, TextField hourField, String label) {
        if (dp == null || dp.getValue() == null)
            throw new IllegalArgumentException(label + ": chưa chọn ngày");
        String timeStr = (hourField != null && !hourField.getText().isBlank())
                ? hourField.getText().trim() : "00:00";
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min  = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return dp.getValue().atTime(hour, min).format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e) {
            throw new IllegalArgumentException(label + ": giờ không hợp lệ, nhập dạng HH:mm (VD: 09:00)");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String require(TextField tf, String name) {
        if (tf == null || tf.getText().isBlank())
            throw new IllegalArgumentException(name + " không được để trống");
        return tf.getText().trim();
    }

    private long parseLong(String s, String errMsg) {
        try { return Long.parseLong(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(errMsg); }
    }

    private String val(TextField tf) {
        return tf != null ? tf.getText().trim() : "";
    }

    private int num(TextField tf) {
        if (tf == null || tf.getText().isBlank()) return 0;
        try { return Integer.parseInt(tf.getText().trim()); } catch (NumberFormatException e) { return 0; }
    }

    private void setVisible(TextField tf, boolean v) {
        if (tf != null) { tf.setVisible(v); tf.setManaged(v); }
    }
}