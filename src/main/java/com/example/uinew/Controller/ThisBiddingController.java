package com.example.uinew.Controller;
import com.example.uinew.model.Product;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import java.net.URL;
import java.util.ResourceBundle;

public class ThisBiddingController extends MainController implements Initializable {
    @FXML private Label lblProductName, lblCurrentPrice, lblTimer;
    @FXML private TextField txtBidAmount;

    private Product currentProduct;
    private int timeLeft = 3600; // Giả sử 1 tiếng (giây)

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Lấy sản phẩm đang được chọn từ MainController (bạn nên lưu nó tương tự currentUser)
        // currentProduct = MainController.getSelectedProduct();
        startTimer();
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

    @FXML
    void handlePlaceBid(ActionEvent event) {
        double bid = Double.parseDouble(txtBidAmount.getText());
        //Gửi bid lên server thông qua Service
        System.out.println("Đã đặt giá: " + bid);
    }

    private String formatTime(int totalSeconds) { //chuan hoa thoi gian
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public void BiddingHistory(ActionEvent event){

    }
}

