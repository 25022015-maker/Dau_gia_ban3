package com.auction.project.UI.Controller;

import com.auction.project.Client.NetworkClient;
import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.auction.project.UI.Interface.ViewCleanup;
import com.auction.project.UI.service.SessionManager;
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
 * Kết nối socket qua SessionManager.getNetworkClient().
 */
public class ThisBiddingController implements Initializable, ViewCleanup {
    private NetworkClient.ResponseListener serverListener;

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
     * Được set từ màn hình trước qua setAuction()
     */
    private String currentAuctionId;

    // ── Initialize ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        startTimer();
        registerBidUpdateListener();

        // Đã xóa phần hardcode currentAuctionId = "1001"
        // Việc gán ID và subscribe sẽ được gọi từ DashboardController thông qua hàm setAuction()
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

        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null || !client.isConnected()) {
            System.out.println("[Bid] Chưa kết nối server!");
            return;
        }

        String bidderId = SessionManager.getCurrentUser();
        if (bidderId == null) {
            System.out.println("[Bid] Chưa đăng nhập!");
            return;
        }

        if (currentAuctionId == null) {
            System.out.println("[Bid] Lỗi: Không xác định được ID phiên đấu giá!");
            return;
        }

        // Gửi BidRequest lên server
        JsonObject req = new JsonObject();
        req.addProperty("action", "BID");
        req.addProperty("auctionId", currentAuctionId);
        req.addProperty("bidderId", bidderId);
        req.addProperty("bidAmount", bidAmount);
        client.sendRequest(gson.toJson(req));

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
        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", "SUBSCRIBE");
        req.addProperty("auctionId", auctionId);
        client.sendRequest(gson.toJson(req));
    }

    private void registerBidUpdateListener() {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null) return;

        // Gán lambda vào biến serverListener thay vì truyền trực tiếp
        serverListener = response -> {
            if (response == null) return;

            if (response.getType() == ResponseType.BID_UPDATE) {
                Platform.runLater(() -> handleBidUpdate(response));
            } else if (response.getType() == ResponseType.BID_SUCCESS) {
                Platform.runLater(() -> {
                    System.out.println("[Bid] Đặt giá thành công!");
                    bidHistoryList.getItems().add(0, "✅ Bạn vừa đặt giá thành công");
                });
            } else if (response.getType() == ResponseType.BID_FAILURE) {
                Platform.runLater(() -> {
                    System.out.println("[Bid] Thất bại: " + response.getMessage());
                    bidHistoryList.getItems().add(0, "❌ " + response.getMessage());
                });
            }
        };

        // Đăng ký listener
        client.addResponseListener(serverListener);
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
        // Gọi lệnh subscribe lên Server với đúng ID của sản phẩm này
        subscribeAuction(auctionId);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
    @Override
    public void cleanup() {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client != null && serverListener != null) {
            client.removeResponseListener(serverListener); // GỠ LISTENER!
            System.out.println("[ThisBidding] Đã gỡ Listener, giải phóng bộ nhớ.");
        }
        if (timer != null) {
            timer.stop(); // Dừng luôn đồng hồ đếm ngược
        }
    }
}