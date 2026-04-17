package com.auction.project.Observer;

public interface AuctionObserver {
    void onNewBidPlaced(String itemName, double newPrice, String bidderName);
}