package com.auction.project.Models;

import com.auction.project.Server.ClientHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Trung tâm quản lý toàn bộ phiên đấu giá trong hệ thống.
 *
 * <p><b>Singleton Pattern:</b> Đảm bảo chỉ có DUY NHẤT một instance tồn tại
 * trong suốt vòng đời của Server. Mọi component đều truy cập qua
 * {@code AuctionManager.getInstance()}.
 *
 * <p><b>Observer Pattern:</b> {@code AuctionManager} đóng vai trò Subject.
 * Các {@code ClientHandler} đăng ký làm Observer. Khi có bid mới hợp lệ,
 * AuctionManager tự động notify tất cả Observer đang quan sát phiên đó.
 *
 * <p><b>Concurrency:</b> Mỗi phiên đấu giá có một {@code ReentrantLock} riêng.
 * Điều này cho phép nhiều phiên xử lý bid đồng thời mà không block lẫn nhau
 * (fine-grained locking), tránh bottleneck so với dùng một lock chung.
 */
public class AuctionManager {

    private static final Logger logger = Logger.getLogger(AuctionManager.class.getName());

    // ── Singleton Implementation (Bill Pugh — thread-safe, lazy init) ─────────

    private AuctionManager() {
        // Private constructor — ngăn khởi tạo từ bên ngoài
    }

    /**
     * Holder class — JVM chỉ load khi được gọi lần đầu, đảm bảo thread-safe
     * mà không cần synchronized trên toàn bộ getInstance().
     */
    private static class SingletonHolder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    /**
     * Truy cập instance duy nhất của AuctionManager.
     * @return instance singleton
     */
    public static AuctionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // ── Data Stores ───────────────────────────────────────────────────────────

    /**
     * Map lưu trữ tất cả phiên đấu giá: auctionId → Auction.
     * Dùng HashMap thường vì chúng ta tự quản lý lock ở tầng dưới.
     */
    private final Map<String, Auction> auctions = new HashMap<>();

    /**
     * Map lưu Lock riêng cho từng phiên đấu giá.
     * Key: auctionId — Value: ReentrantLock của phiên đó.
     * Fine-grained locking: chỉ lock phiên đang được bid, không block toàn hệ thống.
     */
    private final Map<String, ReentrantLock> auctionLocks = new HashMap<>();

    /**
     * Observer registry: auctionId → danh sách ClientHandler đang "xem" phiên đó.
     * Khi có BID_UPDATE, tất cả handler trong list sẽ được notify.
     */
    private final Map<String, List<ClientHandler>> observers = new HashMap<>();

    /** Lock bảo vệ việc thêm/xóa observer và thêm phiên mới (map-level operations) */
    private final ReentrantLock managerLock = new ReentrantLock();

    // ── Auction Management ────────────────────────────────────────────────────

    /**
     * Thêm một phiên đấu giá mới vào hệ thống.
     * Tự động tạo Lock và danh sách Observer cho phiên này.
     *
     * @param auction phiên đấu giá cần thêm
     */
    public void addAuction(Auction auction) {
        managerLock.lock();
        try {
            auctions.put(auction.getAuctionId(), auction);
            auctionLocks.put(auction.getAuctionId(), new ReentrantLock());
            observers.put(auction.getAuctionId(), new ArrayList<>());
            logger.info("Đã thêm phiên đấu giá: " + auction.getAuctionId());
        } finally {
            managerLock.unlock();
        }
    }

    /**
     * Lấy phiên đấu giá theo ID.
     *
     * @param auctionId ID phiên cần tìm
     * @return Auction nếu tồn tại, null nếu không
     */
    public Auction getAuction(String auctionId) {
        return auctions.get(auctionId);
    }

    /**
     * Trả về danh sách tất cả phiên đấu giá (bất biến — read-only view).
     *
     * @return unmodifiable list chứa tất cả phiên
     */
    public List<Auction> getAllAuctions() {
        return Collections.unmodifiableList(new ArrayList<>(auctions.values()));
    }

    // ── Core Bid Logic (Thread-safe) ──────────────────────────────────────────

    /**
     * Xử lý một lượt đặt giá từ client.
     *
     * <p><b>Luồng xử lý:</b>
     * <ol>
     *   <li>Acquire ReentrantLock của phiên đó (chỉ block thread khác bid CÙNG phiên)</li>
     *   <li>Kiểm tra tính hợp lệ (phiên tồn tại, đang chạy, giá hợp lệ)</li>
     *   <li>Gọi {@code auction.placeBid()} để cập nhật leader</li>
     *   <li>Notify toàn bộ Observer — broadcast BID_UPDATE</li>
     *   <li>Release lock</li>
     * </ol>
     *
     * @param auctionId ID phiên đấu giá
     * @param bidderId  username người đặt giá
     * @param amount    số tiền đặt
     * @return {@code BidResult} chứa trạng thái thành công/thất bại và thông báo
     */
    public BidResult processBid(String auctionId, String bidderId, double amount) {
        // Lấy lock của phiên này (không ảnh hưởng các phiên khác)
        ReentrantLock lock = auctionLocks.get(auctionId);
        if (lock == null) {
            return BidResult.failure("Phiên đấu giá không tồn tại: " + auctionId);
        }

        lock.lock(); // ← Chỉ một thread được xử lý bid trong phiên này tại một thời điểm
        try {
            Auction auction = auctions.get(auctionId);
            if (auction == null) {
                return BidResult.failure("Phiên đấu giá không tồn tại.");
            }
            if (!auction.isAcceptingBids()) {
                return BidResult.failure("Phiên đấu giá không còn nhận bid (trạng thái: "
                        + auction.getStatus() + ")");
            }
            if (amount <= auction.getCurrentPrice()) {
                return BidResult.failure(
                        String.format(
                                "Giá đặt (%.0f) phải cao hơn giá hiện tại (%.0f).",
                                amount, auction.getCurrentPrice()));
            }

            // Bid hợp lệ — cập nhật trạng thái phiên
            boolean accepted = auction.placeBid(bidderId, amount);
            if (!accepted) {
                return BidResult.failure("Không thể đặt giá lúc này.");
            }

            logger.info(
                    String.format("BID ACCEPTED | Phiên %s | %s đặt %.0f", auctionId, bidderId, amount));

            // Notify tất cả observer đang xem phiên này (Observer Pattern)
            notifyObservers(auction);

            return BidResult.success(auction);

        } finally {
            lock.unlock(); // ← Luôn release lock dù có exception
        }
    }

    // ── Observer Pattern ──────────────────────────────────────────────────────

    /**
     * Đăng ký một ClientHandler để nhận thông báo khi phiên có bid mới.
     * Được gọi khi client "mở" màn hình chi tiết phiên đấu giá.
     *
     * @param auctionId ID phiên muốn theo dõi
     * @param handler   ClientHandler của client đăng ký
     */
    public void registerObserver(String auctionId, ClientHandler handler) {
        managerLock.lock();
        try {
            List<ClientHandler> list = observers.get(auctionId);
            if (list != null && !list.contains(handler)) {
                list.add(handler);
                logger.info("Observer registered | Phiên " + auctionId
                        + " | Client: " + handler.getClientId());
            }
        } finally {
            managerLock.unlock();
        }
    }

    /**
     * Hủy đăng ký Observer — gọi khi client ngắt kết nối hoặc thoát phiên.
     *
     * @param auctionId ID phiên
     * @param handler   ClientHandler cần xóa
     */
    public void unregisterObserver(String auctionId, ClientHandler handler) {
        managerLock.lock();
        try {
            List<ClientHandler> list = observers.get(auctionId);
            if (list != null) {
                list.remove(handler);
                logger.info("Observer unregistered | Phiên " + auctionId
                        + " | Client: " + handler.getClientId());
            }
        } finally {
            managerLock.unlock();
        }
    }

    /**
     * Hủy đăng ký một client khỏi TẤT CẢ phiên — gọi khi client disconnect.
     *
     * @param handler ClientHandler bị ngắt kết nối
     */
    public void unregisterFromAllAuctions(ClientHandler handler) {
        managerLock.lock();
        try {
            observers.values().forEach(list -> list.remove(handler));
        } finally {
            managerLock.unlock();
        }
    }

    /**
     * Broadcast thông tin giá mới tới TẤT CẢ client đang theo dõi phiên này.
     * Đây là bước "notify" trong Observer Pattern.
     *
     * <p>Mỗi ClientHandler sẽ tự gửi JSON response về cho client tương ứng
     * qua Socket riêng của nó — không có bottleneck chia sẻ socket.
     *
     * @param auction phiên vừa được cập nhật giá
     */
    private void notifyObservers(Auction auction) {
        // Lấy snapshot để tránh ConcurrentModificationException
        List<ClientHandler> snapshot;
        managerLock.lock();
        try {
            List<ClientHandler> list = observers.get(auction.getAuctionId());
            snapshot = list != null ? new ArrayList<>(list) : new ArrayList<>();
        } finally {
            managerLock.unlock();
        }

        // Gửi BID_UPDATE cho từng observer — mỗi handler tự lo việc gửi socket
        for (ClientHandler handler : snapshot) {
            handler.sendBidUpdate(auction);
        }
        logger.info("Broadcast BID_UPDATE | Phiên " + auction.getAuctionId()
                + " | Số observer: " + snapshot.size());
    }

    // ── Inner class: BidResult ────────────────────────────────────────────────

    /**
     * Kết quả xử lý một lượt bid.
     * Tránh dùng Exception cho control flow — dùng Result Object thay thế.
     */
    public static class BidResult {

        private final boolean success;
        private final String message;
        private final Auction auction; // null nếu bid thất bại

        private BidResult(boolean success, String message, Auction auction) {
            this.success = success;
            this.message = message;
            this.auction = auction;
        }

        public static BidResult success(Auction auction) {
            return new BidResult(true, "Đặt giá thành công!", auction);
        }

        public static BidResult failure(String reason) {
            return new BidResult(false, reason, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Auction getAuction() {
            return auction;
        }
    }
}