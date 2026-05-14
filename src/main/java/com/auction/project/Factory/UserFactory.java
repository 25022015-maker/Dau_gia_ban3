package com.auction.project.Factory;

import com.auction.project.Entities.*;

public class UserFactory {
    public static User createUser(String type, String username, String email) {
        return switch (type.toUpperCase()) {
            case "BIDDER" -> new Bidder(username, email);
            case "SELLER" -> new Seller(username, email);
            case "ADMIN" -> new Admin(username, email);
            default -> throw new IllegalArgumentException("Unknown user type");
        };
    }
}