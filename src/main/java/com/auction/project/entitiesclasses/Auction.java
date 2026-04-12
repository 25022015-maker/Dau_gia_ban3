package com.auction.project.entitiesclasses;
import java.util.*;
import java.time.LocalDateTime;
import com.auction.project.AuctionStatus;

public class Auction extends Entity implements Observer {
    private LocalDateTime timestamp;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private AuctionStatus status;
    private String auctionName;
    private Bidder currentBidder;
    List<Observer> observers;

    Auction(){};

    public String getAuctionName(){return auctionName;}

    public String getInfo(){
        return "Auction: "+ getAuctionName() +" - id:  "+getId();
    }
    public void update (double newPrice, Bidder bidder){
      this.currentPrice = newPrice;
      this.currentBidder = bidder;
    }

}
