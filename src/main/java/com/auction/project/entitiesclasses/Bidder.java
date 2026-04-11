package com.auction.project.entitiesclasses;
import java.util.*;

public class Bidder extends User {
    List<BidTransaction> biddingHistory = new ArrayList<>();

    public void addTransaction(BidTransaction t) {
        biddingHistory.add(t);
    }

    public void placeBid(Auction auction, double amount){}

    public String getInfo() {
        return ("Bidder: " + getUsername() + " - id:  " + getId());
    }

    public void getBidHistory() {
        for (BidTransaction t : biddingHistory) {
            System.out.println(t);
        }
    }

    public void setUpAutoBid(Auction auction, double maxBid, double increment){

    }
}