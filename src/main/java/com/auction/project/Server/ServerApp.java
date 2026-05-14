package com.auction.project.Server;

import com.auction.project.Controllers.ServerController;
import java.util.logging.Logger;

/**
 * Entry point của Server ứng dụng đấu giá.
 *
 * <p><b>Thứ tự khởi động:</b>
 * <ol>
 *   <li>Khởi tạo {@code ServerController} — nạp dữ liệu từ DAO vào AuctionManager</li>
 *   <li>Khởi tạo {@code SocketServer} với controller</li>
 *   <li>Gọi {@code server.start()} — blocking loop chờ client</li>
 * </ol>
 *
 * <p>Chạy ServerApp và ClientApp trên cùng máy hoặc khác máy trong cùng mạng LAN.
 */
public class ServerApp {

    private static final Logger logger = Logger.getLogger(ServerApp.class.getName());
    private static final int SERVER_PORT = 9090;

    public static void main(String[] args) {
        logger.info("Đang khởi động Hệ thống Đấu Giá Trực Tuyến...");

        // 1. Controller — cũng khởi tạo DAO và nạp dữ liệu vào AuctionManager
        ServerController controller = new ServerController();

        // 2. Socket server
        SocketServer server = new SocketServer(SERVER_PORT, controller);

        // 3. Đăng ký shutdown hook để dọn dẹp khi Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Nhận tín hiệu dừng — đang shutdown server...");
            server.stop();
        }));

        // 4. Start — blocking tại đây cho đến khi server dừng
        server.start();
    }
}