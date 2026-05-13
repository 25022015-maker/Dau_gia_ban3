package com.auction.project.Controllers;

import com.auction.project.DAO.AuctionDAO;
import com.auction.project.Entities.Auction;
import com.auction.project.Entities.Bidder;
import com.auction.project.Exception.AuctionClosedException;
import com.auction.project.Exception.InvalidBidException;
import com.auction.project.Manager.AuctionManager;
import com.auction.project.Packets.AuctionDTO;
import com.auction.project.Entities.enums.AuctionStatus;
import com.auction.project.Factory.ItemFactory;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import com.auction.project.Packets.BidRequest;
import com.auction.project.Packets.LoginRequest;
import com.auction.project.Packets.Response;
import com.auction.project.Packets.ResponseType;
import com.auction.project.Server.ClientHandler;
import com.auction.project.Server.ClientHandlerObserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * MVC Controller phía Server — kết nối Socket với logic của nhóm.
 *
 * Luồng xử lý bid hoàn chỉnh:
 *
 *   Client A gửi BidRequest JSON
 *     → ClientHandler.handleMessage()
 *         → ServerController.handleBid()
 *             → AuctionDAO.findBidderByUsername()   // lấy Bidder object
 *             → AuctionManager.getAuction(id)       // lấy Auction object
 *             → Auction.placeBid(bidder, amount)    // logic nhóm: lock + validate + anti-snipe
 *                 → notifyObservers()
 *                     → ClientHandlerObserver.update()   // Adapter
 *                         → ClientHandler.sendResponse(BID_UPDATE)
 *                             → Socket → Client B, C nhận realtime
 *             → trả BID_SUCCESS về Client A
 *
 * Lưu ý quan trọng:
 *   - AuctionManager của nhóm dùng int id (không phải String)
 *   - BidRequest.auctionId là String → parse sang int trước khi dùng
 *   - Auction.placeBid() tự throw InvalidBidException / AuctionClosedException
 *   - KHÔNG có lock ở ServerController vì lock đã nằm trong Auction.placeBid()
 */
public class ServerController {

    private static final Logger logger = Logger.getLogger(ServerController.class.getName());

    private final AuctionManager auctionManager; // Singleton của nhóm
    private final AuctionDAO auctionDao;

    /**
     * Registry theo dõi Observer nào đang subscribe phiên nào.
     * Dùng để unregister đúng observer khi client disconnect.
     * Key: auctionId (int) — Value: list ClientHandlerObserver của phiên đó
     */
    private final Map<Integer, List<ClientHandlerObserver>> observerRegistry
            = new ConcurrentHashMap<>();

    public ServerController() {
        this.auctionManager = AuctionManager.getInstance();
        this.auctionDao = new AuctionDAO();
        initializeAuctions();
    }

    // ── Khởi tạo ─────────────────────────────────────────────────────────────

    private void initializeAuctions() {
        List<Auction> auctions = auctionDao.findAllAuctions();
        for (Auction auction : auctions) {
            auctionManager.addAuction(auction);
            observerRegistry.put(auction.getId(), new ArrayList<>());
        }
        logger.info("ServerController: Đã nạp " + auctions.size() + " phiên đấu giá.");
    }

    // ── Đăng nhập ─────────────────────────────────────────────────────────────

    public Response handleLogin(LoginRequest request, ClientHandler handler) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return Response.error("Tên đăng nhập và mật khẩu không được để trống.");
        }

        boolean valid = auctionDao.validateUser(request.getUsername(), request.getPassword());

        if (valid) {
            logger.info("LOGIN SUCCESS | " + request.getUsername()
                    + " | Client: " + handler.getClientId());
            return new Response(
                    ResponseType.LOGIN_SUCCESS,
                    "Đăng nhập thành công! Chào mừng " + request.getUsername(),
                    request.getUsername()); // client lưu username vào session
        } else {
            logger.warning("LOGIN FAILURE | " + request.getUsername());
            return new Response(ResponseType.LOGIN_FAILURE,
                    "Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    // ── Danh sách phiên ───────────────────────────────────────────────────────

    public Response handleGetAuctionList() {
        Collection<Auction> auctions = auctionManager.getAllAuctions();
        auctions.forEach(Auction::checkAndUpdateStatus);
        // Convert sang DTO để tránh lỗi serialize LocalDateTime
        List<AuctionDTO> dtos = auctions.stream()
                .map(AuctionDTO::from)
                .collect(java.util.stream.Collectors.toList());
        logger.info("AUCTION_LIST | " + dtos.size() + " phiên.");
        return new Response(ResponseType.AUCTION_LIST,
                "Danh sách phiên đấu giá.", dtos);
    }

    // ── Đặt giá ───────────────────────────────────────────────────────────────

    public Response handleBid(BidRequest request, ClientHandler handler) {
        // Validate input cơ bản
        if (request.getAuctionId() == null || request.getBidderId() == null) {
            return Response.error("Thiếu thông tin phiên hoặc người đặt giá.");
        }
        if (request.getBidAmount() <= 0) {
            return Response.error("Số tiền đặt giá phải lớn hơn 0.");
        }

        // Parse auctionId String → int (AuctionManager của nhóm dùng int)
        int auctionId;
        try {
            auctionId = Integer.parseInt(request.getAuctionId());
        } catch (NumberFormatException e) {
            return Response.error("ID phiên không hợp lệ: " + request.getAuctionId());
        }

        // Lấy Auction từ AuctionManager của nhóm
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            return Response.error("Phiên đấu giá không tồn tại: " + auctionId);
        }

        // Lấy Bidder object — cần để gọi auction.placeBid(bidder, amount)
        Bidder bidder = auctionDao.findBidderByUsername(request.getBidderId());
        if (bidder == null) {
            return Response.error("Người dùng không tồn tại hoặc không phải Bidder: "
                    + request.getBidderId());
        }

        logger.info(String.format("BID REQUEST | Phiên %d | %s → %.0f",
                auctionId, request.getBidderId(), request.getBidAmount()));

        try {
            // Gọi vào logic của nhóm:
            //   - ReentrantLock bên trong ngăn lost update
            //   - checkAndUpdateStatus() tự chuyển trạng thái
            //   - Validate amount > currentPrice
            //   - Anti-sniping: gia hạn nếu bid trong 1 phút cuối
            //   - notifyObservers() → ClientHandlerObserver → broadcast BID_UPDATE
            auction.placeBid(bidder, request.getBidAmount());

            // Persist trạng thái mới
            auctionDao.saveAuction(auction);

            logger.info("BID SUCCESS | Phiên " + auctionId
                    + " | Giá mới: " + auction.getCurrentPrice());

            return new Response(ResponseType.BID_SUCCESS, "Đặt giá thành công!", AuctionDTO.from(auction));

        } catch (InvalidBidException e) {
            // Giá thấp hơn hiện tại — lỗi nghiệp vụ bình thường
            logger.info("BID INVALID | " + e.getMessage());
            return new Response(ResponseType.BID_FAILURE, e.getMessage());

        } catch (AuctionClosedException e) {
            // Phiên đã FINISHED / CANCELED / chưa RUNNING
            logger.info("BID CLOSED | " + e.getMessage());
            return new Response(ResponseType.BID_FAILURE, e.getMessage());
        }
    }

    // ── Subscribe / Unsubscribe realtime ──────────────────────────────────────

    /**
     * Client đăng ký theo dõi realtime một phiên.
     *
     * Tạo ClientHandlerObserver (Adapter) rồi đăng ký vào Auction.
     * Từ lúc này, mỗi lần Auction.notifyObservers() được gọi,
     * client này sẽ nhận BID_UPDATE qua socket.
     */
    public Response handleCreateAuction(JsonObject json, ClientHandler handler) {
        try {
            String itemName   = json.get("itemName").getAsString();
            String itemType   = json.has("itemType") ? json.get("itemType").getAsString() : "ELECTRONICS";
            double startPrice = json.get("startPrice").getAsDouble();
            String startTimeStr = json.has("startTime") ? json.get("startTime").getAsString() : null;
            String endTimeStr   = json.has("endTime")   ? json.get("endTime").getAsString()   : null;

            // Tạo Item
            com.auction.project.Entities.Item item = ItemFactory.createItem(itemType, itemName, startPrice, "Unknown");

            // Parse thời gian
            LocalDateTime startTime = startTimeStr != null
                    ? LocalDateTime.parse(startTimeStr)
                    : LocalDateTime.now();
            LocalDateTime endTime = endTimeStr != null
                    ? LocalDateTime.parse(endTimeStr)
                    : LocalDateTime.now().plusHours(2);

            // Tạo Auction
            com.auction.project.Entities.Auction auction =
                    new com.auction.project.Entities.Auction(startTime, endTime, startPrice, item);
            auction.setStatus(AuctionStatus.RUNNING);

            // Thêm vào AuctionManager và DAO
            auctionManager.addAuction(auction);
            auctionDao.saveAuction(auction);
            observerRegistry.put(auction.getId(), new java.util.ArrayList<>());

            logger.info("CREATE_AUCTION | " + itemName + " | ID: " + auction.getId());

            return new Response(ResponseType.BID_SUCCESS,
                    "Tạo phiên đấu giá thành công!", AuctionDTO.from(auction));

        } catch (Exception e) {
            logger.warning("Lỗi tạo phiên: " + e.getMessage());
            return Response.error("Không thể tạo phiên: " + e.getMessage());
        }
    }

    public Response handleSubscribeAuction(String auctionIdStr, ClientHandler handler) {
        int auctionId;
        try {
            auctionId = Integer.parseInt(auctionIdStr);
        } catch (NumberFormatException e) {
            return Response.error("ID phiên không hợp lệ.");
        }

        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            return Response.error("Phiên không tồn tại: " + auctionId);
        }

        // Tạo Adapter bridge và đăng ký vào Auction của nhóm
        ClientHandlerObserver observer = new ClientHandlerObserver(handler);
        auction.registerObserver(observer);

        // Lưu vào registry để unregister đúng khi disconnect
        observerRegistry
                .computeIfAbsent(auctionId, k -> new ArrayList<>())
                .add(observer);

        logger.info("SUBSCRIBE | Phiên " + auctionId + " | Client: " + handler.getClientId());

        // Trả về trạng thái hiện tại của phiên ngay lập tức
        auction.checkAndUpdateStatus();
        return new Response(ResponseType.BID_UPDATE,
                "Đã đăng ký theo dõi phiên " + auctionId, AuctionDTO.from(auction));
    }

    public void handleUnsubscribeAuction(String auctionIdStr, ClientHandler handler) {
        try {
            int auctionId = Integer.parseInt(auctionIdStr);
            Auction auction = auctionManager.getAuction(auctionId);
            if (auction != null) {
                removeObserverFromAuction(auctionId, auction, handler);
            }
        } catch (NumberFormatException ignored) {}
    }

    /**
     * Client ngắt kết nối — xóa khỏi tất cả phiên đang subscribe.
     * Quan trọng: nếu không xóa, Auction.notifyObservers() sẽ cố gửi
     * vào socket đã đóng → SocketException.
     */
    public void handleClientDisconnect(ClientHandler handler) {
        observerRegistry.forEach((auctionId, list) -> {
            Auction auction = auctionManager.getAuction(auctionId);
            if (auction != null) {
                removeObserverFromAuction(auctionId, auction, handler);
            }
        });
        logger.info("DISCONNECT cleanup | Client: " + handler.getClientId());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void removeObserverFromAuction(int auctionId, Auction auction,
                                           ClientHandler handler) {
        List<ClientHandlerObserver> list = observerRegistry.get(auctionId);
        if (list == null) return;
        list.stream()
                .filter(obs -> obs.getHandler() == handler)
                .findFirst()
                .ifPresent(obs -> {
                    auction.removeObserver(obs); // gọi removeObserver của nhóm
                    list.remove(obs);
                });
    }
}