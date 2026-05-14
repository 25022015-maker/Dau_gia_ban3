package com.auction.project.Observer;

public interface Observer {
    void update(int auctionId, double newPrice, String bidderName);
}