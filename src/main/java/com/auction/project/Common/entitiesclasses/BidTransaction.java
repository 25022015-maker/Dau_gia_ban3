package com.auction.project.entitiesclasses;
import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private double amount;
    private Bidder bidder;
    private LocalDateTime timestamp; // được khởi tạo mỗi thời điểm đặt một bid mới

    public BidTransaction(double amount, LocalDateTime timestamp, Bidder bidder){
        this.amount = amount;
        this.timestamp = timestamp;
        this.bidder = bidder;
    }

    public double getAmount() {return this.amount;}

    public String getBidder(){return bidder.getInfo();}

    public LocalDateTime getInfo(){return this.timestamp;}
}