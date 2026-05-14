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
}