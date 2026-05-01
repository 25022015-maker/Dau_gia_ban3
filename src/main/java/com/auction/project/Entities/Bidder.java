package com.auction.project.Entities;

import com.auction.project.Manager.BidTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

public class Bidder extends User implements Observer {
    private List<BidTransaction> biddingHistory = new ArrayList<>();

    public Bidder(String username, String email) { super(username, email); }

    public void placeBid(Auction auction, double amount) {
        auction.placeBid(this, amount);
    }

    @Override
    public void update(int auctionId, double newPrice, String bidderName) {
        System.out.println("[Thông báo " + username + "]: Cuộc đấu giá #" + auctionId +
                " Giá tăng đến " + newPrice + " bởi " + bidderName);
    }

    public void addTransaction(BidTransaction bt) { biddingHistory.add(bt); }
}
