package com.example.uinew.Controller;

import com.auction.project.Client.SocketClient;
import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.example.uinew.service.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controller cho màn hình đặt giá realtime.
 *
 * Không extends HomeController vì đây là view con load vào contentArea.
 * Kết nối socket qua SessionManager.getSocketClient().
 */
public class ThisBiddingController implements Initializable {

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private Label lblProductName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblTimer;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private ListView<String> bidHistoryList;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final Gson gson = GsonFactory.create();
    private int timeLeft = 3600;
    private Timeline timer;

    /**
     * auctionId của phiên đang xem.
     * Được set từ màn hình trước qua setAuctionId() hoặc lấy từ selectedProduct.
     */
    private String currentAuctionId;

    // ── Initialize ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        startTimer();
        registerBidUpdateListener();

        // Lấy auctionId từ màn hình trước (nếu có)
        // TODO: set currentAuctionId từ màn hình Dashboard khi click vào card
        // Tạm thời dùng auctionId đầu tiên để test
        currentAuctionId = "1001"; // ← thay bằng ID thật khi có

        subscribeAuction(currentAuctionId);
    }

    // ── Đặt giá ───────────────────────────────────────────────────────────────

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        String amountText = txtBidAmount.getText().trim();
        if (amountText.isEmpty()) {
            System.out.println("[Bid] Chưa nhập giá!");
            return;
        }

        double bidAmount;
        try {
            // Hỗ trợ cả dạng "6000000" và "6,000,000"
            bidAmount = Double.parseDouble(amountText.replace(",", ""));
        } catch (NumberFormatException e) {
            System.out.println("[Bid] Giá không hợp lệ: " + amountText);
            return;
        }

        SocketClient client = SessionManager.getSocketClient();
        if (client == null || !client.isConnected()) {
            System.out.println("[Bid] Chưa kết nối server!");
            return;
        }

        String bidderId = SessionManager.getCurrentUser();
        if (bidderId == null) {
            System.out.println("[Bid] Chưa đăng nhập!");
            return;
        }

        // Gửi BidRequest lên server
        JsonObject req = new JsonObject();
        req.addProperty("action", "BID");
        req.addProperty("auctionId", currentAuctionId);
        req.addProperty("bidderId", bidderId);
        req.addProperty("bidAmount", bidAmount);
        client.sendRaw(gson.toJson(req));

        System.out.println("[Bid] Đã gửi: " + bidAmount + " cho phiên " + currentAuctionId);

        // Xóa ô nhập sau khi gửi
        txtBidAmount.clear();

        // Disable nút tạm thời tránh spam
        btnPlaceBid.setDisable(true);
        new Timeline(new KeyFrame(Duration.seconds(1),
                e -> btnPlaceBid.setDisable(false))).play();
    }

    // ── Subscribe & nhận realtime ─────────────────────────────────────────────

    private void subscribeAuction(String auctionId) {
        SocketClient client = SessionManager.getSocketClient();
        if (client == null) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", "SUBSCRIBE");
        req.addProperty("auctionId", auctionId);
        client.sendRaw(gson.toJson(req));
    }

    private void registerBidUpdateListener() {
        SocketClient client = SessionManager.getSocketClient();
        if (client == null) return;

        client.setOnResponse(response -> {
            if (response == null) return;

            if (response.getType() == ResponseType.BID_UPDATE) {
                Platform.runLater(() -> handleBidUpdate(response));

            } else if (response.getType() == ResponseType.BID_SUCCESS) {
                Platform.runLater(() -> {
                    System.out.println("[Bid] Đặt giá thành công!");
                    // Thêm vào lịch sử
                    bidHistoryList.getItems().add(0,
                            "✅ Bạn vừa đặt giá thành công");
                });

            } else if (response.getType() == ResponseType.BID_FAILURE) {
                Platform.runLater(() -> {
                    System.out.println("[Bid] Thất bại: " + response.getMessage());
                    bidHistoryList.getItems().add(0,
                            "❌ " + response.getMessage());
                });
            }
        });
    }

    /**
     * Cập nhật UI khi nhận BID_UPDATE từ server.
     * Chạy trên JavaFX thread (Platform.runLater đảm bảo).
     */
    private void handleBidUpdate(Response response) {
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(response.getData()),
                    new TypeToken<Map<String, Object>>(){}.getType());

            if (data == null) return;

            double newPrice = ((Number) data.get("currentPrice")).doubleValue();
            String bidder   = String.valueOf(data.get("leadingBidder"));

            // Cập nhật label giá
            lblCurrentPrice.setText(String.format("Giá hiện tại: %,.0f ₫", newPrice));

            // Cập nhật người dẫn đầu
            lblLeader.setText("Người giữ giá: " + bidder);

            // Thêm vào lịch sử
            bidHistoryList.getItems().add(0,
                    String.format("💰 %s đặt %,.0f ₫", bidder, newPrice));

            // Hiệu ứng nhấp nháy giá
            lblCurrentPrice.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 18px;");
            new Timeline(new KeyFrame(Duration.seconds(1), e ->
                    lblCurrentPrice.setStyle("-fx-font-size: 18px;")
            )).play();

        } catch (Exception e) {
            System.out.println("[ThisBidding] Lỗi update: " + e.getMessage());
        }
    }

    // ── Timer đếm ngược ───────────────────────────────────────────────────────

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (timeLeft > 0) {
                timeLeft--;
                lblTimer.setText("Còn: " + formatTime(timeLeft));
            } else {
                lblTimer.setText("Còn: Đã kết thúc");
                timer.stop();
                if (btnPlaceBid != null) {
                    btnPlaceBid.setDisable(true);
                    btnPlaceBid.setText("Đã đóng");
                }
            }
        }));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    // ── Lịch sử ───────────────────────────────────────────────────────────────

    @FXML
    public void BiddingHistory(ActionEvent event) {
        // Toggle hiện/ẩn lịch sử — đã hiện sẵn trong ListView
        System.out.println("[History] Lịch sử: " + bidHistoryList.getItems().size() + " lượt");
    }

    // ── Setter — gọi từ Dashboard khi click vào card ──────────────────────────

    /**
     * Set auctionId và tên sản phẩm khi navigate từ Dashboard.
     * Gọi sau khi load FXML:
     *   ThisBiddingController ctrl = loader.getController();
     *   ctrl.setAuction("1001", "Laptop Dell XPS 15");
     */
    public void setAuction(String auctionId, String productName) {
        this.currentAuctionId = auctionId;
        if (lblProductName != null) {
            lblProductName.setText(productName);
        }
        subscribeAuction(auctionId);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}