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
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

public class AuctionRoomController {

    // ── Info panel ────────────────────────────────────────────────────────────
    @FXML private Label     lblAuctionName;
    @FXML private Label     lblStatus;
    @FXML private Label     lblCurrentPrice;
    @FXML private Label     lblMinBid;
    @FXML private Label     lblSeller;
    @FXML private Label     lblEndTime;
    @FXML private Label     lblTotalBids;
    @FXML private Label     lblItemDetails;
    @FXML private Label     lblCurrentWinner;   // người đang dẫn đầu
    @FXML private ImageView imgItem;

    // ── Bid panel (Bidder) ────────────────────────────────────────────────────
    @FXML private TextField txtBidInput;
    @FXML private Label     lblCurrentPrice2;   // dòng nhắc trong form bid
    @FXML private Label     lblMinBidStep;
    @FXML private Label     lblPlacedBid;

    // ── Auto-bid panel ────────────────────────────────────────────────────────
    @FXML private TextField  txtAutoMaxPrice;
    @FXML private TextField  txtAutoBidStep;
    @FXML private CheckBox   chkEnableAutoBid;
    @FXML private Label      lblAutoBidMessage;

    // ── Countdown ─────────────────────────────────────────────────────────────
    @FXML private Label       lblCountdown;

    // ── Seller panel ──────────────────────────────────────────────────────────
    @FXML private Button     btnCancelAuction;

    // ── Admin view ────────────────────────────────────────────────────────────
    @FXML private HBox       bidAndAutoBidBox;
    @FXML private TabPane    tabPaneMain;
    @FXML private Label      lblAntiSnipeNotif;

    // ── Bid history table ─────────────────────────────────────────────────────
    @FXML private TableView<JsonObject>          tableBidHistory;
    @FXML private TableColumn<JsonObject,String> colBidTime;
    @FXML private TableColumn<JsonObject,String> colBidder;
    @FXML private TableColumn<JsonObject,String> colAmount;
    @FXML private TableColumn<JsonObject,String> colBidType;

    // ── Price chart ───────────────────────────────────────────────────────────
    @FXML private AreaChart<String, Number>  priceChart;
    @FXML private CategoryAxis               chartXAxis;
    @FXML private NumberAxis                 chartYAxis;
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    private long auctionId;
    private long currentPrice;
    private long minBid;
    private final ObservableList<JsonObject> bidHistory     = FXCollections.observableArrayList();
    private final ObservableList<JsonObject> participants   = FXCollections.observableArrayList();

    private LocalDateTime endTimeValue;
    private Timeline countdownTimeline;
    private Timeline antiSnipeHideTimer;

    // ── Initialize ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTable();
        setupChart();
        if (btnCancelAuction != null) {
            btnCancelAuction.setVisible(SessionManager.isSeller());
        }
        if (SessionManager.isAdmin()) {
            setupAdminView();
        }
    }

    public void setAuctionId(long id) {
        this.auctionId = id;
        loadAuction();
        subscribeWebSocket();
    }

    // ── Load & display auction ────────────────────────────────────────────────

    private void loadAuction() {
        new Thread(() -> {
            try {
                JsonObject a = ApiClient.getAuction(auctionId);
                Platform.runLater(() -> displayAuction(a));

                JsonArray hist = ApiClient.getBidHistory(auctionId);
                Platform.runLater(() -> loadBidHistory(hist));
            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("Lỗi: " + e.getMessage()));
            }
        }, "LoadAuctionThread").start();
    }

    private void displayAuction(JsonObject a) {
        currentPrice = a.get("currentPrice").getAsLong();
        minBid       = a.get("minBid").getAsLong();

        lblAuctionName.setText(a.get("itemName").getAsString());
        lblCurrentPrice.setText(formatPrice(currentPrice));
        lblMinBid.setText("Bước giá tối thiểu: " + formatPrice(minBid));
        if (lblCurrentPrice2 != null) lblCurrentPrice2.setText("Giá hiện tại: " + formatPrice(currentPrice));
        if (lblMinBidStep    != null) lblMinBidStep.setText("+ " + formatPrice(minBid));

        String status = a.get("status").getAsString();
        updateStatus(status);

        JsonElement sellerEl = a.get("sellerUsername");
        if (lblSeller != null && sellerEl != null && !sellerEl.isJsonNull())
            lblSeller.setText("Người bán: " + sellerEl.getAsString());

        JsonElement winnerEl = a.get("currentWinnerUsername");
        if (lblCurrentWinner != null) {
            lblCurrentWinner.setText(winnerEl != null && !winnerEl.isJsonNull()
                    ? "Dẫn đầu: " + winnerEl.getAsString()
                    : "Dẫn đầu: —");
        }

        String endTime = a.get("endTime").getAsString();
        if (lblEndTime != null)
            lblEndTime.setText("Kết thúc: " + endTime.replace("T", " ").substring(0, Math.min(16, endTime.length())));
        startCountdown(endTime, status);

        if (lblTotalBids != null)
            lblTotalBids.setText("Lượt đặt: " + a.get("totalBids").getAsLong());

        JsonElement detailEl = a.get("itemDetails");
        if (lblItemDetails != null && detailEl != null && !detailEl.isJsonNull())
            lblItemDetails.setText(detailEl.getAsString());

        String img = getString(a, "imageBase64", null);
        if (imgItem != null && img != null && !img.isEmpty()) {
            try {
                byte[] bytes = Base64.getDecoder().decode(img);
                imgItem.setImage(new Image(new ByteArrayInputStream(bytes)));
            } catch (Exception ignored) {}
        }

        // Ẩn form bid nếu phiên không RUNNING
        if (txtBidInput != null)
            txtBidInput.setDisable(!"RUNNING".equals(status));
    }

    private void loadBidHistory(JsonArray arr) {
        bidHistory.clear();
        for (JsonElement el : arr) bidHistory.add(el.getAsJsonObject());
        if (tableBidHistory != null) tableBidHistory.setItems(bidHistory);
        rebuildChart();
        rebuildParticipants();
    }

    // ── WebSocket realtime ────────────────────────────────────────────────────

    private void subscribeWebSocket() {
        StompClient.getInstance().connect();
        StompClient.getInstance().subscribe("/topic/auction/" + auctionId, body -> {
            try {
                JsonObject msg = JsonParser.parseString(body).getAsJsonObject();
                Platform.runLater(() -> handleBidUpdate(msg));
            } catch (Exception ignored) {}
        });
    }

    private void handleBidUpdate(JsonObject msg) {
        if (msg.has("newPrice")) {
            currentPrice = msg.get("newPrice").getAsLong();
            lblCurrentPrice.setText(formatPrice(currentPrice));
            if (lblCurrentPrice2 != null) lblCurrentPrice2.setText("Giá hiện tại: " + formatPrice(currentPrice));
        }
        if (msg.has("status"))  updateStatus(msg.get("status").getAsString());
        if (msg.has("endTime")) {
            String et = msg.get("endTime").getAsString();
            if (lblEndTime != null)
                lblEndTime.setText("Kết thúc: " + et.replace("T", " ").substring(0, Math.min(16, et.length())));
            String currentStatus = getString(msg, "status", "RUNNING");
            startCountdown(et, currentStatus);
        }
        if (msg.has("totalBids") && lblTotalBids != null)
            lblTotalBids.setText("Lượt đặt: " + msg.get("totalBids").getAsLong());

        if (msg.has("winnerUsername") && lblCurrentWinner != null) {
            String winner = getString(msg, "winnerUsername", "—");
            String type   = getString(msg, "bidType", "");
            String tag    = "AUTO".equals(type) ? " (tự động)" : "";
            lblCurrentWinner.setText("Dẫn đầu: " + winner + tag);
        }

        // Thêm ngay hàng mới vào đầu bảng từ dữ liệu WS — không cần REST
        appendBidRow(msg);

        // Thông báo anti-snipe
        if (msg.has("antiSnipeTriggered") && msg.get("antiSnipeTriggered").getAsBoolean()) {
            String who = getString(msg, "antiSnipeUsername", "?");
            showAntiSnipeNotif(who);
        }
    }

    private void appendBidRow(JsonObject msg) {
        if (!msg.has("newPrice") || !msg.has("winnerUsername")) return;
        JsonObject row = new JsonObject();
        row.addProperty("bidderUsername", getString(msg, "winnerUsername", "?"));
        row.addProperty("bidderId",       getLong(msg, "winnerId"));
        row.addProperty("amount",         msg.get("newPrice").getAsLong());
        row.addProperty("bidType",        getString(msg, "bidType", "MANUAL"));
        String t = getString(msg, "bidTime", "");
        row.addProperty("bidTime", t.isEmpty() ? SessionManager.serverNow().toString() : t);
        bidHistory.add(0, row);
        rebuildParticipants();
        if (tableBidHistory != null) tableBidHistory.scrollTo(0);

        // Thêm điểm mới vào cuối chart (real-time)
        if (priceChart != null) {
            int next = priceSeries.getData().size() + 1;
            long amount = msg.get("newPrice").getAsLong();
            priceSeries.getData().add(new XYChart.Data<>("Lần " + next, amount));
        }
    }

    // ── Place bid ─────────────────────────────────────────────────────────────

    @FXML
    public void handlePlaceBid() {
        if (lblPlacedBid != null) lblPlacedBid.setText("");
        String text = txtBidInput.getText().trim().replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            if (lblPlacedBid != null) lblPlacedBid.setText("Vui lòng nhập số tiền");
            return;
        }
        long amount = Long.parseLong(text);

        new Thread(() -> {
            try {
                ApiClient.placeBid(auctionId, amount);
                Platform.runLater(() -> {
                    txtBidInput.clear();
                    if (lblPlacedBid != null) {
                        lblPlacedBid.setText("✅ Đặt giá thành công!");
                        lblPlacedBid.setStyle("-fx-text-fill: #2ecc71;");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblPlacedBid != null) {
                        lblPlacedBid.setText(e.getMessage());
                        lblPlacedBid.setStyle("-fx-text-fill: #e74c3c;");
                    }
                });
            }
        }, "PlaceBidThread").start();
    }

    // ── Auto-bid ──────────────────────────────────────────────────────────────

    @FXML
    public void handleToggleAutoBid() {
        if (chkEnableAutoBid == null || !chkEnableAutoBid.isSelected()) return;
        if (lblAutoBidMessage != null) lblAutoBidMessage.setText("");
        try {
            String maxText  = txtAutoMaxPrice.getText().trim().replaceAll("[^0-9]", "");
            String stepText = txtAutoBidStep.getText().trim().replaceAll("[^0-9]", "");
            if (maxText.isEmpty() || stepText.isEmpty()) {
                if (lblAutoBidMessage != null) lblAutoBidMessage.setText("Nhập đầy đủ thông tin");
                return;
            }
            long maxBid    = Long.parseLong(maxText);
            long increment = Long.parseLong(stepText);
            if (increment < minBid) {
                if (lblAutoBidMessage != null)
                    lblAutoBidMessage.setText("Bước nhảy phải >= bước giá tối thiểu (" + formatPrice(minBid) + ")");
                return;
            }

            new Thread(() -> {
                try {
                    ApiClient.registerAutoBid(auctionId, maxBid, increment);
                    Platform.runLater(() -> {
                        if (lblAutoBidMessage != null) {
                            lblAutoBidMessage.setText("✅ Đã kích hoạt auto-bid");
                            lblAutoBidMessage.setStyle("-fx-text-fill: #2ecc71;");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        if (lblAutoBidMessage != null) {
                            lblAutoBidMessage.setText(e.getMessage());
                            lblAutoBidMessage.setStyle("-fx-text-fill: #e74c3c;");
                        }
                        if (chkEnableAutoBid != null) chkEnableAutoBid.setSelected(false);
                    });
                }
            }, "AutoBidThread").start();
        } catch (NumberFormatException e) {
            if (lblAutoBidMessage != null) lblAutoBidMessage.setText("Số tiền không hợp lệ");
        }
    }

    // ── Seller: hủy phiên ────────────────────────────────────────────────────

    @FXML
    public void handleCancelAuction() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Bạn có chắc muốn hủy phiên đấu giá này?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        ApiClient.cancelAuction(auctionId);
                        Platform.runLater(() -> lblStatus.setText("❌ Đã hủy"));
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                new Alert(Alert.AlertType.ERROR, e.getMessage()).show());
                    }
                }).start();
            }
        });
    }

    // ── Quay lại dashboard ────────────────────────────────────────────────────

    @FXML
    public void handleBack() {
        if (countdownTimeline != null) countdownTimeline.stop();
        StompClient.getInstance().unsubscribe("/topic/auction/" + auctionId);
        MainLayoutController main = MainLayoutController.getInstance();
        if (main != null) main.loadView("/com/example/user/Dashboard.fxml");
    }

    // ── Chart setup ───────────────────────────────────────────────────────────

    private void setupChart() {
        if (priceChart == null) return;
        priceSeries.setName("Giá đặt");
        priceChart.getData().add(priceSeries);
        priceChart.setLegendVisible(false);
        priceChart.setCreateSymbols(true);
        if (chartYAxis != null) {
            chartYAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(chartYAxis) {
                @Override public String toString(Number value) {
                    long v = value.longValue();
                    if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
                    if (v >= 1_000)     return String.format("%.0fK", v / 1_000.0);
                    return String.valueOf(v);
                }
            });
        }
    }

    private void rebuildChart() {
        if (priceChart == null) return;
        priceSeries.getData().clear();
        List<JsonObject> asc = new ArrayList<>(bidHistory);
        Collections.reverse(asc);
        for (int i = 0; i < asc.size(); i++) {
            long amount = getLong(asc.get(i), "amount");
            priceSeries.getData().add(new XYChart.Data<>("Lần " + (i + 1), amount));
        }
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void setupTable() {
        if (tableBidHistory == null) return;
        if (colBidTime != null) colBidTime.setCellValueFactory(c -> {
            String t = getString(c.getValue(), "bidTime", "");
            return new SimpleStringProperty(t.replace("T", " ").substring(0, Math.min(16, t.length())));
        });
        if (colBidder  != null) colBidder.setCellValueFactory(c ->
                new SimpleStringProperty(getString(c.getValue(), "bidderUsername", "")));
        if (colAmount  != null) colAmount.setCellValueFactory(c ->
                new SimpleStringProperty(formatPrice(getLong(c.getValue(), "amount"))));
        if (colBidType != null) colBidType.setCellValueFactory(c ->
                new SimpleStringProperty(getString(c.getValue(), "bidType", "")));
        tableBidHistory.setItems(bidHistory);
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private void startCountdown(String endTimeStr, String status) {
        if (lblCountdown == null) return;
        if ("FINISHED".equals(status) || "CANCELED".equals(status)) return;

        try {
            String normalized = endTimeStr.length() > 19 ? endTimeStr.substring(0, 19) : endTimeStr;
            endTimeValue = LocalDateTime.parse(normalized);
        } catch (Exception e) {
            lblCountdown.setText("--:--:--");
            return;
        }

        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdown()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
        updateCountdown();
    }

    private void updateCountdown() {
        if (endTimeValue == null || lblCountdown == null) return;
        long remaining = java.time.Duration.between(SessionManager.serverNow(), endTimeValue).getSeconds();

        if (remaining <= 0) {
            lblCountdown.setText("00:00:00");
            lblCountdown.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 22;");
            if (countdownTimeline != null) countdownTimeline.stop();
            return;
        }

        long hours   = remaining / 3600;
        long minutes = (remaining % 3600) / 60;
        long seconds = remaining % 60;
        lblCountdown.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

        if (remaining <= 60) {
            lblCountdown.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 22;");
        } else if (remaining <= 300) {
            lblCountdown.setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold; -fx-font-size: 22;");
        } else {
            lblCountdown.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 22;");
        }
    }

    private void stopCountdown(String displayText, String color) {
        if (countdownTimeline != null) countdownTimeline.stop();
        if (lblCountdown != null) {
            lblCountdown.setText(displayText);
            lblCountdown.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 16;");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateStatus(String status) {
        if (lblStatus == null) return;
        switch (status) {
            case "RUNNING"  -> { lblStatus.setText("🔴 Đang diễn ra"); lblStatus.setStyle("-fx-text-fill: #2ecc71;"); }
            case "PENDING"  -> { lblStatus.setText("⏳ Sắp bắt đầu");  lblStatus.setStyle("-fx-text-fill: #f39c12;"); }
            case "FINISHED" -> {
                lblStatus.setText("✅ Đã kết thúc"); lblStatus.setStyle("-fx-text-fill: #7f8c8d;");
                stopCountdown("KẾT THÚC", "#7f8c8d");
            }
            case "CANCELED" -> {
                lblStatus.setText("❌ Đã hủy"); lblStatus.setStyle("-fx-text-fill: #e74c3c;");
                stopCountdown("ĐÃ HỦY", "#e74c3c");
            }
            default -> lblStatus.setText(status);
        }
        if (txtBidInput != null) txtBidInput.setDisable(!"RUNNING".equals(status));
    }

    private String formatPrice(long p) {
        return String.format("%,d ₫", p).replace(",", ".");
    }

    private String getString(JsonObject o, String key, String def) {
        JsonElement el = o.get(key);
        return (el == null || el.isJsonNull()) ? def : el.getAsString();
    }

    private long getLong(JsonObject o, String key) {
        JsonElement el = o.get(key);
        return (el == null || el.isJsonNull()) ? 0L : el.getAsLong();
    }

    // ── Admin view ────────────────────────────────────────────────────────────

    private void setupAdminView() {
        if (bidAndAutoBidBox != null) {
            bidAndAutoBidBox.setVisible(false);
            bidAndAutoBidBox.setManaged(false);
        }
        if (tabPaneMain != null) {
            Tab tab = new Tab("👥 NGƯỜI THAM GIA");
            tab.setClosable(false);
            tab.setContent(buildParticipantsTable());
            tabPaneMain.getTabs().add(0, tab);
            tabPaneMain.getSelectionModel().select(0);
        }
    }

    private TableView<JsonObject> buildParticipantsTable() {
        TableView<JsonObject> table = new TableView<>(participants);

        TableColumn<JsonObject, String> cUser = new TableColumn<>("Người dùng");
        cUser.setCellValueFactory(c -> new SimpleStringProperty(getString(c.getValue(), "bidderUsername", "")));
        cUser.setPrefWidth(160);

        TableColumn<JsonObject, String> cBid = new TableColumn<>("Giá cao nhất");
        cBid.setCellValueFactory(c -> new SimpleStringProperty(formatPrice(getLong(c.getValue(), "amount"))));
        cBid.setPrefWidth(150);

        TableColumn<JsonObject, String> cAction = new TableColumn<>("Thao tác");
        cAction.setPrefWidth(110);
        cAction.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("🚫 Ban");
            {
                btn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    JsonObject row = getTableView().getItems().get(getIndex());
                    long uid  = getLong(row, "bidderId");
                    String un = getString(row, "bidderUsername", "?");
                    banUser(uid, un);
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().setAll(cUser, cBid, cAction);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void rebuildParticipants() {
        if (!SessionManager.isAdmin()) return;
        Map<Long, JsonObject> best = new LinkedHashMap<>();
        for (JsonObject b : bidHistory) {
            long uid = getLong(b, "bidderId");
            if (uid == 0) continue;
            if (!best.containsKey(uid) || getLong(b, "amount") > getLong(best.get(uid), "amount")) {
                best.put(uid, b);
            }
        }
        participants.setAll(best.values());
    }

    private void banUser(long userId, String username) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Ban người dùng \"" + username + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        ApiClient.updateUserStatus(userId, "BANNED");
                        Platform.runLater(() ->
                                new Alert(Alert.AlertType.INFORMATION, "Đã ban " + username + ".").show());
                    } catch (Exception e) {
                        Platform.runLater(() ->
                                new Alert(Alert.AlertType.ERROR, e.getMessage()).show());
                    }
                }).start();
            }
        });
    }

    private void showAntiSnipeNotif(String username) {
        if (lblAntiSnipeNotif == null) return;
        lblAntiSnipeNotif.setText("⚠  " + username + " đã kích hoạt anti-snipping — thời gian được gia hạn");
        lblAntiSnipeNotif.setVisible(true);
        lblAntiSnipeNotif.setManaged(true);
        if (antiSnipeHideTimer != null) antiSnipeHideTimer.stop();
        antiSnipeHideTimer = new Timeline(new KeyFrame(Duration.seconds(12), e -> {
            lblAntiSnipeNotif.setVisible(false);
            lblAntiSnipeNotif.setManaged(false);
        }));
        antiSnipeHideTimer.play();
    }
}