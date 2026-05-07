package com.auction.project.Client;

import com.auction.project.Packets.BidRequest;
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
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * SocketClient — lớp quản lý kết nối TCP tới Server.
 *
 * Đây là lớp low-level xử lý socket thực tế:
 *  Kết nối / ngắt kết nối TCP
 *  Gửi object dưới dạng JSON (thay thế ObjectOutputStream cũ)
 *  Lắng nghe response từ server trong một thread riêng
 *  Gọi callback {@code onResponse} khi nhận được dữ liệu mới

 * Được sử dụng bởi {@link NetworkClient} — tầng cao hơn cung cấp
 * các method tiện lợi như {@code sendBid()}, {@code sendLogin()}, v.v.
 */
public class SocketClient {

    private static final Logger logger = Logger.getLogger(SocketClient.class.getName());
    private static final Gson gson = new Gson();

    // ── Socket & I/O ──────────────────────────────────────────────────────────
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    // ── Listener thread ───────────────────────────────────────────────────────
    private Thread listenerThread;

    /**
     * Callback được gọi mỗi khi nhận được Response từ server.
     * Set từ bên ngoài qua {@link #setOnResponse(Consumer)}.
     */
    private Consumer<Response> onResponse;

    // ── Constructor ───────────────────────────────────────────────────────────

    public SocketClient() {
        // Không kết nối ngay — gọi connect() sau
    }

    public SocketClient(String host, int port) throws IOException {
        connect(host, port);
    }

    // ── Connect / Disconnect ──────────────────────────────────────────────────

    /**
     * Kết nối tới server và bắt đầu thread lắng nghe response.
     *
     * @param host hostname hoặc IP của server
     * @param port port của server
     * @throws IOException nếu không kết nối được
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true); // autoFlush
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        connected = true;
        logger.info("SocketClient: Đã kết nối tới " + host + ":" + port);
        startListenerThread();
    }

    /**
     * Ngắt kết nối, dừng listener thread và đóng socket.
     */
    public void disconnect() {
        connected = false;
        try {
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt(); // Báo hiệu thread dừng lại
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("SocketClient: Đã ngắt kết nối.");
        } catch (IOException e) {
            logger.warning("SocketClient: Lỗi khi ngắt kết nối — " + e.getMessage());
        }
    }

    // ── Listener Thread ───────────────────────────────────────────────────────

    /**
     * Khởi động thread ngầm lắng nghe JSON response từ server.
     * Mỗi dòng = một JSON response → parse → gọi {@code onResponse} callback.
     */
    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = in.readLine()) != null) {
                    final String rawJson = line;
                    try {
                        Response response = gson.fromJson(rawJson, Response.class);
                        if (onResponse != null) {
                            onResponse.accept(response); // Gọi callback (thường là Platform.runLater trong JavaFX)
                        }
                    } catch (Exception parseEx) {
                        logger.warning("SocketClient: Không parse được response — " + parseEx.getMessage());
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    logger.warning("SocketClient: Mất kết nối server — " + e.getMessage());
                    // Thông báo mất kết nối qua callback
                    if (onResponse != null) {
                        onResponse.accept(new Response(ResponseType.ERROR, "Mất kết nối tới server."));
                    }
                }
            }
            logger.info("SocketClient: Listener thread kết thúc.");
        }, "SocketListenerThread");

        listenerThread.setDaemon(true); // Tự tắt khi app đóng
        listenerThread.start();
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    /**
     * Gửi bất kỳ object nào lên server dưới dạng JSON.
     *
     * <p>Thay thế {@code ObjectOutputStream.writeObject()} cũ —
     * dùng Gson serialize thay vì Java serialization, nhẹ hơn và dễ debug hơn.
     *
     * <p>{@code synchronized} để tránh 2 thread cùng ghi socket đồng thời.
     *
     * @param req object cần gửi (BidRequest, LoginRequest, JsonObject, v.v.)
     */
    public synchronized void send(Object req) {
        if (!connected || socket == null || socket.isClosed()) {
            System.err.println("Không còn kết nối server.");
            return;
        }
        try {
            String json = gson.toJson(req);
            out.println(json); // println tự thêm newline — server dùng readLine()
            logger.fine("SocketClient SEND: " + json);
        } catch (Exception e) {
            logger.warning("Lỗi gửi dữ liệu: " + e.getMessage());
            connected = false;
        }
    }

    /**
     * Gửi một JSON string thô (dùng khi cần gửi action đơn giản không có body).
     * Ví dụ: {@code sendRaw("{\"action\":\"GET_AUCTIONS\"}")}
     *
     * @param jsonString chuỗi JSON cần gửi
     */
    public synchronized void sendRaw(String jsonString) {
        if (!connected || socket == null || socket.isClosed()) {
            System.err.println("Không còn kết nối server.");
            return;
        }
        out.println(jsonString);
    }

    // ── Callback Registration ─────────────────────────────────────────────────

    /**
     * Đặt callback xử lý response từ server.
     *
     * <p>Thường được gọi trong JavaFX Controller:
     * <pre>{@code
     * socketClient.setOnResponse(response -> {
     *     Platform.runLater(() -> {
     *         if (response.getType() == ResponseType.BID_UPDATE) {
     *             priceLabel.setText(String.valueOf(auction.getCurrentPrice()));
     *         }
     *     });
     * });
     * }</pre>
     *
     * @param onResponse consumer nhận Response mỗi khi server gửi dữ liệu
     */
    public void setOnResponse(Consumer<Response> onResponse) {
        this.onResponse = onResponse;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return true nếu socket đang mở và kết nối */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}