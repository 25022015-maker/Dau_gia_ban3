package com.auction.project.entitiesclasses;
import java.util.*;
import java.time.LocalDateTime;

public class Auction implements Observer {
    private LocalDateTime timestamp;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private AuctionStatus status;
    List<Observer> observers;

    Auction(){};

    public void update (int AuctionID, double newPrice, String bidderName){

    }

}
