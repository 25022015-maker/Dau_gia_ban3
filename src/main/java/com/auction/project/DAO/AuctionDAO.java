package com.auction.project.DAO;

import com.auction.project.Entities.*;
import com.auction.project.Entities.enums.AuctionStatus;
import com.auction.project.Factory.ItemFactory;
import com.auction.project.Factory.UserFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionDAO {

    private static final Logger logger = Logger.getLogger(AuctionDAO.class.getName());

    // ── Xác thực người dùng ───────────────────────────────────────────────────
    public boolean validateUser(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                return storedPassword != null && storedPassword.equals(password);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi Database khi đăng nhập: ", e);
        }
        return false;
    }

    // ── Tìm Bidder theo username ──────────────────────────────────────────────
    public Bidder findBidderByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ? AND role = 'BIDDER'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                return (Bidder) UserFactory.createUser("BIDDER", username, email);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi tìm kiếm Bidder: ", e);
        }
        return null;
    }

    // ── Lấy danh sách tất cả phiên đấu giá ────────────────────────────────────
    public List<Auction> findAllAuctions() {
        List<Auction> auctions = new ArrayList<>();
        String query = "SELECT a.id, a.start_time, a.end_time, a.current_price, a.status, " +
                "i.name, i.type, i.start_price as item_price, i.extra_info " +
                "FROM auctions a JOIN items i ON a.item_id = i.id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // 1. Khởi tạo Item
                Product item = ItemFactory.createItem(
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getDouble("item_price"),
                        rs.getString("extra_info")
                );

                // 2. Xử lý an toàn thời gian (tránh NullPointerException nếu DB bị null)
                Timestamp startTs = rs.getTimestamp("start_time");
                Timestamp endTs = rs.getTimestamp("end_time");
                LocalDateTime startTime = startTs != null ? startTs.toLocalDateTime() : LocalDateTime.now();
                LocalDateTime endTime = endTs != null ? endTs.toLocalDateTime() : LocalDateTime.now().plusDays(1);

                // 3. Khởi tạo Auction
                Auction auction = new Auction(
                        startTime,
                        endTime,
                        rs.getDouble("current_price"),
                        item
                );

                // 4. Set trạng thái
                String statusStr = rs.getString("status");
                if (statusStr != null) {
                    auction.setStatus(AuctionStatus.valueOf(statusStr));
                }

                // 5. QUAN TRỌNG: Gán ID thực tế từ Database vào Object
                auction.setId(rs.getInt("id"));

                auctions.add(auction);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi tải danh sách phiên đấu giá: ", e);
        }
        return auctions;
    }

    // ── Cập nhật phiên đấu giá (Chỉ dùng khi có người Bid hoặc Update Status) ──
    public void saveAuction(Auction auction) {
        String query = "UPDATE auctions SET current_price = ?, status = ?, end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDouble(1, auction.getCurrentPrice());
            stmt.setString(2, auction.getStatus().name());

            if (auction.getEndTime() != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }

            stmt.setInt(4, auction.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("DAO: Đã cập nhật thành công phiên đấu giá #" + auction.getId() + " vào Database.");
            } else {
                logger.warning("DAO: Không tìm thấy phiên đấu giá #" + auction.getId() + " để cập nhật!");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi lưu phiên đấu giá: ", e);
        }
    }

    // ── TẠO MỚI PHIÊN ĐẤU GIÁ (INSERT VÀO CẢ 2 BẢNG ITEMS VÀ AUCTIONS) ────────
    public void createNewAuction(Auction auction) {
        String insertItemQuery = "INSERT INTO items (name, type, start_price, extra_info) VALUES (?, ?, ?, ?)";
        String insertAuctionQuery = "INSERT INTO auctions (item_id, start_time, end_time, current_price, status) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Tắt auto-commit để dùng Transaction (đảm bảo thêm Item và Auction cùng thành công hoặc cùng thất bại)
            conn.setAutoCommit(false);

            // 1. Thêm Item vào bảng items
            int itemId = -1;
            try (PreparedStatement itemStmt = conn.prepareStatement(insertItemQuery, Statement.RETURN_GENERATED_KEYS)) {
                Product item = auction.getItem();
                itemStmt.setString(1, item.getName());

                // Xác định type dựa trên class
                String type = "ELECTRONICS";
                if (item instanceof VehicleItem) type = "VEHICLE";
                else if (item instanceof ArtItem) type = "ART";

                itemStmt.setString(2, type);
                itemStmt.setDouble(3, auction.getCurrentPrice());
                itemStmt.setString(4, "Unknown"); // extra_info mặc định

                itemStmt.executeUpdate();

                // Lấy ID của item vừa tạo từ MySQL
                ResultSet rs = itemStmt.getGeneratedKeys();
                if (rs.next()) {
                    itemId = rs.getInt(1);
                    item.setId(itemId);
                }
            }

            // 2. Thêm Auction vào bảng auctions
            if (itemId != -1) {
                try (PreparedStatement auctionStmt = conn.prepareStatement(insertAuctionQuery, Statement.RETURN_GENERATED_KEYS)) {
                    auctionStmt.setInt(1, itemId);
                    auctionStmt.setTimestamp(2, Timestamp.valueOf(auction.getStartTime()));
                    auctionStmt.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
                    auctionStmt.setDouble(4, auction.getCurrentPrice());
                    auctionStmt.setString(5, auction.getStatus().name());

                    auctionStmt.executeUpdate();

                    // Lấy ID của auction vừa tạo và gán ngược lại cho object Auction
                    ResultSet rs2 = auctionStmt.getGeneratedKeys();
                    if (rs2.next()) {
                        int auctionId = rs2.getInt(1);
                        auction.setId(auctionId); // RẤT QUAN TRỌNG: Cập nhật ID thực tế từ DB
                    }
                }
            }

            conn.commit(); // Lưu toàn bộ thay đổi
            logger.info("DAO: Đã tạo mới thành công phiên đấu giá #" + auction.getId() + " vào Database.");
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            logger.log(Level.SEVERE, "Lỗi khi tạo mới phiên đấu giá: ", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}