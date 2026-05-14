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
                return storedPassword.equals(password);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during login", e);
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
            logger.log(Level.SEVERE, "Error finding bidder", e);
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
                Product item = ItemFactory.createItem(
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getDouble("item_price"),
                        rs.getString("extra_info")
                );

                Auction auction = new Auction(
                        rs.getTimestamp("start_time").toLocalDateTime(),
                        rs.getTimestamp("end_time").toLocalDateTime(),
                        rs.getDouble("current_price"),
                        item
                );
                auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
                // Note: You might need a setter for ID in Entity.java to map the DB ID back to the object

                auctions.add(auction);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading auctions", e);
        }
        return auctions;
    }

    public void saveAuction(Auction auction) {
        String query = "UPDATE auctions SET current_price = ?, status = ?, end_time = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDouble(1, auction.getCurrentPrice());
            stmt.setString(2, auction.getStatus().name());
            stmt.setTimestamp(3, Timestamp.valueOf(auction.getEndTime()));
            stmt.setInt(4, auction.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info("DAO: Updated auction " + auction.getId() + " in database.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error saving auction", e);
        }
    }
}