package com.auction.project.Server;

import com.auction.project.Controllers.ServerController;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Server Socket lắng nghe kết nối TCP đến từ các client.
 *
 * <p><b>Cơ chế:</b> Mỗi client kết nối → tạo một {@code ClientHandler} mới
 * → submit vào {@code ExecutorService} (thread pool) thay vì tạo Thread thủ công.
 * Thread pool giới hạn số thread đồng thời, tránh server bị quá tải (thread per connection).
 *
 * <p><b>Thiết kế:</b> SocketServer chỉ lo việc accept connection, không xử lý logic.
 * Mọi nghiệp vụ được ủy quyền cho ServerController thông qua ClientHandler.
 */
public class SocketServer {

    private static final Logger logger = Logger.getLogger(SocketServer.class.getName());

    /** Port mặc định — có thể cấu hình qua constructor */
    private static final int DEFAULT_PORT = 9090;

    /** Số thread tối đa phục vụ đồng thời */
    private static final int MAX_THREADS = 50;

    private final int port;
    private final ServerController controller;
    private ServerSocket serverSocket;

    /**
     * Thread pool — dùng CachedThreadPool thay vì tạo Thread mới mỗi kết nối.
     * Tái sử dụng thread đã rảnh, tạo thread mới nếu cần, tự xóa thread nhàn rỗi.
     */
    private final ExecutorService threadPool;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SocketServer(int port, ServerController controller) {
        this.port = port;
        this.controller = controller;
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    }

    public SocketServer(ServerController controller) {
        this(DEFAULT_PORT, controller);
    }

    // ── Start / Stop ──────────────────────────────────────────────────────────

    /**
     * Bắt đầu lắng nghe kết nối — blocking loop.
     * Gọi phương thức này trong thread riêng hoặc là thread main của server.
     *
     * <p>Thoát khỏi loop khi ServerSocket bị đóng (gọi {@code stop()}).
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("═══════════════════════════════════════════");
            logger.info("  Server đấu giá đang chạy tại cổng: " + port);
            logger.info("  Chờ kết nối từ client...");
            logger.info("═══════════════════════════════════════════");

            // Vòng lặp accept — mỗi vòng xử lý một client mới kết nối
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept(); // Blocking — đợi client kết nối
                    ClientHandler handler = new ClientHandler(clientSocket, controller);
                    threadPool.submit(handler); // Chạy trong thread pool, không block vòng lặp
                } catch (IOException e) {
                    if (serverSocket.isClosed()) {
                        break; // Server đã được dừng có chủ đích — thoát loop
                    }
                    logger.warning("Lỗi khi accept kết nối: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.severe("Không thể khởi động server tại cổng " + port + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Dừng server — đóng ServerSocket và shutdown thread pool.
     * Các kết nối hiện tại sẽ hoàn thành trước khi thread pool tắt.
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown(); // Không nhận task mới, chờ task hiện tại hoàn thành
            logger.info("Server đã dừng.");
        } catch (IOException e) {
            logger.warning("Lỗi khi dừng server: " + e.getMessage());
        }
    }
}