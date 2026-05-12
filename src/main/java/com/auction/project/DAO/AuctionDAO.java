package com.auction.project.DAO;

import com.auction.project.Entities.*;
import com.auction.project.Entities.enums.AuctionStatus;
import com.auction.project.Factory.ItemFactory;
import com.auction.project.Factory.UserFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Data Access Object — CHỈ Server mới được dùng class này.
 *
 * Dùng đúng Entities + Factory của nhóm.
 * Hiện tại giả lập bằng HashMap. Sau này thay bằng JDBC mà không
 * ảnh hưởng ServerController.
 */
public class AuctionDAO {

    private static final Logger logger = Logger.getLogger(AuctionDAO.class.getName());

    // ── Giả lập database ──────────────────────────────────────────────────────

    /** username → password (plaintext để demo) */
    private static final Map<String, String> userDatabase = new HashMap<>();

    /**
     * username → Bidder object.
     * ServerController cần Bidder object để gọi auction.placeBid(bidder, amount).
     */
    private static final Map<String, Bidder> bidderDatabase = new HashMap<>();

    /** auctionId → Auction */
    private static final Map<Integer, Auction> auctionDatabase = new HashMap<>();

    static {
        seedUsers();
        seedAuctions();
    }

    // ── Seed dữ liệu mẫu ─────────────────────────────────────────────────────

    private static void seedUsers() {
        // Tạo user và lưu password riêng (vì User class không lưu password raw)
        userDatabase.put("alice",   "alice123");
        userDatabase.put("bob",     "bob123");
        userDatabase.put("seller1", "seller123");
        userDatabase.put("admin",   "admin123");

        // Tạo Bidder dùng UserFactory của nhóm
        // UserFactory.createUser("BIDDER", username, email)
        bidderDatabase.put("alice", (Bidder) UserFactory.createUser("BIDDER", "alice", "alice@mail.com"));
        bidderDatabase.put("bob",   (Bidder) UserFactory.createUser("BIDDER", "bob",   "bob@mail.com"));

        logger.info("DAO: Đã seed " + userDatabase.size() + " người dùng mẫu.");
    }

    private static void seedAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // Dùng ItemFactory của nhóm: createItem(type, name, startPrice, ...extras)
        Item laptop = ItemFactory.createItem("ELECTRONICS", "Laptop Dell XPS 15", 5_000_000, "Dell");
        Item watch  = ItemFactory.createItem("ART",         "Rolex Submariner",   50_000_000, "Rolex", "2022");
        Item car    = ItemFactory.createItem("VEHICLE",     "Toyota Camry 2023",  800_000_000, "Toyota");

        Auction a1 = new Auction(now.minusMinutes(5),  now.plusHours(2), 5_000_000,   laptop);
        Auction a2 = new Auction(now.minusMinutes(10), now.plusHours(1), 50_000_000,  watch);
        Auction a3 = new Auction(now.plusHours(1),     now.plusHours(3), 800_000_000, car);

        // Set RUNNING cho phiên đang diễn ra
        a1.setStatus(AuctionStatus.RUNNING);
        a2.setStatus(AuctionStatus.RUNNING);
        // a3 giữ OPEN vì chưa bắt đầu

        auctionDatabase.put(a1.getId(), a1);
        auctionDatabase.put(a2.getId(), a2);
        auctionDatabase.put(a3.getId(), a3);

        logger.info("DAO: Đã seed " + auctionDatabase.size() + " phiên đấu giá mẫu.");
    }

    // ── User Operations ───────────────────────────────────────────────────────

    /**
     * Kiểm tra thông tin đăng nhập.
     * So sánh với userDatabase (lưu password riêng vì User class không có getter password).
     */
    public boolean validateUser(String username, String password) {
        String stored = userDatabase.get(username);
        boolean valid = stored != null && stored.equals(password);
        logger.info("Xác thực '" + username + "': " + (valid ? "OK" : "FAIL"));
        return valid;
    }

    /**
     * Lấy Bidder object theo username.
     * Được gọi trong ServerController.handleBid() để có object truyền vào
     * auction.placeBid(bidder, amount).
     *
     * @return Bidder nếu tìm thấy, null nếu không có hoặc không phải Bidder
     */
    public Bidder findBidderByUsername(String username) {
        return bidderDatabase.get(username);
    }

    // ── Auction Operations ────────────────────────────────────────────────────

    public List<Auction> findAllAuctions() {
        return new ArrayList<>(auctionDatabase.values());
    }

    public Auction findById(int auctionId) {
        return auctionDatabase.get(auctionId);
    }

    public void saveAuction(Auction auction) {
        auctionDatabase.put(auction.getId(), auction);
        logger.info("DAO: Đã lưu phiên " + auction.getId());
    }
}