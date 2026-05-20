package com.auction.project.Entities;

public class Admin extends User {
    public Admin() { super(); }
    public void cancelAuction(Auction auction) { auction.setStatus("CANCELED"); }
}
