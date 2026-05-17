package com.auction.project.Entities;

import java.time.LocalDateTime;

public class Seller extends User {
    public Seller(String username, String email) { super(username, email); }

    public Auction createAuction(Item item, LocalDateTime start, LocalDateTime end, double startPrice) {
        return new Auction(start, end, startPrice, item);
    }
}
