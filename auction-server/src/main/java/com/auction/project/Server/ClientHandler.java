package com.auction.project.Server;

import com.auction.project.Controllers.ServerController;
import com.auction.project.Entities.Auction;

import com.auction.project.Packets.BidRequest;
import com.auction.project.Packets.LoginRequest;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.auction.project.Packets.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Mỗi instance của lớp này quản lý kết nối với MỘT client cụ thể.
 *
 * <p><b>Vòng đời:</b> SocketServer tạo một ClientHandler mới và chạy nó
 * trong Thread riêng cho mỗi client kết nối. Thread này kết thúc khi client
 * ngắt kết nối.
 *
 * <p><b>Giao thức:</b> Mỗi message là một dòng JSON (newline-delimited JSON).
 * Field "action" trong JSON xác định loại request để Controller xử lý.
 *
 * <p><b>Vai trò Observer:</b> Khi AuctionManager cần broadcast BID_UPDATE,
 * nó gọi {@code sendBidUpdate()} trên tất cả ClientHandler đã đăng ký.
 * Phương thức này thread-safe nhờ {@code synchronized}.
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    // ── Gson instance (thread-safe — dùng chung được) ─────────────────────────
    private static final Gson gson = GsonFactory.create();

    // ── Socket & I/O ──────────────────────────────────────────────────────────
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    // ── Identity ──────────────────────────────────────────────────────────────
    /** ID ngẫu nhiên để phân biệt các client trong log */
    private final String clientId;
    /** Username sau khi client đăng nhập thành công */
    private String loggedInUser;

    // ── Controller ────────────────────────────────────────────────────────────
    private final ServerController controller;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ClientHandler(Socket socket, ServerController controller) throws IOException {
        this.socket = socket;
        this.controller = controller;
        this.clientId = UUID.randomUUID().toString().substring(0, 8); // ID ngắn gọn

        // Khởi tạo stream đọc/ghi — UTF-8 để hỗ trợ tiếng Việt
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.writer = new PrintWriter(socket.getOutputStream(), true); // autoFlush = true

        logger.info("Client kết nối | ID: " + clientId + " | IP: " + socket.getInetAddress());
    }

    // ── Main Loop ─────────────────────────────────────────────────────────────

    /**
     * Vòng lặp chính: đọc JSON từ client → parse → dispatch tới Controller → gửi Response.
     *
     * <p>Chạy trong thread riêng do SocketServer tạo, kết thúc khi client ngắt kết nối.
     */
    @Override
    public void run() {
        try {
            String rawMessage;
            // Đọc từng dòng (mỗi dòng = một request JSON)
            while ((rawMessage = reader.readLine()) != null) {
                logger.fine("RECV [" + clientId + "]: " + rawMessage);
                handleMessage(rawMessage);
            }
        } catch (IOException e) {
            // Client ngắt kết nối đột ngột (đóng ứng dụng, mất mạng, v.v.)
            logger.info("Client ngắt kết nối | ID: " + clientId + " | Lý do: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ── Message Dispatcher ────────────────────────────────────────────────────

    /**
     * Parse JSON và dispatch tới handler phù hợp dựa trên field "action".
     *
     * <p>Giao thức action:
     * <ul>
     *   <li>"LOGIN"       → xử lý đăng nhập</li>
     *   <li>"BID"         → xử lý đặt giá</li>
     *   <li>"GET_AUCTIONS"→ lấy danh sách phiên</li>
     *   <li>"SUBSCRIBE"   → đăng ký theo dõi realtime một phiên</li>
     *   <li>"UNSUBSCRIBE" → hủy theo dõi</li>
     * </ul>
     *
     * @param rawMessage chuỗi JSON nhận được từ client
     */
    private void handleMessage(String rawMessage) {
        try {
            // Parse JSON để lấy field "action" trước
            JsonObject json = JsonParser.parseString(rawMessage).getAsJsonObject();
            String action = json.get("action").getAsString();

            Response response;

            switch (action) {
                case "LOGIN":
                    LoginRequest loginRequest = gson.fromJson(rawMessage, LoginRequest.class);
                    response = controller.handleLogin(loginRequest, this);
                    // Lưu username vào session nếu đăng nhập thành công
                    if (response.getType() == ResponseType.LOGIN_SUCCESS) {
                        this.loggedInUser = loginRequest.getUsername();
                    }
                    sendResponse(response);
                    break;

                case "SIGNUP":
                    response = controller.handleSignup(json, this);
                    if (response.getType() == ResponseType.LOGIN_SUCCESS) {
                        this.loggedInUser = json.get("username").getAsString();
                    }
                    sendResponse(response);
                    break;

                case "GET_AUCTIONS":
                    // Không cần body — chỉ cần action
                    response = controller.handleGetAuctionList();
                    sendResponse(response);
                    break;

                case "BID":
                    // Yêu cầu đăng nhập trước khi đặt giá
                    if (loggedInUser == null) {
                        sendResponse(Response.error("Bạn cần đăng nhập trước khi đặt giá."));
                        break;
                    }
                    BidRequest bidRequest = gson.fromJson(rawMessage, BidRequest.class);
                    // Đảm bảo bidderId khớp với user đang đăng nhập (chống giả mạo)
                    bidRequest.setBidderId(loggedInUser);
                    response = controller.handleBid(bidRequest, this);
                    sendResponse(response);
                    break;

                case "SUBSCRIBE":
                    String auctionId = json.get("auctionId").getAsString();
                    response = controller.handleSubscribeAuction(auctionId, this);
                    sendResponse(response);
                    break;

                case "UNSUBSCRIBE":
                    String unsubAuctionId = json.get("auctionId").getAsString();
                    controller.handleUnsubscribeAuction(unsubAuctionId, this);
                    break;

                case "CREATE_AUCTION":
                    response = controller.handleCreateAuction(json, this);
                    sendResponse(response);
                    break;

                default:
                    logger.warning("Action không xác định từ client " + clientId + ": " + action);
                    sendResponse(Response.error("Action không được hỗ trợ: " + action));
            }

        } catch (Exception e) {
            // JSON malformed hoặc thiếu field bắt buộc
            logger.warning("Lỗi parse message từ [" + clientId + "]: " + e.getMessage());
            sendResponse(Response.error("Dữ liệu không hợp lệ: " + e.getMessage()));
        }
    }

    // ── Send Methods ──────────────────────────────────────────────────────────

    /**
     * Gửi một Response object về cho client dưới dạng JSON.
     *
     * <p><b>synchronized</b> để đảm bảo không có hai thread cùng ghi vào
     * socket của client này đồng thời (ví dụ: thread xử lý request của client này
     * và thread broadcast BID_UPDATE từ AuctionManager).
     *
     * @param response object cần gửi
     */
    public synchronized void sendResponse(Response response) {
        try {
            String json = gson.toJson(response);
            writer.println(json); // println tự thêm newline — phía client dùng readLine()
            logger.fine("SEND [" + clientId + "]: " + response.getType());
        } catch (Exception e) {
            logger.warning("Không thể gửi response tới [" + clientId + "]: " + e.getMessage());
        }
    }

    /**
     * Gửi thông báo BID_UPDATE khi có giá mới trong phiên mà client đang theo dõi.
     *
     * <p>Được gọi bởi {@code AuctionManager} từ thread bất kỳ — do đó phải synchronized.
     * Đây là điểm mấu chốt của Observer Pattern: Server CHỦ ĐỘNG push dữ liệu,
     * không phải Client polling.
     *
     * @param auction phiên vừa được cập nhật giá mới
     */
    public void sendBidUpdate(Auction auction) {
        com.auction.project.Packets.AuctionDTO dto = com.auction.project.Packets.AuctionDTO.from(auction);
        Response update = new Response(
                ResponseType.BID_UPDATE,
                String.format(
                        "💰 %s vừa đặt %.0f VNĐ cho '%s'",
                        dto.getLeadingBidder(),
                        dto.getCurrentPrice(),
                        dto.getItemName()),
                dto);
        sendResponse(update);
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    /**
     * Dọn dẹp tài nguyên khi client ngắt kết nối.
     * Đảm bảo xóa khỏi tất cả observer list để tránh memory leak và NPE khi broadcast.
     */
    private void cleanup() {
        try {
            // Xóa khỏi tất cả observer (rất quan trọng — tránh broadcast tới socket đã đóng)
            controller.handleClientDisconnect(this);

            if (!socket.isClosed()) {
                socket.close();
            }
            logger.info("Đã cleanup | Client: " + clientId);
        } catch (IOException e) {
            logger.warning("Lỗi khi cleanup client " + clientId + ": " + e.getMessage());
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    /** @return ID định danh client (dùng trong log) */
    public String getClientId() {
        return clientId;
    }

    /** @return username của client đang đăng nhập, null nếu chưa đăng nhập */
    public String getLoggedInUser() {
        return loggedInUser;
    }
}