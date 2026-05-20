package com.example.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class ProductCardController {

    @FXML private ImageView imgProduct;
    @FXML private Label     lblProductName;
    @FXML private Label     lblProductPrice;
    @FXML private Label     lblStatus;
    @FXML private Label     lblEndTimeProduct;
    @FXML private Button    btnBid;

    private long auctionId;

    // ── Set dữ liệu ban đầu ───────────────────────────────────────────────────

    public void setData(JsonObject auction) {
        this.auctionId = auction.get("id").getAsLong();

        String name   = getString(auction, "itemName", "Sản phẩm");
        long   price  = getLong(auction, "currentPrice");
        String status = getString(auction, "status", "PENDING");
        String endTime= getString(auction, "endTime", "");

        lblProductName.setText(name);
        lblProductPrice.setText(formatPrice(price));
        updateStatusLabel(status);
        if (!endTime.isEmpty()) {
            lblEndTimeProduct.setText("⏱ " + endTime.replace("T", " ").substring(0, Math.min(16, endTime.length())));
        }

        // Load ảnh Base64 nếu có
        String img = getString(auction, "imageBase64", null);
        if (img != null && !img.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(img);
                imgProduct.setImage(new Image(new ByteArrayInputStream(bytes)));
            } catch (Exception ignored) {}
        }

        btnBid.setOnAction(e -> openAuctionRoom(auction));
    }

    // ── Cập nhật realtime từ WebSocket ────────────────────────────────────────

    public void updateFromWs(JsonObject msg) {
        if (msg.has("newPrice")) {
            lblProductPrice.setText(formatPrice(msg.get("newPrice").getAsLong()));
        }
        if (msg.has("status")) {
            updateStatusLabel(msg.get("status").getAsString());
        }
        if (msg.has("endTime")) {
            String et = msg.get("endTime").getAsString();
            lblEndTimeProduct.setText("⏱ " + et.replace("T", " ").substring(0, Math.min(16, et.length())));
        }
    }

    @FXML
    public void handleBidAction() {
        // Được gọi khi click nút Tham Gia (dự phòng, setData đã bind)
        openAuctionRoom(null);
    }

    // ── Mở phòng đấu giá ─────────────────────────────────────────────────────

    private void openAuctionRoom(JsonObject auction) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/user/AuctionRoom.fxml"));
            Node view = loader.load();
            AuctionRoomController ctrl = loader.getController();
            ctrl.setAuctionId(auctionId);
            MainLayoutController main = MainLayoutController.getInstance();
            if (main != null) main.setContent(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateStatusLabel(String status) {
        switch (status) {
            case "RUNNING"  -> { lblStatus.setText("🔴 Đang diễn ra"); lblStatus.setStyle("-fx-text-fill: #2ecc71;"); }
            case "PENDING"  -> { lblStatus.setText("⏳ Sắp bắt đầu");  lblStatus.setStyle("-fx-text-fill: #f39c12;"); }
            case "FINISHED" -> { lblStatus.setText("✅ Đã kết thúc");   lblStatus.setStyle("-fx-text-fill: #7f8c8d;"); }
            case "CANCELED" -> { lblStatus.setText("❌ Đã hủy");        lblStatus.setStyle("-fx-text-fill: #e74c3c;"); }
            default         -> lblStatus.setText(status);
        }
    }

    private String formatPrice(long price) {
        return String.format("%,d ₫", price).replace(",", ".");
    }

    private String getString(JsonObject o, String key, String def) {
        JsonElement el = o.get(key);
        return (el == null || el.isJsonNull()) ? def : el.getAsString();
    }

    private long getLong(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return (el == null || el.isJsonNull()) ? 0L : el.getAsLong();
    }
}