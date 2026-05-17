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

    public boolean registerUser(String username, String password, String email, String role) {
        String query = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);
            stmt.setString(4, role);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi Database khi đăng ký: ", e);
        }
        return false;
    }

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
                Item item = ItemFactory.createItem(
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
                // Nếu không có dòng này, ID sẽ tự sinh (1000, 1001...) gây lỗi khi Update
                auction.setId(rs.getInt("id"));

                auctions.add(auction);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi tải danh sách phiên đấu giá: ", e);
        }
        return auctions;
    }

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
    // =========================================================================
    // 1. HÀM THÊM USER MỚI (DÙNG CHO ĐĂNG KÝ)
    // =========================================================================
    public boolean insertUser(String username, String password, String email, String role) {
        String query = "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);
            stmt.setString(4, role);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("DAO: Đã tạo user mới thành công: " + username);
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi tạo user mới: ", e);
        }
        return false;
    }

    // =========================================================================
    // 2. HÀM TẠO PHIÊN ĐẤU GIÁ MỚI (INSERT VÀO 2 BẢNG ITEMS VÀ AUCTIONS)
    // =========================================================================
    public boolean insertAuction(Auction auction) {
        String insertItemQuery = "INSERT INTO items (name, type, start_price, extra_info) VALUES (?, ?, ?, ?)";
        String insertAuctionQuery = "INSERT INTO auctions (item_id, start_time, end_time, current_price, status) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Tắt auto-commit để dùng Transaction (đảm bảo insert cả 2 bảng thành công hoặc không bảng nào cả)
            conn.setAutoCommit(false);

            // --- BƯỚC A: INSERT VÀO BẢNG ITEMS ---
            int itemId = -1;
            // Statement.RETURN_GENERATED_KEYS dùng để lấy ID tự tăng do MySQL tạo ra
            try (PreparedStatement stmtItem = conn.prepareStatement(insertItemQuery, Statement.RETURN_GENERATED_KEYS)) {
                stmtItem.setString(1, auction.getItem().getName());

                // Xác định loại sản phẩm
                String type = "ELECTRONICS";
                if (auction.getItem() instanceof VehicleItem) type = "VEHICLE";
                else if (auction.getItem() instanceof ArtItem) type = "ART";

                stmtItem.setString(2, type);
                stmtItem.setDouble(3, auction.getItem().getStartPrice());
                stmtItem.setString(4, "N/A"); // extra_info tạm thời để trống

                stmtItem.executeUpdate();

                // Lấy ID của item vừa tạo
                try (ResultSet rs = stmtItem.getGeneratedKeys()) {
                    if (rs.next()) {
                        itemId = rs.getInt(1);
                    }
                }
            }

            // --- BƯỚC B: INSERT VÀO BẢNG AUCTIONS ---
            try (PreparedStatement stmtAuction = conn.prepareStatement(insertAuctionQuery, Statement.RETURN_GENERATED_KEYS)) {
                stmtAuction.setInt(1, itemId);
                stmtAuction.setTimestamp(2, Timestamp.valueOf(auction.getStartTime()));
                stmtAuction.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
                stmtAuction.setDouble(4, auction.getCurrentPrice());
                stmtAuction.setString(5, auction.getStatus().name());

                stmtAuction.executeUpdate();

                // Lấy ID của auction vừa tạo và CẬP NHẬT NGƯỢC LẠI VÀO OBJECT TRONG RAM
                try (ResultSet rs = stmtAuction.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newAuctionId = rs.getInt(1);
                        auction.setId(newAuctionId); // RẤT QUAN TRỌNG: Đồng bộ ID giữa DB và RAM
                        logger.info("DAO: Đã tạo phiên đấu giá mới thành công với ID = " + newAuctionId);
                    }
                }
            }

            // Xác nhận lưu thay đổi
            conn.commit();
            return true;

        } catch (SQLException e) {
            // Nếu có lỗi ở bất kỳ bước nào, rollback (hoàn tác) toàn bộ
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            logger.log(Level.SEVERE, "Lỗi khi tạo phiên đấu giá mới: ", e);
            return false;
        } finally {
            // Trả lại trạng thái auto-commit và đóng kết nối
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) {}
            }
        }
    }
    public void saveBidTransaction(int auctionId, String bidderUsername, double amount) {
        String query = "INSERT INTO bid_transactions (auction_id, bidder_username, amount, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, auctionId);
            stmt.setString(2, bidderUsername);
            stmt.setDouble(3, amount);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khi lưu lịch sử đặt giá: ", e);
        }
    }
}