package com.auction.project.Entities;

import com.auction.project.Exception.InvalidBidException;
import com.auction.project.ManagerServer.BidTransaction;
import com.auction.project.Observer.Observer;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User implements Observer {
    private List<BidTransaction> biddingHistory = new ArrayList<>();

    public Bidder() { super(); }

    @Override
    public void update(int auctionId, double newPrice, String bidderName) {
        System.out.println("[Thông báo " + getUsername() + "]: Cuộc đấu giá #" + auctionId +
                " Giá tăng đến " + newPrice + " bởi " + bidderName);
    }

    public void addTransaction(BidTransaction bt) { biddingHistory.add(bt); }
}
