package com.auction.project.UI.Controller;

import com.auction.project.Client.NetworkClient;
import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.ResponseType;
import com.auction.project.UI.service.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemListController {

    @FXML private VBox ItemList;
    @FXML private TextField searchField;
    @FXML private Label lblCount;
    @FXML private Button btnSortPrice;

    private final Gson gson = GsonFactory.create();
    private List<Map<String, Object>> allItems = new ArrayList<>();
    private String currentSort = "price_asc";

    @FXML
    public void initialize() {
        loadItems();
    }

    private void loadItems() {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null || !client.isConnected()) {
            showEmpty("⚠️ Chưa kết nối server");
            return;
        }

        client.addResponseListener(response -> {
            if (response != null && response.getType() == ResponseType.AUCTION_LIST) {
                Platform.runLater(() -> {
                    try {
                        allItems = gson.fromJson(gson.toJson(response.getData()),
                                new TypeToken<List<Map<String, Object>>>(){}.getType());
                        if (allItems == null) allItems = new ArrayList<>();
                        // Chỉ hiện phiên đang RUNNING
                        allItems = allItems.stream()
                                .filter(p -> "RUNNING".equals(String.valueOf(p.get("status"))))
                                .collect(Collectors.toList());
                        renderItems(allItems);
                    } catch (Exception e) {
                        showEmpty("Lỗi tải dữ liệu");
                    }
                });
            }
        });

        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUCTIONS");
        client.sendRequest(gson.toJson(req));
    }

    private void renderItems(List<Map<String, Object>> Items) {
        ItemList.getChildren().clear();
        if (lblCount != null) lblCount.setText(Items.size() + " sản phẩm");

        if (Items.isEmpty()) {
            showEmpty("Không có sản phẩm nào đang đấu giá");
            return;
        }

        for (Map<String, Object> Item : Items) {
            ItemList.getChildren().add(buildItemRow(Item));
        }
    }

    private HBox buildItemRow(Map<String, Object> Item) {
        String id     = String.valueOf(((Number) Item.get("id")).intValue());
        String name   = String.valueOf(Item.getOrDefault("itemName", "Sản phẩm"));
        double price  = Item.get("currentPrice") instanceof Number
                ? ((Number) Item.get("currentPrice")).doubleValue() : 0;
        String bidder = String.valueOf(Item.getOrDefault("leadingBidder", "Chưa có"));

        // Image placeholder
        VBox img = new VBox();
        img.setPrefSize(70, 70);
        img.setStyle("-fx-background-color: #2d1b4e; -fx-background-radius: 8;");
        Label imgLabel = new Label("🖼");
        imgLabel.setStyle("-fx-font-size: 24px;");
        img.setAlignment(Pos.CENTER);
        img.getChildren().add(imgLabel);

        // Info
        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: #f8f0ff; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label lblBidder = new Label("👤 Người dẫn đầu: " + bidder);
        lblBidder.setStyle("-fx-text-fill: #c39bd3; -fx-font-size: 12px;");

        Label lblStatus = new Label("🔴 Đang diễn ra");
        lblStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        VBox info = new VBox(6, lblName, lblBidder, lblStatus);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Price + button
        Label lblPrice = new Label(String.format("%,.0f ₫", price));
        lblPrice.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 18px; -fx-font-weight: bold;");

        Button btnBid = new Button("Đặt giá →");
        btnBid.setStyle("-fx-background-color: #6c3483; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        btnBid.setOnAction(e -> navigateToBidding(id, name));

        VBox priceBox = new VBox(8, lblPrice, btnBid);
        priceBox.setAlignment(Pos.CENTER_RIGHT);

        HBox row = new HBox(16, img, info, priceBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 16, 14, 16));
        row.setStyle("-fx-background-color: #16213e; -fx-background-radius: 10;" +
                "-fx-border-color: #2d2d4e; -fx-border-radius: 10; -fx-border-width: 1;");

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #1e2a45; -fx-background-radius: 10;" +
                "-fx-border-color: #6c3483; -fx-border-radius: 10; -fx-border-width: 2;"));
        row.setOnMouseExited(e -> row.setStyle("-fx-background-color: #16213e; -fx-background-radius: 10;" +
                "-fx-border-color: #2d2d4e; -fx-border-radius: 10; -fx-border-width: 1;"));

        return row;
    }

    private void navigateToBidding(String auctionId, String itemName) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/uinew/ThisBidding.fxml"));
            Node node = loader.load();
            ThisBiddingController ctrl = loader.getController();
            ctrl.setAuction(auctionId, itemName);
            HomeController home = HomeController.getInstance();
            if (home != null) home.setView(node);
        } catch (Exception e) {
            System.out.println("[ItemList] Lỗi navigate: " + e.getMessage());
        }
    }

    // ── Search & Sort ─────────────────────────────────────────────────────────

    @FXML
    public void handleSearch() {
        String kw = searchField.getText().toLowerCase().trim();
        List<Map<String, Object>> filtered = kw.isEmpty() ? allItems :
                allItems.stream()
                        .filter(p -> String.valueOf(p.getOrDefault("itemName","")).toLowerCase().contains(kw))
                        .collect(Collectors.toList());
        renderItems(filtered);
    }

    @FXML
    public void sortByPrice(ActionEvent e) {
        currentSort = "price_asc";
        allItems.sort(Comparator.comparingDouble(p ->
                p.get("currentPrice") instanceof Number ? ((Number)p.get("currentPrice")).doubleValue() : 0));
        renderItems(allItems);
    }

    @FXML
    public void sortByPriceDesc(ActionEvent e) {
        currentSort = "price_desc";
        allItems.sort((a, b) -> {
            double pa = a.get("currentPrice") instanceof Number ? ((Number)a.get("currentPrice")).doubleValue() : 0;
            double pb = b.get("currentPrice") instanceof Number ? ((Number)b.get("currentPrice")).doubleValue() : 0;
            return Double.compare(pb, pa);
        });
        renderItems(allItems);
    }

    @FXML
    public void sortByTime(ActionEvent e) {
        // Giữ thứ tự server trả về (đã sort theo endTime)
        renderItems(allItems);
    }

    private void showEmpty(String msg) {
        ItemList.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #7f8c9a; -fx-font-size: 14px;");
        ItemList.getChildren().add(lbl);
        if (lblCount != null) lblCount.setText("0 sản phẩm");
    }
}