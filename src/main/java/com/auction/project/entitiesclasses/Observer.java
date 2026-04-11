package com.auction.project.entitiesclasses;

interface Observer {
    void update (int AuctionID, double newPrice, String bidderName);
}
