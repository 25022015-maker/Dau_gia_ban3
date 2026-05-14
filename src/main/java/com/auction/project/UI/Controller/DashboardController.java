package com.auction.project.UI.Controller;

import com.auction.project.Client.NetworkClient;
import com.auction.project.Client.SocketClient;
import com.auction.project.DAO.DatabaseConnection;
import com.auction.project.Entities.Auction;
import com.auction.project.Entities.Product;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController implements ViewCleanup {

    @FXML private Label lblWelcome;
    @FXML private Label statLive;
    @FXML private Label statUpcoming;
    @FXML private Label statWatching;
    @FXML private Label statWon;
    @FXML private TextField searchField;
    @FXML private FlowPane cardsContainer;
    @FXML private Button btnAll;
    @FXML private Button btnLive;
    @FXML private Button btnUpcoming;

    private NetworkClient.ResponseListener serverListener;
    private volatile boolean isDestroyed = false;
    private final Gson gson = GsonFactory.create();
    private final Map<String, AuctionCard> cardMap = new HashMap<>();
    private List<Map<String, Object>> allAuctions = new ArrayList<>();

    // ── Initialize ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        // Hiện tên user ngay lập tức
        String user = SessionManager.getCurrentUser();
        if (lblWelcome != null && user != null) {
            lblWelcome.setText("Chào mừng trở lại, " + user + " 🖤");
        }

        // Đăng ký listener trước
        registerBidUpdateListener();

        // Thử kết nối và load data — retry tối đa 10 lần (5 giây)
        // Vì socket connect trong thread riêng, cần đợi nó xong
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                NetworkClient client = SessionManager.getNetworkClient();
                if (client != null && client.isConnected()) {
                    // Socket đã sẵn sàng — gửi request
                    Platform.runLater(this::loadAuctionList);
                    return;
                }
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
            System.out.println("[Dashboard] Timeout: không kết nối được server.");
        }, "DashboardRetryThread").start();
    }

    // ── Load danh sách phiên ──────────────────────────────────────────────────

    private void loadAuctionList() {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null || !client.isConnected()) {
            System.out.println("[Dashboard] Chưa kết nối server.");
            return;
        }
        System.out.println("[Dashboard] Gửi GET_AUCTIONS...");
        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUCTIONS");
        client.sendRequest(gson.toJson(req));
    }

    // ── Đăng ký nhận realtime ─────────────────────────────────────────────────

    private void registerBidUpdateListener() {
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                if (isDestroyed) return; // Nếu đã chuyển trang thì hủy thread

                NetworkClient client = SessionManager.getNetworkClient();
                if (client != null) {
                    serverListener = response -> {
                        if (response == null) return;
                        if (response.getType() == ResponseType.AUCTION_LIST) {
                            Platform.runLater(() -> buildCards(response));
                        } else if (response.getType() == ResponseType.BID_UPDATE) {
                            Platform.runLater(() -> updateCard(response));
                        }
                    };

                    if (!isDestroyed) {
                        client.addResponseListener(serverListener);
                        System.out.println("[Dashboard] Đã đăng ký listener.");
                    }
                    return;
                }
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
        }, "ListenerRetryThread").start();
    }

    // ── Build cards ───────────────────────────────────────────────────────────

    private void buildCards(Response response) {
        if (cardsContainer == null) return;
        try {
            allAuctions = gson.fromJson(
                    gson.toJson(response.getData()),
                    new TypeToken<List<Map<String, Object>>>(){}.getType());
            if (allAuctions == null) allAuctions = new ArrayList<>();
            System.out.println("[Dashboard] Build " + allAuctions.size() + " cards.");
            renderCards(allAuctions);
            updateStats(allAuctions);
        } catch (Exception e) {
            System.out.println("[Dashboard] Lỗi parse: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renderCards(List<Map<String, Object>> auctions) {
        cardsContainer.getChildren().clear();
        cardMap.clear();

        if (auctions.isEmpty()) {
            Label empty = new Label("Không có phiên đấu giá nào.");
            empty.setStyle("-fx-text-fill: #7f8c9a; -fx-font-size: 14px;");
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (Map<String, Object> auction : auctions) {
            try {
                String auctionId = String.valueOf(((Number) auction.get("id")).intValue());
                String itemName  = getItemName(auction);
                double price     = getDouble(auction, "currentPrice");
                String status    = String.valueOf(auction.get("status"));

                AuctionCard card = new AuctionCard(auctionId, itemName, price, status);
                cardMap.put(auctionId, card);

                // Click vào card → chuyển sang màn hình đặt giá
                final String fId   = auctionId;
                final String fName = itemName;
                card.getNode().setOnMouseClicked(e -> navigateToThisBidding(fId, fName));

                cardsContainer.getChildren().add(card.getNode());
                subscribeAuction(auctionId);
            } catch (Exception e) {
                System.out.println("[Dashboard] Lỗi tạo card: " + e.getMessage());
            }
        }
    }

    private void navigateToThisBidding(String auctionId, String itemName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/uinew/ThisBidding.fxml"));
            Node node = loader.load();
            ThisBiddingController ctrl = loader.getController();
            node.setUserData(ctrl);
            ctrl.setAuction(auctionId, itemName);
            HomeController home = HomeController.getInstance();
            if (home != null) home.setView(node);
        } catch (Exception e) {
            System.out.println("[Dashboard] Lỗi navigate: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStats(List<Map<String, Object>> auctions) {
        long live     = auctions.stream().filter(a -> "RUNNING".equals(String.valueOf(a.get("status")))).count();
        long upcoming = auctions.stream().filter(a -> "OPEN".equals(String.valueOf(a.get("status")))).count();
        if (statLive != null)     statLive.setText(String.valueOf(live));
        if (statUpcoming != null) statUpcoming.setText(String.valueOf(upcoming));
        if (statWatching != null) statWatching.setText(String.valueOf(auctions.size()));
        if (statWon != null)      statWon.setText("0");
    }

    // ── Update card realtime ──────────────────────────────────────────────────

    private void updateCard(Response response) {
        try {
            Map<String, Object> data = gson.fromJson(
                    gson.toJson(response.getData()),
                    new TypeToken<Map<String, Object>>(){}.getType());
            if (data == null) return;

            // Lấy ID an toàn: Hỗ trợ cả 2 định dạng trả về từ Server ("auctionId" hoặc "id")
            Object idObj = data.get("auctionId");
            if (idObj == null) {
                idObj = data.get("id");
            }

            // Nếu vẫn null thì bỏ qua để tránh crash
            if (idObj == null) {
                System.out.println("[Dashboard] Không tìm thấy ID trong gói tin BID_UPDATE");
                return;
            }

            String auctionId = String.valueOf(((Number) idObj).intValue());
            double newPrice  = getDouble(data, "currentPrice");
            String bidder    = String.valueOf(data.get("leadingBidder"));

            AuctionCard card = cardMap.get(auctionId);
            if (card != null) card.updatePrice(newPrice, bidder);

        } catch (Exception e) {
            System.out.println("[Dashboard] Lỗi updateCard: " + e.getMessage());
            e.printStackTrace(); // In ra chi tiết lỗi nếu có để dễ debug
        }
    }

    // ── Subscribe ─────────────────────────────────────────────────────────────

    private void subscribeAuction(String auctionId) {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", "SUBSCRIBE");
        req.addProperty("auctionId", auctionId);
        client.sendRequest(gson.toJson(req));
    }

    // ── Filter buttons ────────────────────────────────────────────────────────

    @FXML
    public void filterAll(ActionEvent event) {
        setActiveButton(btnAll);
        renderCards(allAuctions);
    }

    @FXML
    public void filterLive(ActionEvent event) {
        setActiveButton(btnLive);
        renderCards(allAuctions.stream()
                .filter(a -> "RUNNING".equals(String.valueOf(a.get("status"))))
                .collect(Collectors.toList()));
    }

    @FXML
    public void filterUpcoming(ActionEvent event) {
        setActiveButton(btnUpcoming);
        renderCards(allAuctions.stream()
                .filter(a -> "OPEN".equals(String.valueOf(a.get("status"))))
                .collect(Collectors.toList()));
    }

    @FXML
    public void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        if (keyword.isEmpty()) { renderCards(allAuctions); return; }
        renderCards(allAuctions.stream()
                .filter(a -> getItemName(a).toLowerCase().contains(keyword))
                .collect(Collectors.toList()));
    }

    private void setActiveButton(Button active) {
        String normal = "-fx-background-color: #2d1b4e; -fx-text-fill: #c39bd3; -fx-background-radius: 16; -fx-padding: 5 14;";
        String act    = "-fx-background-color: #6c3483; -fx-text-fill: white; -fx-background-radius: 16; -fx-padding: 5 14;";
        if (btnAll != null)      btnAll.setStyle(normal);
        if (btnLive != null)     btnLive.setStyle(normal);
        if (btnUpcoming != null) btnUpcoming.setStyle(normal);
        if (active != null)      active.setStyle(act);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0;
    }

    private String getItemName(Map<String, Object> auction) {
        if (auction.containsKey("itemName") && auction.get("itemName") != null) {
            return String.valueOf(auction.get("itemName"));
        }
        return "Sản phẩm đấu giá"; // Fallback nếu bị null
    }

    // ── Inner class: AuctionCard ──────────────────────────────────────────────

    private static class AuctionCard {
        private final VBox root;
        private final Label lblPrice;
        private final Label lblTimer;
        private final Label lblBidder;
        private int timeLeft = 3600;
        private Timeline timer;

        public AuctionCard(String auctionId, String itemName, double currentPrice, String status) {
            Pane imgPane = new Pane();
            imgPane.setPrefSize(192, 80);
            imgPane.setStyle("-fx-background-color: #2d1b4e; -fx-background-radius: 7;");

            Label lblName = new Label(itemName);
            lblName.setStyle("-fx-text-fill: #f8f0ff; -fx-font-weight: bold; -fx-font-size: 13px;");
            lblName.setWrapText(true);

            Label lblPriceTitle = new Label("Giá hiện tại");
            lblPriceTitle.setStyle("-fx-text-fill: #7f8c9a; -fx-font-size: 10px;");

            lblPrice = new Label(formatPrice(currentPrice));
            lblPrice.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 17px; -fx-font-weight: bold;");

            lblBidder = new Label("");
            lblBidder.setStyle("-fx-text-fill: #c39bd3; -fx-font-size: 10px;");

            String badgeText  = "RUNNING".equals(status) ? "🔴 Đang diễn ra" : "⏳ Sắp bắt đầu";
            String badgeColor = "RUNNING".equals(status) ? "#e74c3c" : "#f39c12";
            Label lblStatus = new Label(badgeText);
            lblStatus.setStyle("-fx-text-fill: " + badgeColor + "; -fx-font-size: 10px;");

            lblTimer = new Label("⏱ 01:00:00");
            lblTimer.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

            Label lblHint = new Label("👆 Click để đặt giá");
            lblHint.setStyle("-fx-text-fill: #555; -fx-font-size: 9px;");

            root = new VBox(8, imgPane, lblName, lblPriceTitle,
                    lblPrice, lblBidder, lblStatus, lblTimer, lblHint);
            root.setPrefSize(220, 220);
            root.setStyle(
                    "-fx-background-color: #16213e;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: #2d2d4e;" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 1;" +
                            "-fx-padding: 14;" +
                            "-fx-cursor: hand;");

            root.setOnMouseEntered(e -> root.setStyle(
                    "-fx-background-color: #1e2a45;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: #6c3483;" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 2;" +
                            "-fx-padding: 14;" +
                            "-fx-cursor: hand;"));
            root.setOnMouseExited(e -> root.setStyle(
                    "-fx-background-color: #16213e;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: #2d2d4e;" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 1;" +
                            "-fx-padding: 14;" +
                            "-fx-cursor: hand;"));

            if ("RUNNING".equals(status)) startTimer();
            else lblTimer.setText("⏳ Chưa bắt đầu");
        }

        public void updatePrice(double newPrice, String bidder) {
            lblPrice.setText(formatPrice(newPrice));
            if (bidder != null && !bidder.isEmpty() && !"null".equals(bidder)) {
                lblBidder.setText("👤 " + bidder);
            }
            lblPrice.setStyle("-fx-text-fill: #2ecc71; -fx-font-size: 17px; -fx-font-weight: bold;");
            new Timeline(new KeyFrame(Duration.seconds(1), e ->
                    lblPrice.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 17px; -fx-font-weight: bold;")
            )).play();
        }

        public Node getNode() { return root; }

        private void startTimer() {
            timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                if (timeLeft > 0) { timeLeft--; lblTimer.setText("⏱ còn " + formatTime(timeLeft)); }
                else { lblTimer.setText("⏱ Đã kết thúc"); timer.stop(); }
            }));
            timer.setCycleCount(Animation.INDEFINITE);
            timer.play();
        }

        private String formatPrice(double price) { return String.format("%,.0f ₫", price); }
        private String formatTime(int s) { return String.format("%02d:%02d:%02d", s/3600, (s%3600)/60, s%60); }
    }
    @Override
    public void cleanup() {
        isDestroyed = true;
        NetworkClient client = SessionManager.getNetworkClient();
        if (client != null && serverListener != null) {
            client.removeResponseListener(serverListener);
            System.out.println("[Dashboard] Đã gỡ Listener.");
        }
    }
}