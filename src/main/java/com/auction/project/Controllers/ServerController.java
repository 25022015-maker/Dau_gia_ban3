package com.auction.project.Controllers;

import com.auction.project.DAO.AuctionDAO;
import com.auction.project.Models.Auction;
import com.auction.project.Models.AuctionManager;
import com.auction.project.Models.AuctionManager.BidResult;
import com.auction.project.Packets.BidRequest;
import com.auction.project.Packets.LoginRequest;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.auction.project.Server.ClientHandler;
import java.util.List;
import java.util.logging.Logger;

/**
 * Tầng Controller phía Server trong mô hình MVC.
 *
 * <p><b>Luồng xử lý request:</b>
 * <pre>
 *   Client (Socket)
 *       ↓ JSON String
 *   ClientHandler.run()        ← parse JSON → gọi ServerController
 *       ↓
 *   ServerController           ← business logic, validate
 *       ↓
 *   AuctionDAO                 ← đọc/ghi dữ liệu
 *       ↓
 *   AuctionManager             ← cập nhật state, notify Observer
 *       ↓ BID_UPDATE broadcast
 *   Tất cả ClientHandler       ← gửi JSON response về client
 * </pre>
 *
 * <p>Controller không giữ state — stateless, có thể tạo mới mỗi request
 * hoặc dùng chung một instance (hiện tại dùng chung qua ClientHandler).
 */
public class ServerController {

    private static final Logger logger = Logger.getLogger(ServerController.class.getName());

    /** DAO để truy cập dữ liệu — CHỈ Controller/Server mới dùng */
    private final AuctionDAO auctionDao;

    /** Singleton manager — quản lý state runtime của các phiên */
    private final AuctionManager auctionManager;

    public ServerController() {
        this.auctionDao = new AuctionDAO();
        this.auctionManager = AuctionManager.getInstance();

        // Nạp dữ liệu ban đầu từ DAO vào AuctionManager khi khởi động
        initializeAuctions();
    }

    // ── Initialization ────────────────────────────────────────────────────────

    /**
     * Tải tất cả phiên đấu giá từ DAO vào AuctionManager khi Server khởi động.
     * Chỉ gọi một lần duy nhất trong constructor.
     */
    private void initializeAuctions() {
        List<Auction> auctions = auctionDao.findAllAuctions();
        for (Auction auction : auctions) {
            auctionManager.addAuction(auction);
        }
        logger.info("ServerController: Đã nạp " + auctions.size() + " phiên đấu giá.");
    }

    // ── Request Handlers ──────────────────────────────────────────────────────

    /**
     * Xử lý yêu cầu đăng nhập từ client.
     *
     * <p>Kiểm tra thông tin qua DAO, trả về LOGIN_SUCCESS hoặc LOGIN_FAILURE.
     * Không có session token trong demo này — production nên dùng JWT.
     *
     * @param request  LoginRequest chứa username/password
     * @param handler  ClientHandler của client đang yêu cầu (để ghi log)
     * @return Response kết quả đăng nhập
     */
    public Response handleLogin(LoginRequest request, ClientHandler handler) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return Response.error("Tên đăng nhập và mật khẩu không được để trống.");
        }

        boolean valid = auctionDao.validateUser(request.getUsername(), request.getPassword());

        if (valid) {
            logger.info("LOGIN SUCCESS | User: " + request.getUsername()
                    + " | Client: " + handler.getClientId());
            return new Response(
                    ResponseType.LOGIN_SUCCESS,
                    "Đăng nhập thành công! Chào mừng " + request.getUsername(),
                    request.getUsername() // Trả về username để client lưu session
            );
        } else {
            logger.warning("LOGIN FAILURE | User: " + request.getUsername()
                    + " | Client: " + handler.getClientId());
            return new Response(
                    ResponseType.LOGIN_FAILURE,
                    "Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại.");
        }
    }

    /**
     * Xử lý yêu cầu lấy danh sách phiên đấu giá.
     *
     * @return Response chứa List&lt;Auction&gt; trong field data
     */
    public Response handleGetAuctionList() {
        List<Auction> auctions = auctionManager.getAllAuctions();
        logger.info("AUCTION_LIST | Trả về " + auctions.size() + " phiên.");
        return new Response(
                ResponseType.AUCTION_LIST,
                "Danh sách phiên đấu giá hiện tại.",
                auctions);
    }

    /**
     * Xử lý yêu cầu đặt giá từ một client.
     *
     * <p><b>Luồng xử lý chi tiết:</b>
     * <ol>
     *   <li>Validate input cơ bản (null check, amount > 0)</li>
     *   <li>Ủy quyền cho {@code AuctionManager.processBid()} — nơi acquire lock và kiểm tra nghiệp vụ</li>
     *   <li>Nếu thành công: AuctionManager đã tự broadcast BID_UPDATE cho tất cả observer</li>
     *   <li>Trả về BID_SUCCESS / BID_FAILURE cho client gọi</li>
     * </ol>
     *
     * @param request BidRequest từ client
     * @param handler ClientHandler của client đang đặt giá
     * @return Response kết quả đặt giá (chỉ gửi cho client này)
     */
    public Response handleBid(BidRequest request, ClientHandler handler) {
        // --- Bước 1: Validate đầu vào ---
        if (request.getAuctionId() == null || request.getBidderId() == null) {
            return Response.error("Thiếu thông tin phiên đấu giá hoặc người đặt giá.");
        }
        if (request.getBidAmount() <= 0) {
            return Response.error("Số tiền đặt giá phải lớn hơn 0.");
        }

        logger.info(String.format("BID REQUEST | Phiên %s | %s → %.0f",
                request.getAuctionId(), request.getBidderId(), request.getBidAmount()));

        // --- Bước 2: Xử lý qua AuctionManager (có lock + broadcast) ---
        BidResult result = auctionManager.processBid(
                request.getAuctionId(),
                request.getBidderId(),
                request.getBidAmount());

        // --- Bước 3: Trả kết quả cho client này ---
        if (result.isSuccess()) {
            // Lưu trạng thái mới vào DAO (persistence)
            auctionDao.saveAuction(result.getAuction());

            return new Response(
                    ResponseType.BID_SUCCESS,
                    result.getMessage(),
                    result.getAuction()); // Trả về Auction object để client cập nhật UI
        } else {
            return new Response(ResponseType.BID_FAILURE, result.getMessage());
        }
    }

    /**
     * Xử lý khi client muốn theo dõi realtime một phiên đấu giá.
     * Đăng ký handler làm observer của phiên đó.
     *
     * @param auctionId ID phiên muốn theo dõi
     * @param handler   ClientHandler của client
     * @return Response xác nhận đăng ký
     */
    public Response handleSubscribeAuction(String auctionId, ClientHandler handler) {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            return Response.error("Phiên đấu giá không tồn tại: " + auctionId);
        }

        auctionManager.registerObserver(auctionId, handler);
        logger.info("SUBSCRIBE | Phiên " + auctionId + " | Client: " + handler.getClientId());

        return new Response(
                ResponseType.BID_UPDATE, // Gửi trạng thái hiện tại ngay khi subscribe
                "Đã đăng ký theo dõi phiên " + auctionId,
                auction);
    }

    /**
     * Xử lý khi client thoát khỏi phiên — hủy đăng ký observer.
     *
     * @param auctionId ID phiên đang theo dõi
     * @param handler   ClientHandler cần hủy
     */
    public void handleUnsubscribeAuction(String auctionId, ClientHandler handler) {
        auctionManager.unregisterObserver(auctionId, handler);
    }

    /**
     * Dọn dẹp khi client ngắt kết nối — xóa khỏi tất cả observer list.
     *
     * @param handler ClientHandler vừa disconnect
     */
    public void handleClientDisconnect(ClientHandler handler) {
        auctionManager.unregisterFromAllAuctions(handler);
        logger.info("DISCONNECT | Client: " + handler.getClientId() + " | Đã xóa khỏi tất cả observer.");
    }
}