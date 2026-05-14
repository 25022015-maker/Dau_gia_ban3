package com.example.uinew.Controller;
import com.example.uinew.Interface.ShowError;
import com.example.uinew.model.Product;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class ThisBiddingController implements ShowError {
    @FXML private Label lblProductName, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount; //Bid user gõ
    @FXML Label lblError;
    @FXML private Label currentBudget; //số tiền đã bid gần nhất


    @FXML Button autoPlaceBid;
    @FXML Label history;
    @FXML TextField txtAutoStep;

    private double bidAmount; //model
    private double autoStep = 0;
    private Product currentProduct;
    private int timeLeft = 3600; //Giả sử 1 tiếng (giây)

    public void showError(String message) {
        lblError.setText(message);
    }

    @FXML
    public void initialize() {
        currentProduct = MainController.getSelectedProduct();
        if (currentProduct != null) {
            refreshUI();
        }
        startTimer();
    }

    @FXML
    ListView<String> biddingHistory;

    @FXML
    void setAutoBid (ActionEvent event){
        try {
            autoStep = Double.parseDouble(txtAutoStep.getText());
            //if (autoStep =< Auction.getMinBid() -> lblError.setTxt()
            //Lấy giá hiện tại cộng thêm bước nhảy
            double newPrice = currentProduct.getPrice() + autoStep;
            currentProduct.setPrice(newPrice);
            biddingHistory.getItems().add(0, "Bạn đã đặt: " + newPrice);

            refreshUI(); //Cập nhật toàn bộ Label trên màn hình
        } catch (Exception e) {
            showError("Bước nhảy không hợp lệ");
        }
    }
    private void refreshUI() {
        String priceText = String.format("%.0f", currentProduct.getPrice());
        lblProductName.setText(currentProduct.getName());
        lblCurrentPrice.setText("Giá hiện tại: " + priceText);
        currentBudget.setText("Giá bạn vừa đặt: " + priceText);
    }

    @FXML
    void handleSetBid(ActionEvent event){
        try {
            bidAmount = Double.parseDouble(txtBidAmount.getText());
            // Kiểm tra logic: Giá mới phải cao hơn giá cũ
            if (bidAmount <= currentProduct.getPrice()) {
                showError("Giá phải cao hơn giá hiện tại!");
                return;
            }

            currentProduct.setPrice(bidAmount);
            refreshUI();// Cập nhật lại UI

            // Thêm vào lịch sử
            biddingHistory.getItems().add(0, "Bạn đã đặt: " + bidAmount);
            lblError.setText("");
        } catch (Exception e) {
            showError("Số tiền nhập vào không đúng");
        }
    }

    @FXML private void toggleHistory(ActionEvent event) {
        if (biddingHistory.isVisible()) {
            biddingHistory.setVisible(false);
            biddingHistory.setManaged(false); // Thu hồi không gian
        } else {
            biddingHistory.setVisible(true);
            biddingHistory.setManaged(true);  // Hiển thị lại không gian
        }
    }

    private void startTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            lblTimer.setText(formatTime(timeLeft));
            if (timeLeft <= 0) {
                // Xử lý khi hết giờ
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private String formatTime(int totalSeconds) { //chuan hoa thoi gian
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public void biddingHistory(ActionEvent event){
       String historyBid = new String(history.getText());

    }
}

