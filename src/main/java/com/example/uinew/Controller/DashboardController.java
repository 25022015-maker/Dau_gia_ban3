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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller cho Dashboard.fxml — được load vào contentArea của MainLayout.
 *
 * KHÔNG extends HomeController vì Dashboard là view con,
 * không phải màn hình độc lập. HomeController quản lý layout chính.
 */
public class DashboardController {

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

    private final Gson gson = GsonFactory.create();
    private final Map<String, AuctionCard> cardMap = new HashMap<>();
    private List<Map<String, Object>> allAuctions = new ArrayList<>();

    // ── Initialize — JavaFX gọi tự động sau khi FXML load ────────────────────

    @FXML
    public void initialize() {
        // Hiện tên user đang đăng nhập
        String user = SessionManager.getCurrentUser();
        if (lblWelcome != null && user != null) {
            lblWelcome.setText("Chào mừng trở lại, " + user + " 🖤");
        }

        loadAuctionList();
        registerBidUpdateListener();
    }

    // ── Load danh sách phiên ──────────────────────────────────────────────────

    private void loadAuctionList() {
        SocketClient client = SessionManager.getSocketClient();
        if (client == null || !client.isConnected()) {
            System.out.println("[Dashboard] Chưa kết nối server.");
            return;
        }
        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUCTIONS");
        client.sendRaw(gson.toJson(req));
    }

    // ── Đăng ký nhận realtime ─────────────────────────────────────────────────

    private void registerBidUpdateListener() {
        SocketClient client = SessionManager.getSocketClient();
        if (client == null) return;

        client.setOnResponse(response -> {
            if (response == null) return;
            if (response.getType() == ResponseType.AUCTION_LIST) {
                Platform.runLater(() -> buildCards(response));
            } else if (response.getType() == ResponseType.BID_UPDATE) {
                Platform.runLater(() -> updateCard(response));
            }
        });
    }

    // ── Build cards ───────────────────────────────────────────────────────────

    private void buildCards(Response response) {
        if (cardsContainer == null) return;
        try {
            allAuctions = gson.fromJson(
                    gson.toJson(response.getData()),
                    new TypeToken<List<Map<String, Object>>>(){}.getType());
            if (allAuctions == null) allAuctions = new ArrayList<>();
            renderCards(allAuctions);
            updateStats(allAuctions);
        } catch (Exception e) {
            System.out.println("[Dashboard] Lỗi parse: " + e.getMessage());
        }
    }

    private void renderCards(List<Map<String, Object>> auctions) {
        cardsContainer.getChildren().clear();
        cardMap.clear();
        for (Map<String, Object> auction : auctions) {
            String auctionId = String.valueOf(((Number) auction.get("id")).intValue());
            String itemName  = getItemName(auction);
            double price     = getDouble(auction, "currentPrice");
            String status    = String.valueOf(auction.get("status"));

            AuctionCard card = new AuctionCard(auctionId, itemName, price, status);
            cardMap.put(auctionId, card);
            cardsContainer.getChildren().add(card.getNode());
            subscribeAuction(auctionId);
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
            String auctionId = String.valueOf(((Number) data.get("auctionId")).intValue());
            double newPrice  = getDouble(data, "currentPrice");
            String bidder    = String.valueOf(data.get("leadingBidder"));
            AuctionCard card = cardMap.get(auctionId);
            if (card != null) card.updatePrice(newPrice, bidder);
        } catch (Exception e) {
            System.out.println("[Dashboard] Lỗi updateCard: " + e.getMessage());
        }
    }

    // ── Subscribe ─────────────────────────────────────────────────────────────

    private void subscribeAuction(String auctionId) {
        SocketClient client = SessionManager.getSocketClient();
        if (client == null) return;
        JsonObject req = new JsonObject();
        req.addProperty("action", "SUBSCRIBE");
        req.addProperty("auctionId", auctionId);
        client.sendRaw(gson.toJson(req));
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
        String activeStyle = "-fx-background-color: #6c3483; -fx-text-fill: white; -fx-background-radius: 16; -fx-padding: 5 14;";
        if (btnAll != null)      btnAll.setStyle(normal);
        if (btnLive != null)     btnLive.setStyle(normal);
        if (btnUpcoming != null) btnUpcoming.setStyle(normal);
        if (active != null)      active.setStyle(activeStyle);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0;
    }

    private String getItemName(Map<String, Object> auction) {
        try {
            Map<String, Object> item = gson.fromJson(
                    gson.toJson(auction.get("item")),
                    new TypeToken<Map<String, Object>>(){}.getType());
            if (item != null && item.get("name") != null) return String.valueOf(item.get("name"));
        } catch (Exception ignored) {}
        return "Sản phẩm đấu giá";
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

            root = new VBox(8, imgPane, lblName, lblPriceTitle,
                    lblPrice, lblBidder, lblStatus, lblTimer);
            root.setPrefSize(220, 210);
            root.setStyle(
                    "-fx-background-color: #16213e;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: #2d2d4e;" +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 1;" +
                            "-fx-padding: 14;");

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
}