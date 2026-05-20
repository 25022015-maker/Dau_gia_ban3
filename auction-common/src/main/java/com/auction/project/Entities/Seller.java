package com.auction.project.Entities;

import java.time.LocalDateTime;

public class Seller extends User {
    public Seller() { super(); }

    public Auction createAuction(Item item, LocalDateTime start, LocalDateTime end, long startPrice) {
        Auction a = new Auction();
        a.setItemId(item.getId());
        a.setStartPrice(startPrice);
        a.setCurrentPrice(startPrice);
        a.setStartTime(start);
        a.setEndTime(end);
        return a;
    }
}
