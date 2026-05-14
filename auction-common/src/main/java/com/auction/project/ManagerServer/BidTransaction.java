package com.auction.project.ManagerServer;

import com.auction.project.Entities.Bidder;

import java.time.LocalDateTime;

public class BidTransaction {
    private double amount;
    private LocalDateTime timestamp;
    private Bidder bidder;

    public BidTransaction(Bidder bidder, double amount) {
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }
    public double getAmount() { return amount; }
    public Bidder getBidder() { return bidder; }
}
