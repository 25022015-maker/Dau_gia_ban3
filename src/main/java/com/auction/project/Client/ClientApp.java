package com.auction.project.Client;

import com.auction.project.UI.View.LoginUI;
import javafx.application.Application;

import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Entry point demo của Client — mô phỏng kết nối và giao tiếp với Server.
 *
 * <p>Trong dự án hoàn chỉnh với JavaFX, đây sẽ là {@code Application.main()}
 * khởi động JavaFX GUI. Class này demo luồng kết nối bằng console.
 *
 * <p><b>Cách sử dụng NetworkClient trong JavaFX Controller:</b>
 * <pre>{@code
 * // Trong JavaFX Controller (initialize method):
 * NetworkClient client = new NetworkClient();
 * ClientHandler handler = new ClientHandler();
 * client.addResponseListener(handler);
 * client.connect("localhost", 9090);
 *
 * // Khi người dùng click nút "Đăng nhập":
 * client.sendLogin(usernameField.getText(), passwordField.getText());
 *
 * // Khi mở màn hình chi tiết phiên A001 (để nhận BID_UPDATE realtime):
 * client.subscribeToAuction("A001");
 *
 * // Khi người dùng click nút "Đặt giá":
 * client.sendBid("A001", currentUser, Double.parseDouble(amountField.getText()));
 * }</pre>
 */
public class ClientApp {

    private static final Logger logger = Logger.getLogger(ClientApp.class.getName());
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;

    public static void main(String[] args) {
        NetworkClient networkClient = new NetworkClient();
        ClientHandler responseHandler = new ClientHandler();

        // Đăng ký handler để nhận tất cả response từ server
        networkClient.addResponseListener(responseHandler);

        try {
            // Bước 1: Kết nối tới server — listener thread tự khởi động
            networkClient.connect(SERVER_HOST, SERVER_PORT);
            Application.launch(LoginUI.class, args);
            logger.info("Kết nối thành công! Listener thread đang chạy nền.");

            // Bước 2: Demo flow — đăng nhập, lấy danh sách, đặt giá
            // Trong thực tế, các lệnh này được kích hoạt bởi hành động UI

            Thread.sleep(500); // Chờ kết nối ổn định

            // Đăng nhập
            logger.info("Gửi yêu cầu đăng nhập...");
            networkClient.sendLogin("alice", "alice123");

            Thread.sleep(500);

            // Lấy danh sách phiên đấu giá
            logger.info("Lấy danh sách phiên đấu giá...");
            networkClient.sendGetAuctions();

            Thread.sleep(500);

            // Subscribe để nhận realtime update của phiên A001
            logger.info("Subscribe phiên A001 để nhận realtime update...");
            networkClient.subscribeToAuction("A001");

            Thread.sleep(500);

            // Đặt giá
            logger.info("Đặt giá 6,000,000 cho phiên A001...");
            networkClient.sendBid("A001", "alice", 6_000_000);

            // Giữ kết nối để nhận BID_UPDATE từ các client khác
            logger.info("Đang lắng nghe BID_UPDATE realtime... (nhấn Enter để thoát)");
            new Scanner(System.in).nextLine();

        } catch (Exception e) {
            logger.severe("Lỗi: " + e.getMessage());
        } finally {
            networkClient.disconnect();
        }
    }
}