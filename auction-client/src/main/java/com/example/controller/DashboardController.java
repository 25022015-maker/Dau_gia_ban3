package com.example.controller;

import com.example.service.ApiClient;
import com.example.service.SessionManager;
import com.example.service.StompClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    @FXML private Label     lblClock;
    @FXML private Label     lblDate;
    @FXML private GridPane  productGrid;
    @FXML private TextField searchField; // mapped via onAction="#handleSearch" — may be null if not wired

    private List<JsonObject> allAuctions = new ArrayList<>();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy");

    @FXML
    public void initialize() {
        syncServerTime();
        startClock();
        StompClient.getInstance().connect();
        loadAuctions();
    }

    // ── Clock ─────────────────────────────────────────────────────────────────

    private void syncServerTime() {
        new Thread(() -> {
            java.time.LocalDateTime serverTime = ApiClient.getServerTime();
            SessionManager.syncServerOffset(serverTime);
        }, "SyncTimeThread").start();
    }

    private void startClock() {
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = SessionManager.serverNow();
            if (lblClock != null) lblClock.setText(now.format(TIME_FMT));
            if (lblDate  != null) lblDate.setText(now.format(DATE_FMT));
        }));
        tl.setCycleCount(Animation.INDEFINITE);
        tl.play();
        LocalDateTime now = SessionManager.serverNow();
        if (lblClock != null) lblClock.setText(now.format(TIME_FMT));
        if (lblDate  != null) lblDate.setText(now.format(DATE_FMT));
    }

    // ── Load data ─────────────────────────────────────────────────────────────

    private void loadAuctions() {
        new Thread(() -> {
            try {
                JsonArray arr = ApiClient.getAuctions();
                allAuctions = new ArrayList<>();
                for (JsonElement el : arr) allAuctions.add(el.getAsJsonObject());
                Platform.runLater(() -> renderGrid(allAuctions));
            } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi tải danh sách: " + e.getMessage()));
            }
        }, "LoadAuctionsThread").start();
    }

    // ── Render grid ───────────────────────────────────────────────────────────

    private void renderGrid(List<JsonObject> list) {
        productGrid.getChildren().clear();
        if (list.isEmpty()) {
            Label lbl = new Label("Không có phiên đấu giá nào.");
            lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14;");
            productGrid.add(lbl, 0, 0);
            return;
        }

        int col = 0, row = 0;
        for (JsonObject a : list) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/user/AuctionCard.fxml"));
                Node card = loader.load();
                ProductCardController ctrl = loader.getController();
                ctrl.setData(a);
                productGrid.add(card, col, row);
                col++;
                if (col == 4) { col = 0; row++; }

                subscribeRealtime(a, ctrl);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void subscribeRealtime(JsonObject auction, ProductCardController ctrl) {
        long id = auction.get("id").getAsLong();
        String topic = "/topic/auction/" + id;
        StompClient.getInstance().subscribe(topic, body -> {
            try {
                JsonObject msg = JsonParser.parseString(body).getAsJsonObject();
                Platform.runLater(() -> ctrl.updateFromWs(msg));
            } catch (Exception ignored) {}
        });
    }

    // ── Filter buttons ────────────────────────────────────────────────────────

    @FXML public void handleFilterAll()       { renderGrid(allAuctions); }

    @FXML public void handleFilterActive()    {
        renderGrid(filter("RUNNING")); }

    @FXML public void handleFilterUpcoming()  {
        renderGrid(filter("PENDING")); }

    @FXML public void handleFilterCompleted() {
        List<JsonObject> done = new ArrayList<>();
        done.addAll(filter("FINISHED"));
        done.addAll(filter("CANCELED"));
        renderGrid(done);
    }

    @FXML public void handleFilterMyAuctions() {
        String me = SessionManager.getUsername();
        List<JsonObject> mine = new ArrayList<>();
        for (JsonObject a : allAuctions) {
            JsonElement sellerEl = a.get("sellerUsername");
            JsonElement winnerEl = a.get("currentWinnerUsername");
            boolean isSeller = sellerEl != null && !sellerEl.isJsonNull()
                    && me.equals(sellerEl.getAsString());
            boolean isWinner = winnerEl != null && !winnerEl.isJsonNull()
                    && me.equals(winnerEl.getAsString());
            if (isSeller || isWinner) mine.add(a);
        }
        renderGrid(mine);
    }

    @FXML public void handleSearch() {
        if (searchField == null) return;
        String kw = searchField.getText().toLowerCase().trim();
        if (kw.isEmpty()) { renderGrid(allAuctions); return; }
        List<JsonObject> res = new ArrayList<>();
        for (JsonObject a : allAuctions) {
            String name = a.has("itemName") ? a.get("itemName").getAsString().toLowerCase() : "";
            if (name.contains(kw)) res.add(a);
        }
        renderGrid(res);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<JsonObject> filter(String status) {
        List<JsonObject> res = new ArrayList<>();
        for (JsonObject a : allAuctions) {
            if (status.equals(a.get("status").getAsString())) res.add(a);
        }
        return res;
    }

    private void showError(String msg) {
        productGrid.getChildren().clear();
        Label lbl = new Label(msg);
        lbl.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13;");
        productGrid.add(lbl, 0, 0);
    }
}