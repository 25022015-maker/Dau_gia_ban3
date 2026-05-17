package com.auction.project.Controllers;

import com.auction.project.DAO.AuctionDAO;
import com.auction.project.Entities.Auction;
import com.auction.project.Entities.Bidder;
import com.auction.project.Exception.AuctionClosedException;
import com.auction.project.Exception.InvalidBidException;
import com.auction.project.AuctionManager;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class ServerController {

    private static final Logger logger = Logger.getLogger(ServerController.class.getName());

    private final AuctionManager auctionManager;
    private final AuctionDAO auctionDao;

    /**
     * Registry theo dõi Observer nào đang subscribe phiên nào.
     * Dùng để unregister đúng observer khi client disconnect.
     * Key: auctionId (int) — Value: list ClientHandlerObserver của phiên đó
     */
    private final Map<Integer, List<ClientHandlerObserver>> observerRegistry = new ConcurrentHashMap<>();

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
            // FIX 1.3: Dùng CopyOnWriteArrayList để tránh ConcurrentModificationException
            observerRegistry.put(auction.getId(), new CopyOnWriteArrayList<>());
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
            logger.info("LOGIN SUCCESS | " + request.getUsername() + " | Client: " + handler.getClientId());
            return new Response(
                    ResponseType.LOGIN_SUCCESS,
                    "Đăng nhập thành công! Chào mừng " + request.getUsername(),
                    request.getUsername());
        } else {
            logger.warning("LOGIN FAILURE | " + request.getUsername());
            return new Response(ResponseType.LOGIN_FAILURE, "Sai tên đăng nhập hoặc mật khẩu.");
        }
    }

    // ── Danh sách phiên ───────────────────────────────────────────────────────

    public Response handleGetAuctionList() {
        Collection<Auction> auctions = auctionManager.getAllAuctions();
        auctions.forEach(Auction::checkAndUpdateStatus);
        List<AuctionDTO> dtos = auctions.stream()
                .map(AuctionDTO::from)
                .collect(java.util.stream.Collectors.toList());
        logger.info("AUCTION_LIST | " + dtos.size() + " phiên.");
        return new Response(ResponseType.AUCTION_LIST, "Danh sách phiên đấu giá.", dtos);
    }

    // ── Đặt giá ───────────────────────────────────────────────────────────────

    public Response handleBid(BidRequest request, ClientHandler handler) {
        if (request.getAuctionId() == null || request.getBidderId() == null) {
            return Response.error("Thiếu thông tin phiên hoặc người đặt giá.");
        }
        if (request.getBidAmount() <= 0) {
            return Response.error("Số tiền đặt giá phải lớn hơn 0.");
        }

        int auctionId;
        try {
            auctionId = Integer.parseInt(request.getAuctionId());
        } catch (NumberFormatException e) {
            return Response.error("ID phiên không hợp lệ: " + request.getAuctionId());
        }

        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            return Response.error("Phiên đấu giá không tồn tại: " + auctionId);
        }

        Bidder bidder = auctionDao.findBidderByUsername(request.getBidderId());
        if (bidder == null) {
            return Response.error("Người dùng không tồn tại hoặc không phải Bidder: " + request.getBidderId());
        }

        logger.info(String.format("BID REQUEST | Phiên %d | %s → %.0f",
                auctionId, request.getBidderId(), request.getBidAmount()));

        try {
            auction.placeBid(bidder, request.getBidAmount());
            auctionDao.saveAuction(auction);
            auctionDao.saveBidTransaction(auctionId, request.getBidderId(), request.getBidAmount()); // Lưu lịch sử bid

            logger.info("BID SUCCESS | Phiên " + auctionId + " | Giá mới: " + auction.getCurrentPrice());
            return new Response(ResponseType.BID_SUCCESS, "Đặt giá thành công!", AuctionDTO.from(auction));

        } catch (InvalidBidException | AuctionClosedException e) {
            logger.info("BID FAILED | " + e.getMessage());
            return new Response(ResponseType.BID_FAILURE, e.getMessage());
        }
    }

    // ── Subscribe / Unsubscribe realtime ──────────────────────────────────────

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

        // FIX 1.3: Dùng CopyOnWriteArrayList
        List<ClientHandlerObserver> observers = observerRegistry.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>());

        // FIX 2.2: Tránh Memory Leak (Chỉ đăng ký nếu client chưa có trong danh sách)
        boolean alreadySubscribed = observers.stream().anyMatch(obs -> obs.getHandler() == handler);

        if (!alreadySubscribed) {
            ClientHandlerObserver observer = new ClientHandlerObserver(handler);
            auction.registerObserver(observer);
            observers.add(observer);
            logger.info("SUBSCRIBE | Phiên " + auctionId + " | Client: " + handler.getClientId());
        }

        auction.checkAndUpdateStatus();
        return new Response(ResponseType.BID_UPDATE, "Đã đăng ký theo dõi phiên " + auctionId, AuctionDTO.from(auction));
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

    private void removeObserverFromAuction(int auctionId, Auction auction, ClientHandler handler) {
        List<ClientHandlerObserver> list = observerRegistry.get(auctionId);
        if (list == null) return;

        // Dùng removeIf để xóa an toàn và gọi removeObserver của nhóm
        list.removeIf(obs -> {
            if (obs.getHandler() == handler) {
                auction.removeObserver(obs);
                return true;
            }
            return false;
        });
    }
    // Thêm hàm xử lý đăng ký
    public Response handleRegister(JsonObject json, ClientHandler handler) {
        String username = json.get("username").getAsString();
        String password = json.get("password").getAsString();
        String email = json.has("email") ? json.get("email").getAsString() : username + "@auction.com";
        String role = json.has("role") ? json.get("role").getAsString() : "BIDDER";

        boolean success = auctionDao.registerUser(username, password, email, role);
        if (success) {
            logger.info("REGISTER SUCCESS | " + username);
            return new Response(ResponseType.LOGIN_SUCCESS, "Đăng ký thành công!", username);
        } else {
            return Response.error("Tên đăng nhập đã tồn tại hoặc lỗi hệ thống.");
        }
    }

    // Sửa lại hàm handleCreateAuction
    public Response handleCreateAuction(JsonObject json, ClientHandler handler) {
        try {
            String itemName   = json.get("itemName").getAsString();
            String itemType   = json.has("itemType") ? json.get("itemType").getAsString() : "ELECTRONICS";
            double startPrice = json.get("startPrice").getAsDouble();
            String startTimeStr = json.has("startTime") ? json.get("startTime").getAsString() : null;
            String endTimeStr   = json.has("endTime")   ? json.get("endTime").getAsString()   : null;

            com.auction.project.Entities.Item item = ItemFactory.createItem(itemType, itemName, startPrice, "Unknown");
            if (json.has("description")) item.setDescription(json.get("description").getAsString());

            LocalDateTime startTime = startTimeStr != null ? LocalDateTime.parse(startTimeStr) : LocalDateTime.now();
            LocalDateTime endTime = endTimeStr != null ? LocalDateTime.parse(endTimeStr) : LocalDateTime.now().plusHours(2);

            Auction auction = new Auction(startTime, endTime, startPrice, item);
            auction.setStatus(AuctionStatus.RUNNING);

            auctionDao.insertAuction(auction); // Insert vào DB trước để lấy ID
            auctionManager.addAuction(auction); // Đưa vào Manager
            observerRegistry.put(auction.getId(), new CopyOnWriteArrayList<>());

            logger.info("CREATE_AUCTION | " + itemName + " | ID: " + auction.getId());
            return new Response(ResponseType.BID_SUCCESS, "Tạo phiên đấu giá thành công!", AuctionDTO.from(auction));

        } catch (Exception e) {
            logger.warning("Lỗi tạo phiên: " + e.getMessage());
            return Response.error("Không thể tạo phiên: " + e.getMessage());
        }
    }
    public Response handleSignup(JsonObject json, ClientHandler handler) {
        try {
            String username = json.get("username").getAsString();
            String password = json.get("password").getAsString();
            // Nếu client không gửi email/role thì dùng giá trị mặc định
            String email = json.has("email") ? json.get("email").getAsString() : username + "@gmail.com";
            String role = json.has("role") ? json.get("role").getAsString() : "BIDDER";

            // Gọi DAO để lưu vào Database
            boolean success = auctionDao.insertUser(username, password, email, role);

            if (success) {
                logger.info("SIGNUP SUCCESS | " + username + " | Client: " + handler.getClientId());
                return new Response(
                        ResponseType.LOGIN_SUCCESS,
                        "Đăng ký thành công! Chào mừng " + username,
                        username
                );
            } else {
                logger.warning("SIGNUP FAILURE | " + username + " (Có thể trùng tên)");
                return new Response(ResponseType.LOGIN_FAILURE, "Đăng ký thất bại. Tên đăng nhập có thể đã tồn tại.");
            }
        } catch (Exception e) {
            logger.warning("SIGNUP ERROR | Dữ liệu không hợp lệ: " + e.getMessage());
            return Response.error("Dữ liệu đăng ký không hợp lệ.");
        }
    }
}