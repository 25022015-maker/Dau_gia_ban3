package com.auction.project.Common.entitiesclasses;
import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.locks.ReentrantLock;

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
    private List<Observer> observers = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();

    public Auction(String auctionName, LocalDateTime startTime, LocalDateTime endTime, double initialPrice) {
        super(); // Khởi tạo ID và createdAt từ Entity
        this.auctionName = auctionName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = initialPrice;
        this.status = AuctionStatus.OPEN;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }
    @Override
    public void update(String message) {
        System.out.println("Cập nhật hệ thống đấu giá: " + message);
    }

    public boolean placeBid(Bidder bidder, double bidAmount) {
        lock.lock();
        try {
            if (this.status != AuctionStatus.RUNNING) {
                System.out.println("Lỗi: Phiên đấu giá hiện không diễn ra.");
                return false;
            }

            if (LocalDateTime.now().isAfter(endTime)) {
                this.status = AuctionStatus.FINISHED;
                return false;
            }

            if (bidAmount > currentPrice) {
                this.currentPrice = bidAmount;
                this.currentBidder = bidder;
                notifyAllObservers();
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void notifyAllObservers() {
        String msg = "Giá mới cho [" + auctionName + "] là: " + currentPrice + " bởi " + currentBidder.getUsername();
        for (Observer obs : observers) {
            obs.update(msg);
        }
    }

    public String getAuctionName() { return auctionName; }
    public double getCurrentPrice() { return currentPrice; }
    public AuctionStatus getStatus() { return status; }

    public String getInfo(){
        return "Auction: "+ getAuctionName() +" - id:  "+getId();
    }
    public void update (double newPrice, Bidder bidder){
      this.currentPrice = newPrice;
      this.currentBidder = bidder;
    }

}
