package com.auction.project.service;

import com.auction.project.dto.*;
import com.auction.project.entity.*;
import com.auction.project.entity.enums.AuctionStatus;
import com.auction.project.entity.enums.Role;
import com.auction.project.exception.AuctionClosedException;
import com.auction.project.exception.InvalidBidException;
import com.auction.project.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service nghiệp vụ trung tâm - xử lý toàn bộ logic đấu giá.
 *
 * Các design pattern áp dụng:
 *   - Factory Method: ItemFactory.create() tạo đúng loại Item
 *   - Observer (WebSocket): broadcast BID_UPDATE tới tất cả subscriber
 *   - Singleton: Spring quản lý bean này là singleton
 *   - Template Method: placeBid() gọi validateBid(), applyAntiSnipe(), processAutoBids()
 *
 * Concurrent safety:
 *   - @Transactional + Pessimistic Write Lock tránh race condition khi nhiều user bid cùng lúc
 *   - Mỗi transaction acquire lock trên row auction → serialize các bid
 */
@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionRepository        auctionRepo;
    private final BidTransactionRepository txRepo;
    private final AutoBidRepository        autoBidRepo;
    private final UserRepository           userRepo;
    private final ItemRepository           itemRepo;
    private final NotificationRepository   notifRepo;
    private final SimpMessagingTemplate    ws;

    @Value("${app.auction.anti-snipe-trigger-seconds:60}")
    private int antiSnipeTriggerSeconds;

    public AuctionService(AuctionRepository auctionRepo,
                          BidTransactionRepository txRepo,
                          AutoBidRepository autoBidRepo,
                          UserRepository userRepo,
                          ItemRepository itemRepo,
                          NotificationRepository notifRepo,
                          SimpMessagingTemplate ws) {
        this.auctionRepo = auctionRepo;
        this.txRepo      = txRepo;
        this.autoBidRepo = autoBidRepo;
        this.userRepo    = userRepo;
        this.itemRepo    = itemRepo;
        this.notifRepo   = notifRepo;
        this.ws          = ws;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TẠO PHIÊN ĐẤU GIÁ (Seller / Admin)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo phiên đấu giá mới.
     * Áp dụng Factory Method Pattern để tạo đúng loại Item.
     */
    @Transactional
    public AuctionResponse createAuction(CreateAuctionRequest req, String sellerUsername) {
        User seller = findUser(sellerUsername);

        if (seller.getRole() != Role.SELLER && seller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Chỉ Seller mới có thể tạo phiên đấu giá");
        }

        LocalDateTime start = LocalDateTime.parse(req.startTime());
        LocalDateTime end   = LocalDateTime.parse(req.endTime());
        if (!end.isAfter(start)) {
            throw new InvalidBidException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        // Factory Method: tạo đúng loại Item
        Item item = ItemFactory.create(req, seller.getId());
        itemRepo.save(item);

        Auction auction = new Auction();
        auction.setItem(item);
        auction.setStartPrice(req.startPrice());
        auction.setCurrentPrice(req.startPrice());
        auction.setMinBid(req.minBid());
        auction.setStartTime(start);
        auction.setEndTime(end);
        auction.setSellerId(seller.getId());
        auction.setAntiSnipeExtSeconds(req.antiSnipeExtSeconds());
        auction.setStatus(LocalDateTime.now().isBefore(start)
                ? AuctionStatus.PENDING : AuctionStatus.RUNNING);

        Auction saved = auctionRepo.save(auction);
        log.info("CREATE_AUCTION | id={} | item={} | seller={}", saved.getId(), req.itemName(), sellerUsername);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LẤY DANH SÁCH / CHI TIẾT PHIÊN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AuctionResponse> getAll() {
        return auctionRepo.findAll().stream()
                .peek(Auction::refreshStatus)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuctionResponse getById(Long id) {
        Auction a = auctionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Phiên không tồn tại: " + id));
        a.refreshStatus();
        return toResponse(a);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ĐẶT GIÁ THỦ CÔNG
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Đặt giá thủ công.
     *
     * Luồng xử lý:
     *   1. Pessimistic Lock lấy auction → tránh concurrent bid conflict
     *   2. Validate trạng thái phiên (phải RUNNING)
     *   3. Validate giá đặt (phải > currentPrice + minBid)
     *   4. Cập nhật currentPrice và currentWinner
     *   5. Anti-sniping: gia hạn nếu bid trong giai đoạn cuối
     *   6. Lưu BidTransaction vào DB
     *   7. Observer broadcast: WebSocket → tất cả client đang xem phiên
     *   8. Kích hoạt auto-bid của các user khác (nếu có)
     */
    @Transactional
    public AuctionResponse placeBid(PlaceBidRequest req, String bidderUsername) {
        // Pessimistic Lock - serialize concurrent bids
        Auction auction = auctionRepo.findByIdWithLock(req.auctionId())
                .orElseThrow(() -> new RuntimeException("Phiên không tồn tại: " + req.auctionId()));

        auction.refreshStatus();

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá không đang chạy. Trạng thái: " + auction.getStatus());
        }

        User bidder = findUser(bidderUsername);

        // Seller không được tự bid sản phẩm của mình
        if (auction.getSellerId().equals(bidder.getId())) {
            throw new InvalidBidException("Người bán không thể tự đấu giá sản phẩm của mình");
        }

        // Validate giá
        validateBid(auction, req.amount(), bidderUsername);

        // Cập nhật giá
        auction.setCurrentPrice(req.amount());
        auction.setCurrentWinner(bidder);

        // Anti-sniping
        applyAntiSnipe(auction);

        auctionRepo.save(auction);

        // Lưu lịch sử bid
        txRepo.save(new BidTransaction(auction, bidder, req.amount(), "MANUAL"));

        log.info("BID | auction={} | bidder={} | amount={}", auction.getId(), bidderUsername, req.amount());

        // Observer: broadcast WebSocket
        broadcastUpdate(auction, "MANUAL");

        // Kích hoạt auto-bid người khác
        triggerAutoBids(auction, bidder.getId());

        return toResponse(auction);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ĐĂNG KÝ AUTO-BID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Đăng ký auto-bid cho người dùng.
     * Nếu đã có → cập nhật maxBid và increment.
     */
    @Transactional
    public void registerAutoBid(AutoBidRequest req, String username) {
        User user = findUser(username);
        Auction auction = auctionRepo.findById(req.auctionId())
                .orElseThrow(() -> new RuntimeException("Phiên không tồn tại"));

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Chỉ đăng ký auto-bid khi phiên đang RUNNING");
        }
        if (req.maxBid() <= auction.getCurrentPrice()) {
            throw new InvalidBidException("maxBid phải cao hơn giá hiện tại: " + auction.getCurrentPrice());
        }

        autoBidRepo.findByAuctionIdAndUserId(auction.getId(), user.getId())
                .ifPresentOrElse(
                    existing -> {
                        existing.setMaxBid(req.maxBid());
                        existing.setIncrement(req.increment());
                        existing.setIsActive(true);
                        autoBidRepo.save(existing);
                    },
                    () -> autoBidRepo.save(new AutoBid(auction, user, req.maxBid(), req.increment()))
                );

        log.info("AUTO_BID_REGISTER | user={} | auction={} | maxBid={}", username, req.auctionId(), req.maxBid());
    }

    /**
     * Kích hoạt auto-bid sau mỗi lần đặt giá.
     * Sắp xếp theo createdAt ASC = người đăng ký trước ưu tiên (tie-breaking rule).
     * Gọi đệ quy để xử lý chuỗi auto-bid liên tiếp.
     */
    @Transactional
    public void triggerAutoBids(Auction auction, Long excludeUserId) {
        List<AutoBid> autoBids = autoBidRepo
                .findByAuctionIdAndIsActiveTrueOrderByCreatedAtAsc(auction.getId());

        for (AutoBid ab : autoBids) {
            Long abUserId = ab.getUser().getId();

            // Bỏ qua người vừa bid và người đang dẫn đầu
            if (abUserId.equals(excludeUserId)) continue;
            if (auction.getCurrentWinner() != null && abUserId.equals(auction.getCurrentWinner().getId())) continue;

            Long nextBid = auction.getCurrentPrice() + ab.getIncrement();

            // Kiểm tra số dư của người đăng ký auto-bid
            long userBalance = ab.getUser().getBalance() != null ? ab.getUser().getBalance() : 0L;
            if (userBalance < nextBid) {
                ab.setIsActive(false);
                autoBidRepo.save(ab);
                log.info("AUTO_BID_INSUFFICIENT_BALANCE | user={} | balance={} | needed={}",
                        ab.getUser().getUsername(), userBalance, nextBid);
                continue;
            }

            if (nextBid <= ab.getMaxBid()) {
                auction.setCurrentPrice(nextBid);
                auction.setCurrentWinner(ab.getUser());
                applyAntiSnipe(auction);
                auctionRepo.save(auction);

                txRepo.save(new BidTransaction(auction, ab.getUser(), nextBid, "AUTO"));

                log.info("AUTO_BID | user={} | auction={} | amount={}",
                        ab.getUser().getUsername(), auction.getId(), nextBid);

                broadcastUpdate(auction, "AUTO");

                // Đệ quy: trigger auto-bid của người khác tiếp theo
                triggerAutoBids(auction, abUserId);
                break;

            } else {
                // Đã đạt giới hạn → vô hiệu hóa
                ab.setIsActive(false);
                autoBidRepo.save(ab);
                log.info("AUTO_BID_MAXED | user={} | maxBid={}", ab.getUser().getUsername(), ab.getMaxBid());
            }

        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LỊCH SỬ BID
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BidTransactionResponse> getBidHistory(Long auctionId) {
        return txRepo.findByAuctionIdOrderByBidTimeDesc(auctionId)
                .stream()
                .map(tx -> new BidTransactionResponse(
                        tx.getId(),
                        tx.getAuction().getId(),
                        tx.getBidder().getId(),
                        tx.getBidder().getUsername(),
                        tx.getAmount(),
                        tx.getBidTime() != null ? tx.getBidTime().toString() : "",
                        tx.getBidType()
                ))
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SCHEDULER: MỞ / ĐÓNG PHIÊN TỰ ĐỘNG
    // ─────────────────────────────────────────────────────────────────────────

    /** Gọi bởi AuctionScheduler mỗi 10 giây - mở phiên PENDING đến giờ */
    @Transactional
    public void startPendingAuctions() {
        auctionRepo.findAuctionsToStart(LocalDateTime.now()).forEach(a -> {
            a.setStatus(AuctionStatus.RUNNING);
            auctionRepo.save(a);
            log.info("AUCTION_STARTED | id={}", a.getId());
        });
    }

    /** Gọi bởi AuctionScheduler mỗi 10 giây - đóng phiên RUNNING hết giờ */
    @Transactional
    public void closeExpiredAuctions() {
        auctionRepo.findExpiredAuctions(LocalDateTime.now()).forEach(a -> {
            if (a.getCurrentWinner() == null) {
                a.setStatus(AuctionStatus.CANCELED);
            } else {
                a.setStatus(AuctionStatus.FINISHED);

                // Trừ tiền người thắng
                User winner = a.getCurrentWinner();
                long winAmount = a.getCurrentPrice();
                long currentBalance = winner.getBalance() != null ? winner.getBalance() : 0L;
                winner.setBalance(Math.max(0L, currentBalance - winAmount));
                userRepo.save(winner);
                log.info("PAYMENT | auction={} | winner={} | amount={} | balanceBefore={} | balanceAfter={}",
                        a.getId(), winner.getUsername(), winAmount, currentBalance, winner.getBalance());

                // Gửi thông báo cho người thắng
                notifRepo.save(new Notification(
                        winner.getId(),
                        "🎉 Bạn đã thắng phiên đấu giá!",
                        "Chúc mừng! Bạn đã thắng phiên đấu giá \"" + a.getItem().getName()
                                + "\" với giá " + String.format("%,d", winAmount) + " VND."
                                + " Số dư còn lại: " + String.format("%,d", winner.getBalance()) + " VND."
                ));
            }
            auctionRepo.save(a);
            // Thông báo đóng phiên tới tất cả client
            broadcastUpdate(a, "SYSTEM");
            log.info("AUCTION_CLOSED | id={} | status={}", a.getId(), a.getStatus());
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void cancelAuction(Long auctionId, String requestingUsername) {
        Auction a = auctionRepo.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Phiên không tồn tại: " + auctionId));
        User requester = findUser(requestingUsername);
        if (requester.getRole() != Role.ADMIN && !requester.getId().equals(a.getSellerId())) {
            throw new AccessDeniedException("Bạn không có quyền hủy phiên này");
        }
        a.setStatus(AuctionStatus.CANCELED);
        auctionRepo.save(a);
        broadcastUpdate(a, "SYSTEM");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NOTIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String username) {
        User user = findUser(username);
        return notifRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public void markNotificationsRead(String username) {
        User user = findUser(username);
        notifRepo.findByUserIdAndIsReadFalse(user.getId()).forEach(n -> {
            n.setIsRead(true);
            notifRepo.save(n);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /** Validate giá đặt hợp lệ */
    private void validateBid(Auction auction, Long amount, String bidderUsername) {
        // Không bid thấp hơn currentPrice
        if (amount <= auction.getCurrentPrice()) {
            throw new InvalidBidException(String.format(
                    "Giá đặt (%,d VND) phải cao hơn giá hiện tại (%,d VND)",
                    amount, auction.getCurrentPrice()));
        }
        // Phải đủ bước giá tối thiểu
        long minRequired = auction.getCurrentPrice() + auction.getMinBid();
        if (amount < minRequired) {
            throw new InvalidBidException(String.format(
                    "Giá tối thiểu phải là %,d VND (giá hiện tại + bước giá %,d VND)",
                    minRequired, auction.getMinBid()));
        }
        // Không bid khi đang là người dẫn đầu
        if (auction.getCurrentWinner() != null
                && auction.getCurrentWinner().getUsername().equals(bidderUsername)) {
            throw new InvalidBidException("Bạn đang là người dẫn đầu, không cần bid lại!");
        }
        // Số dư phải đủ để trả nếu thắng
        User bidder = findUser(bidderUsername);
        long balance = bidder.getBalance() != null ? bidder.getBalance() : 0L;
        if (balance < amount) {
            throw new InvalidBidException(String.format(
                    "Số dư không đủ. Số dư hiện tại: %,d VND, cần: %,d VND",
                    balance, amount));
        }
    }

    /**
     * Anti-sniping Algorithm:
     * Nếu bid xảy ra trong antiSnipeTriggerSeconds giây cuối → gia hạn endTime thêm antiSnipeExtSeconds giây.
     * Ngăn "snipe" = đặt giá vào giây cuối cùng để thắng mà không cho ai phản ứng.
     */
    private void applyAntiSnipe(Auction auction) {
        LocalDateTime triggerTime = auction.getEndTime().minusSeconds(antiSnipeTriggerSeconds);
        if (LocalDateTime.now().isAfter(triggerTime)) {
            LocalDateTime newEnd = auction.getEndTime().plusSeconds(auction.getAntiSnipeExtSeconds());
            auction.setEndTime(newEnd);
            log.info("ANTI_SNIPE | auction={} | newEndTime={}", auction.getId(), newEnd);
        }
    }

    /**
     * Observer Pattern qua WebSocket:
     * Broadcast BID_UPDATE tới tất cả client subscribe /topic/auction/{id}.
     * Thay thế hoàn toàn cơ chế loop-qua-ClientHandler của SocketServer cũ.
     */
    private void broadcastUpdate(Auction auction, String bidType) {
        BidUpdateMessage msg = new BidUpdateMessage(
                auction.getId(),
                auction.getCurrentPrice(),
                auction.getCurrentWinner() != null ? auction.getCurrentWinner().getUsername() : null,
                auction.getCurrentWinner() != null ? auction.getCurrentWinner().getId() : null,
                auction.getEndTime().toString(),
                auction.getStatus().name(),
                bidType,
                txRepo.countByAuctionId(auction.getId()),
                java.time.LocalDateTime.now().toString()
        );
        ws.convertAndSend("/topic/auction/" + auction.getId(), msg);
    }

    private User findUser(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User không tồn tại: " + username));
    }

    /** Ánh xạ Auction entity → AuctionResponse DTO */
    public AuctionResponse toResponse(Auction a) {
        String itemType = "ELECTRONICS";
        if (a.getItem() instanceof ArtItem)         itemType = "ART";
        else if (a.getItem() instanceof VehicleItem) itemType = "VEHICLE";

        String sellerUsername = null;
        if (a.getSellerId() != null) {
            sellerUsername = userRepo.findById(a.getSellerId())
                    .map(User::getUsername).orElse(null);
        }

        return new AuctionResponse(
                a.getId(),
                a.getItem().getName(),
                itemType,
                a.getItem().getDetails(),
                a.getItem().getDescription(),
                a.getItem().getImageBase64(),
                a.getStartPrice(),
                a.getMinBid(),
                a.getCurrentPrice(),
                a.getCurrentWinner() != null ? a.getCurrentWinner().getUsername() : null,
                a.getCurrentWinner() != null ? a.getCurrentWinner().getId() : null,
                a.getStartTime().toString(),
                a.getEndTime().toString(),
                a.getStatus().name(),
                a.getSellerId(),
                sellerUsername,
                txRepo.countByAuctionId(a.getId())
        );
    }
}
