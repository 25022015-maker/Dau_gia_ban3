package com.auction.project.DAO;

import com.auction.project.Models.Auction;
import com.auction.project.Models.Auction.Status;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Data Access Object cho hệ thống đấu giá.
 *
 * <p><b>Nguyên tắc quan trọng:</b> CHỈ Server mới được sử dụng class này.
 * Client không bao giờ truy cập trực tiếp vào database — mọi thao tác
 * phải đi qua Socket → ServerController → DAO.
 *
 * <p><b>Hiện tại:</b> Giả lập bằng in-memory HashMap (dữ liệu mẫu hardcoded).
 * Trong production, thay thế các phương thức này bằng JDBC / Hibernate
 * kết nối MySQL/PostgreSQL mà không ảnh hưởng đến các tầng trên.
 *
 * <p><b>Pattern:</b> DAO Pattern — tách biệt logic truy cập dữ liệu khỏi
 * business logic trong Model và Controller.
 */
public class AuctionDAO {

    private static final Logger logger = Logger.getLogger(AuctionDAO.class.getName());

    // ── Giả lập database bằng HashMap ─────────────────────────────────────────

    /** "Bảng" users: username → password (plaintext chỉ để demo — production phải hash!) */
    private static final Map<String, String> userDatabase = new HashMap<>();

    /** "Bảng" auctions: auctionId → Auction */
    private static final Map<String, Auction> auctionDatabase = new HashMap<>();

    // Khởi tạo dữ liệu mẫu khi class được load
    static {
        seedUsers();
        seedAuctions();
    }

    // ── Seeding (dữ liệu mẫu) ────────────────────────────────────────────────

    /** Tạo dữ liệu người dùng mẫu */
    private static void seedUsers() {
        userDatabase.put("admin", "admin123");
        userDatabase.put("alice", "alice123");
        userDatabase.put("bob", "bob123");
        userDatabase.put("seller1", "seller123");
        logger.info("DAO: Đã seed " + userDatabase.size() + " người dùng mẫu.");
    }

    /** Tạo dữ liệu phiên đấu giá mẫu */
    private static void seedAuctions() {
        LocalDateTime now = LocalDateTime.now();

        Auction laptop = new Auction(
                "A001",
                "Laptop Dell XPS 15",
                "Laptop cao cấp, i9, 32GB RAM, 1TB SSD",
                "seller1",
                5_000_000,
                now.minusMinutes(5),
                now.plusHours(2));
        laptop.setStatus(Status.RUNNING);

        Auction watch = new Auction(
                "A002",
                "Đồng hồ Rolex Submariner",
                "Rolex chính hãng, năm 2022, fullbox",
                "seller1",
                50_000_000,
                now.minusMinutes(10),
                now.plusHours(1));
        watch.setStatus(Status.RUNNING);

        Auction phone = new Auction(
                "A003",
                "iPhone 15 Pro Max",
                "256GB, Titanium Blue, chưa active",
                "seller1",
                20_000_000,
                now.plusHours(1),  // Chưa bắt đầu
                now.plusHours(3));
        phone.setStatus(Status.OPEN);

        auctionDatabase.put(laptop.getAuctionId(), laptop);
        auctionDatabase.put(watch.getAuctionId(), watch);
        auctionDatabase.put(phone.getAuctionId(), phone);

        logger.info("DAO: Đã seed " + auctionDatabase.size() + " phiên đấu giá mẫu.");
    }

    // ── User Operations ───────────────────────────────────────────────────────

    /**
     * Kiểm tra thông tin đăng nhập.
     *
     * <p>TODO production: Thay bằng truy vấn SQL với password hash:
     * {@code SELECT * FROM users WHERE username = ? AND password = SHA2(?, 256)}
     *
     * @param username tên đăng nhập
     * @param password mật khẩu plaintext (cần hash trong production)
     * @return true nếu thông tin khớp
     */
    public boolean validateUser(String username, String password) {
        String storedPassword = userDatabase.get(username);
        boolean valid = storedPassword != null && storedPassword.equals(password);
        logger.info("Xác thực người dùng '" + username + "': " + (valid ? "THÀNH CÔNG" : "THẤT BẠI"));
        return valid;
    }

    // ── Auction Operations ────────────────────────────────────────────────────

    /**
     * Lấy tất cả phiên đấu giá từ database.
     *
     * <p>TODO production: {@code SELECT * FROM auctions ORDER BY end_time}
     *
     * @return danh sách tất cả phiên đấu giá
     */
    public List<Auction> findAllAuctions() {
        return new ArrayList<>(auctionDatabase.values());
    }

    /**
     * Tìm phiên đấu giá theo ID.
     *
     * <p>TODO production: {@code SELECT * FROM auctions WHERE auction_id = ?}
     *
     * @param auctionId ID cần tìm
     * @return Auction nếu tồn tại, null nếu không
     */
    public Auction findById(String auctionId) {
        return auctionDatabase.get(auctionId);
    }

    /**
     * Lưu hoặc cập nhật một phiên đấu giá.
     *
     * <p>TODO production: INSERT với ON DUPLICATE KEY UPDATE
     *
     * @param auction phiên cần lưu
     */
    public void saveAuction(Auction auction) {
        auctionDatabase.put(auction.getAuctionId(), auction);
        logger.info("DAO: Đã lưu phiên đấu giá " + auction.getAuctionId());
    }

    /**
     * Lấy danh sách phiên đang chạy (trạng thái RUNNING).
     *
     * <p>TODO production: {@code SELECT * FROM auctions WHERE status = 'RUNNING'}
     *
     * @return danh sách phiên RUNNING
     */
    public List<Auction> findRunningAuctions() {
        List<Auction> running = new ArrayList<>();
        for (Auction auction : auctionDatabase.values()) {
            if (auction.getStatus() == Status.RUNNING) {
                running.add(auction);
            }
        }
        return running;
    }
}