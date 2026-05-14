package com.auction.project.Client;

import com.auction.project.Packets.BidRequest;
import com.auction.project.Packets.LoginRequest;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Quản lý toàn bộ kết nối mạng phía Client.
 *
 * <p><b>Hai chiều giao tiếp:</b>
 * <ul>
 *   <li><b>Client → Server (gửi):</b> Các phương thức public như {@code sendBid()},
 *       {@code sendLogin()} — gọi từ UI thread (JavaFX Application Thread)</li>
 *   <li><b>Server → Client (nhận):</b> Thread {@code listenerThread} chạy ngầm,
 *       đọc JSON từ server và notify các {@code ResponseListener} đã đăng ký.</li>
 * </ul>
 *
 * <p><b>Observer trên client:</b> Tầng UI (Controller JavaFX) đăng ký
 * {@code ResponseListener} để nhận thông báo khi có dữ liệu mới từ server
 * (đặc biệt là BID_UPDATE broadcast). Không cần polling!
 *
 * <p><b>Thread-safety:</b> {@code sendRequest()} được synchronized để tránh
 * hai thread cùng ghi socket đồng thời.
 */
public class NetworkClient {

    private static final Logger logger = Logger.getLogger(NetworkClient.class.getName());
    private static final Gson gson = new Gson();

    // ── Connection ────────────────────────────────────────────────────────────
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;

    // ── Listener Thread ───────────────────────────────────────────────────────
    /** Thread ngầm — liên tục đọc dữ liệu từ server và dispatch cho listener */
    private Thread listenerThread;

    // ── Observer: ResponseListener ────────────────────────────────────────────

    /**
     * Functional interface cho các thành phần muốn nhận response từ server.
     * Thường được implement bởi JavaFX Controller.
     */
    public interface ResponseListener {
        /**
         * Được gọi mỗi khi nhận được response từ server.
         *
         * <p><b>Lưu ý:</b> Phương thức này chạy trên {@code listenerThread},
         * KHÔNG phải JavaFX Application Thread. Để cập nhật UI, phải dùng
         * {@code Platform.runLater(() -> { ... })}.
         *
         * @param response response nhận từ server
         */
        void onResponse(Response response);
    }

    /** Danh sách listener đang đăng ký nhận thông báo */
    private final List<ResponseListener> listeners = new ArrayList<>();

    // ── Connect / Disconnect ──────────────────────────────────────────────────

    /**
     * Kết nối tới Server và bắt đầu thread lắng nghe.
     *
     * @param host hostname hoặc IP của server (ví dụ: "localhost", "192.168.1.10")
     * @param port port của server (mặc định: 9090)
     * @throws IOException nếu không kết nối được
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        writer = new PrintWriter(socket.getOutputStream(), true);
        connected = true;

        logger.info("Đã kết nối tới server " + host + ":" + port);

        // Khởi động thread lắng nghe response từ server
        startListenerThread();
    }

    /**
     * Ngắt kết nối và dừng listener thread.
     * Gọi khi người dùng thoát ứng dụng.
     */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("Đã ngắt kết nối khỏi server.");
        } catch (IOException e) {
            logger.warning("Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    // ── Listener Thread ───────────────────────────────────────────────────────

    /**
     * Khởi động thread ngầm để lắng nghe dữ liệu đến từ server.
     *
     * <p>Thread này chạy suốt vòng đời kết nối, đọc từng dòng JSON
     * và dispatch tới tất cả {@code ResponseListener} đã đăng ký.
     * Đây là cơ chế nhận BID_UPDATE realtime — không có polling!
     */
    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            logger.info("Listener thread bắt đầu — chờ dữ liệu từ server...");
            try {
                String rawResponse;
                // Đọc từng dòng JSON từ server — block tại đây đến khi có dữ liệu
                while (connected && (rawResponse = reader.readLine()) != null) {
                    logger.fine("RECV: " + rawResponse);
                    dispatchResponse(rawResponse);
                }
            } catch (IOException e) {
                if (connected) {
                    // Mất kết nối bất ngờ (server crash, mạng đứt, v.v.)
                    logger.warning("Mất kết nối tới server: " + e.getMessage());
                    // Thông báo cho UI về việc mất kết nối
                    notifyListeners(new Response(ResponseType.ERROR, "Mất kết nối tới server."));
                }
            }
            logger.info("Listener thread kết thúc.");
        });

        listenerThread.setDaemon(true); // Thread tự tắt khi main thread kết thúc
        listenerThread.setName("ServerListenerThread");
        listenerThread.start();
    }

    /**
     * Parse JSON response và notify tất cả listener.
     *
     * @param rawResponse chuỗi JSON nhận từ server
     */
    private void dispatchResponse(String rawResponse) {
        try {
            Response response = gson.fromJson(rawResponse, Response.class);
            notifyListeners(response);
        } catch (Exception e) {
            logger.warning("Không thể parse response từ server: " + e.getMessage());
        }
    }

    /**
     * Gọi {@code onResponse()} trên tất cả listener đã đăng ký.
     *
     * @param response response cần dispatch
     */
    private void notifyListeners(Response response) {
        // Dùng copy để tránh ConcurrentModificationException nếu listener tự hủy
        List<ResponseListener> copy = new ArrayList<>(listeners);
        for (ResponseListener listener : copy) {
            try {
                listener.onResponse(response);
            } catch (Exception e) {
                logger.warning("Lỗi trong ResponseListener: " + e.getMessage());
            }
        }
    }

    // ── Observer Registration ──────────────────────────────────────────────────

    /**
     * Đăng ký nhận thông báo khi có response từ server.
     * Thường được gọi trong JavaFX Controller khi màn hình được khởi tạo.
     *
     * @param listener listener muốn đăng ký
     */
    public void addResponseListener(ResponseListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Hủy đăng ký nhận thông báo.
     * Gọi khi JavaFX Controller bị destroy (ví dụ: đóng cửa sổ).
     *
     * @param listener listener muốn hủy
     */
    public void removeResponseListener(ResponseListener listener) {
        listeners.remove(listener);
    }

    // ── Send Methods ──────────────────────────────────────────────────────────

    /**
     * Gửi yêu cầu đăng nhập lên server.
     *
     * @param username tên đăng nhập
     * @param password mật khẩu
     */
    public void sendLogin(String username, String password) {
        LoginRequest request = new LoginRequest(username, password);
        sendRequest(gson.toJson(request));
    }

    /**
     * Gửi yêu cầu lấy danh sách phiên đấu giá.
     */
    public void sendGetAuctions() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "GET_AUCTIONS");
        sendRequest(gson.toJson(request));
    }

    /**
     * Gửi yêu cầu đặt giá.
     *
     * @param auctionId ID phiên đấu giá
     * @param bidderId  username người đặt (server sẽ verify lại)
     * @param amount    số tiền đặt giá
     */
    public void sendBid(String auctionId, String bidderId, double amount) {
        BidRequest request = new BidRequest(auctionId, bidderId, amount);
        sendRequest(gson.toJson(request));
    }

    /**
     * Đăng ký theo dõi realtime một phiên đấu giá.
     * Sau khi gọi phương thức này, mọi BID_UPDATE của phiên sẽ được server push về.
     *
     * @param auctionId ID phiên muốn theo dõi
     */
    public void subscribeToAuction(String auctionId) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "SUBSCRIBE");
        request.addProperty("auctionId", auctionId);
        sendRequest(gson.toJson(request));
    }

    /**
     * Hủy theo dõi realtime một phiên đấu giá.
     *
     * @param auctionId ID phiên muốn hủy theo dõi
     */
    public void unsubscribeFromAuction(String auctionId) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "UNSUBSCRIBE");
        request.addProperty("auctionId", auctionId);
        sendRequest(gson.toJson(request));
    }

    /**
     * Gửi chuỗi JSON tới server qua socket.
     *
     * <p><b>synchronized</b> để đảm bảo không có hai thread cùng ghi socket
     * (ví dụ: UI thread gọi sendBid() trong khi thread khác đang gửi request khác).
     *
     * @param jsonMessage chuỗi JSON cần gửi
     */
    public synchronized void sendRequest(String jsonMessage) {
        if (!connected || writer == null) {
            logger.warning("Chưa kết nối tới server — không thể gửi: " + jsonMessage);
            return;
        }
        writer.println(jsonMessage); // println tự thêm newline — server dùng readLine()
        logger.fine("SEND: " + jsonMessage);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return true nếu đang kết nối với server */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}