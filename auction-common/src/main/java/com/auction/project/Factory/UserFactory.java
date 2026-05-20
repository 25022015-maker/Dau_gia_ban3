package com.auction.project.Factory;

import com.auction.project.Entities.*;

public class UserFactory {
    public static User createUser(String type, String username, String email) {
        User user = switch (type.toUpperCase()) {
            case "BIDDER" -> new Bidder();
            case "SELLER" -> new Seller();
            case "ADMIN"  -> new Admin();
            default -> throw new IllegalArgumentException("Unknown user type: " + type);
        };
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }
}
