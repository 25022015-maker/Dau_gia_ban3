package com.auction.project.Entities;

import com.auction.project.Entities.enums.AuctionStatus;
import com.auction.project.ManagerServer.BidTransaction;
import com.auction.project.Observer.*;
import com.auction.project.Exception.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity implements Subject, Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double currentPrice;
    private AuctionStatus status;
    private Product item;

    private transient List<Observer> observers = new ArrayList<>();
    private List<BidTransaction> transactions = new ArrayList<>();

    public Auction(LocalDateTime start, LocalDateTime end, double startPrice, Product item) {
        super();
        this.startTime = start;
        this.endTime = end;
        this.currentPrice = startPrice;
        this.item = item;
        this.status = AuctionStatus.OPEN;
    }

    public void placeBid(Bidder bidder, double amount)
            throws InvalidBidException, AuctionClosedException {
        synchronized (this) {
            checkAndUpdateStatus();

            if (this.status != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá đang ở trạng thái: " + status);
            }

            if (amount <= currentPrice) {
                throw new InvalidBidException("Giá đặt phảI cao hơn giá hiện tại!");
            }

            // Ghi nhận đặt giá thành công
            this.currentPrice = amount;
            BidTransaction bt = new BidTransaction(bidder, amount);
            transactions.add(bt);

            // (Anti-sniping)
            if (LocalDateTime.now().isAfter(endTime.minusMinutes(1))) {
                this.endTime = this.endTime.plusMinutes(5);
            }
        }
        notifyObservers();
    }

    //Logic chuyển trạng thái tự động
    public synchronized void checkAndUpdateStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (status == AuctionStatus.OPEN && now.isAfter(startTime) && now.isBefore(endTime)) {
            status = AuctionStatus.RUNNING;
        } else if ((status == AuctionStatus.RUNNING || status == AuctionStatus.OPEN) && now.isAfter(endTime)) {
            status = AuctionStatus.FINISHED;
        }
    }

    @Override
    public void notifyObservers() {
        if (observers == null) return;

        // Lấy thông tin giá và người thắng một cách an toàn
        double priceToSend;
        String bidderToSend;
        synchronized (this) {
            priceToSend = this.currentPrice;
            bidderToSend = transactions.isEmpty() ? "None" :
                    transactions.get(transactions.size() - 1).getBidder().getUsername();
        }

        // Gửi cho các client
        for (Observer o : observers) {
            o.update(this.id, priceToSend, bidderToSend);
        }
    }

    @Override
    public void registerObserver(Observer o) {
        if (observers == null) observers = new ArrayList<>();
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        if (observers != null) observers.remove(o);
    }
    public double getCurrentPrice() { return this.currentPrice; }

    public void setStatus(AuctionStatus s) { this.status = s; }

    public String getLeadingBidder() {
        if (transactions == null || transactions.isEmpty()) return "None";
        return transactions.get(transactions.size()-1).getBidder().getUsername();
    }

    public String getItemName() {
        return item != null ? item.getName() : "Unknown";
    }

    public Product getItem() { return item; }

    public java.time.LocalDateTime getStartTime() { return startTime; }

    public java.time.LocalDateTime getEndTime() { return endTime; }

    public AuctionStatus getStatus() { return status; }

}