package com.auction.project.Client;

import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import javafx.application.Platform;
import java.util.logging.Logger;

/**
 * Xử lý các response nhận từ Server và cập nhật giao diện JavaFX.
 *
 * <p>Class này implement {@code NetworkClient.ResponseListener} để nhận
 * thông báo khi có dữ liệu mới từ server (thông qua listener thread của NetworkClient).
 *
 * <p><b>Quan trọng về JavaFX Threading:</b> Mọi cập nhật UI PHẢI chạy trên
 * JavaFX Application Thread. Vì response đến từ {@code listenerThread} (thread nền),
 * ta dùng {@code Platform.runLater()} để đưa code cập nhật UI vào đúng thread.
 *
 * <p>Trong dự án thực tế, class này sẽ gọi các phương thức của JavaFX Controller
 * (ví dụ: {@code auctionListController.refresh()}, {@code bidController.updatePrice()}).
 * Ở đây ta in log ra console để demo luồng xử lý.
 */
public class ClientHandler implements NetworkClient.ResponseListener {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    /**
     * Callback nhận mọi response từ server.
     *
     * <p><b>Luồng:</b> Phương thức này được gọi trên {@code listenerThread}
     * (thread nền của NetworkClient), sau đó dùng {@code Platform.runLater()}
     * để update UI an toàn trên JavaFX Application Thread.
     *
     * @param response response nhận từ server
     */
    @Override
    public void onResponse(Response response) {
        // Đưa tất cả xử lý UI về JavaFX Application Thread
        Platform.runLater(() -> handleResponse(response));
    }

    /**
     * Dispatch response đến handler cụ thể dựa trên {@code ResponseType}.
     * Chạy trên JavaFX Application Thread (do Platform.runLater).
     *
     * @param response response cần xử lý
     */
    private void handleResponse(Response response) {
        if (response == null || response.getType() == null) {
            logger.warning("Nhận được response null hoặc không có type.");
            return;
        }

        switch (response.getType()) {
            case LOGIN_SUCCESS:
                onLoginSuccess(response);
                break;
            case LOGIN_FAILURE:
                onLoginFailure(response);
                break;
            case AUCTION_LIST:
                onAuctionListReceived(response);
                break;
            case BID_SUCCESS:
                onBidSuccess(response);
                break;
            case BID_FAILURE:
                onBidFailure(response);
                break;
            case BID_UPDATE:
                // Đây là broadcast từ server — client nhận realtime không cần refresh thủ công
                onBidUpdate(response);
                break;
            case AUCTION_ENDED:
                onAuctionEnded(response);
                break;
            case ERROR:
                onError(response);
                break;
            default:
                logger.warning("ResponseType không được xử lý: " + response.getType());
        }
    }

    // ── Các Handler Cụ Thể ────────────────────────────────────────────────────
    // TODO: Thay các logger bằng lời gọi JavaFX Controller method thực tế
    // Ví dụ: loginController.showSuccess(message) hoặc mainController.refreshList()

    private void onLoginSuccess(Response response) {
        logger.info("✅ Đăng nhập thành công! User: " + response.getData());
        // TODO: loginController.navigateToMainScreen();
        // TODO: SessionManager.setCurrentUser((String) response.getData());
    }

    private void onLoginFailure(Response response) {
        logger.warning("❌ Đăng nhập thất bại: " + response.getMessage());
        // TODO: loginController.showErrorAlert(response.getMessage());
    }

    private void onAuctionListReceived(Response response) {
        logger.info("📋 Nhận danh sách phiên đấu giá từ server.");
        // TODO: auctionListController.updateAuctionTable(response.getData());
        // response.getData() là List<Auction> — cần parse lại với Gson nếu dùng Object
    }

    private void onBidSuccess(Response response) {
        logger.info("💚 Đặt giá thành công! " + response.getMessage());
        // TODO: bidController.showSuccessNotification(response.getMessage());
        // TODO: bidController.updateCurrentPrice(auction.getCurrentPrice());
    }

    private void onBidFailure(Response response) {
        logger.warning("🔴 Đặt giá thất bại: " + response.getMessage());
        // TODO: bidController.showErrorAlert(response.getMessage());
    }

    /**
     * Xử lý BID_UPDATE broadcast — server push giá mới về cho TẤT CẢ client.
     * Đây là trái tim của tính năng realtime — không cần refresh thủ công!
     *
     * @param response chứa Auction object với giá và người dẫn đầu mới nhất
     */
    private void onBidUpdate(Response response) {
        logger.info("🔔 BID_UPDATE: " + response.getMessage());
        // TODO: bidViewController.updatePriceLabel(auction.getCurrentPrice());
        // TODO: bidViewController.updateLeaderLabel(auction.getLeadingBidder());
        // TODO: chartController.addDataPoint(LocalDateTime.now(), auction.getCurrentPrice());
    }

    private void onAuctionEnded(Response response) {
        logger.info("🏁 Phiên đấu giá kết thúc: " + response.getMessage());
        // TODO: bidViewController.showAuctionEndedDialog(response.getData());
    }

    private void onError(Response response) {
        logger.severe("⚠️  Lỗi từ server: " + response.getMessage());
        // TODO: mainController.showErrorSnackbar(response.getMessage());
    }
}