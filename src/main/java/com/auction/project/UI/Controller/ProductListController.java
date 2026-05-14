package com.auction.project.UI.Controller;

import com.auction.project.Client.NetworkClient;
import com.auction.project.Packets.GsonFactory;
import com.auction.project.Packets.ResponseType;
import com.auction.project.UI.Interface.ViewCleanup;
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

public class ProductListController implements ViewCleanup {

    @FXML private VBox productList;
    @FXML private TextField searchField;
    @FXML private Label lblCount;
    @FXML private Button btnSortPrice;

    private final Gson gson = GsonFactory.create();
    private List<Map<String, Object>> allProducts = new ArrayList<>();
    private String currentSort = "price_asc";

    // Biến lưu Listener để gỡ khi chuyển trang
    private NetworkClient.ResponseListener serverListener;

    @FXML
    public void initialize() {
        loadProducts();
    }

    private void loadProducts() {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client == null || !client.isConnected()) {
            showEmpty("⚠️ Chưa kết nối server");
            return;
        }

        // Gán lambda vào biến
        serverListener = response -> {
            if (response != null && response.getType() == ResponseType.AUCTION_LIST) {
                Platform.runLater(() -> {
                    try {
                        allProducts = gson.fromJson(gson.toJson(response.getData()),
                                new TypeToken<List<Map<String, Object>>>(){}.getType());
                        if (allProducts == null) allProducts = new ArrayList<>();
                        // Chỉ hiện phiên đang RUNNING
                        allProducts = allProducts.stream()
                                .filter(p -> "RUNNING".equals(String.valueOf(p.get("status"))))
                                .collect(Collectors.toList());
                        renderProducts(allProducts);
                    } catch (Exception e) {
                        showEmpty("Lỗi tải dữ liệu");
                    }
                });
            }
        };

        // Đăng ký listener
        client.addResponseListener(serverListener);

        JsonObject req = new JsonObject();
        req.addProperty("action", "GET_AUCTIONS");
        client.sendRequest(gson.toJson(req));
    }

    private void renderProducts(List<Map<String, Object>> products) {
        productList.getChildren().clear();
        if (lblCount != null) lblCount.setText(products.size() + " sản phẩm");

        if (products.isEmpty()) {
            showEmpty("Không có sản phẩm nào đang đấu giá");
            return;
        }

        for (Map<String, Object> product : products) {
            productList.getChildren().add(buildProductRow(product));
        }
    }

    private HBox buildProductRow(Map<String, Object> product) {
        String id     = String.valueOf(((Number) product.get("id")).intValue());
        String name   = String.valueOf(product.getOrDefault("itemName", "Sản phẩm"));
        double price  = product.get("currentPrice") instanceof Number
                ? ((Number) product.get("currentPrice")).doubleValue() : 0;
        String bidder = String.valueOf(product.getOrDefault("leadingBidder", "Chưa có"));

        VBox img = new VBox();
        img.setPrefSize(70, 70);
        img.setStyle("-fx-background-color: #2d1b4e; -fx-background-radius: 8;");
        Label imgLabel = new Label("🖼");
        imgLabel.setStyle("-fx-font-size: 24px;");
        img.setAlignment(Pos.CENTER);
        img.getChildren().add(imgLabel);

        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: #f8f0ff; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label lblBidder = new Label("👤 Người dẫn đầu: " + bidder);
        lblBidder.setStyle("-fx-text-fill: #c39bd3; -fx-font-size: 12px;");

        Label lblStatus = new Label("🔴 Đang diễn ra");
        lblStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        VBox info = new VBox(6, lblName, lblBidder, lblStatus);
        HBox.setHgrow(info, Priority.ALWAYS);

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

            // QUAN TRỌNG: Lưu controller vào node để dọn dẹp sau này
            node.setUserData(ctrl);

            ctrl.setAuction(auctionId, itemName);
            HomeController home = HomeController.getInstance();
            if (home != null) home.setView(node);
        } catch (Exception e) {
            System.out.println("[ProductList] Lỗi navigate: " + e.getMessage());
        }
    }

    @FXML
    public void handleSearch() {
        String kw = searchField.getText().toLowerCase().trim();
        List<Map<String, Object>> filtered = kw.isEmpty() ? allProducts :
                allProducts.stream()
                .filter(p -> String.valueOf(p.getOrDefault("itemName","")).toLowerCase().contains(kw))
                .collect(Collectors.toList());
        renderProducts(filtered);
    }

    @FXML
    public void sortByPrice(ActionEvent e) {
        currentSort = "price_asc";
        allProducts.sort(Comparator.comparingDouble(p ->
                p.get("currentPrice") instanceof Number ? ((Number)p.get("currentPrice")).doubleValue() : 0));
        renderProducts(allProducts);
    }

    @FXML
    public void sortByPriceDesc(ActionEvent e) {
        currentSort = "price_desc";
        allProducts.sort((a, b) -> {
            double pa = a.get("currentPrice") instanceof Number ? ((Number)a.get("currentPrice")).doubleValue() : 0;
            double pb = b.get("currentPrice") instanceof Number ? ((Number)b.get("currentPrice")).doubleValue() : 0;
            return Double.compare(pb, pa);
        });
        renderProducts(allProducts);
    }

    @FXML
    public void sortByTime(ActionEvent e) {
        renderProducts(allProducts);
    }

    private void showEmpty(String msg) {
        productList.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #7f8c9a; -fx-font-size: 14px;");
        productList.getChildren().add(lbl);
        if (lblCount != null) lblCount.setText("0 sản phẩm");
    }

    // ── Dọn dẹp Listener khi rời khỏi trang ──────────────────────────────────
    @Override
    public void cleanup() {
        NetworkClient client = SessionManager.getNetworkClient();
        if (client != null && serverListener != null) {
            client.removeResponseListener(serverListener);
            System.out.println("[ProductList] Đã gỡ Listener.");
        }
    }
}