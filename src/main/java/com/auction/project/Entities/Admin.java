package com.auction.project.Entities;

import com.auction.project.Entities.enums.AuctionStatus;

public class Admin extends User {
    public Admin(String username, String email) { super(username, email); }
    public void cancelAuction(Auction auction) { auction.setStatus(AuctionStatus.CANCELED); }
}