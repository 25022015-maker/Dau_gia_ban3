package com.auction.project.Entities;

import com.auction.project.Entities.enums.AuctionStatus;
import com.auction.project.Manager.BidTransaction;
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

    // transient vì danh sách observer (thường là ClientHandler) không thể bị Serialize/gửi qua mạng
    private transient List<Observer> observers = new ArrayList<>();
    private List<BidTransaction> transactions = new ArrayList<>();
    private Item item;

    public Auction(LocalDateTime start, LocalDateTime end, double startPrice, Item item) {
        super();
        this.startTime = start;
        this.endTime = end;
        this.currentPrice = startPrice;
        this.item = item;
        this.status = AuctionStatus.OPEN;
    }

    public synchronized void placeBid(Bidder bidder, double amount)
            throws InvalidBidException, AuctionClosedException {

        if (this.status != AuctionStatus.OPEN && this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("Phiên đấu giá hiện không chấp nhận đặt giá (Trạng thái: " + status + ")");
        }

        if (LocalDateTime.now().isAfter(endTime)) {
            this.status = AuctionStatus.FINISHED;
            throw new AuctionClosedException("Phiên đấu giá đã kết thúc vào lúc " + endTime);
        }

        if (amount <= currentPrice) {
            throw new InvalidBidException("Giá đặt (" + amount + ") phải cao hơn giá hiện tại (" + currentPrice + ")");
        }

        this.currentPrice = amount;
        BidTransaction bt = new BidTransaction(bidder, amount);
        transactions.add(bt);
        bidder.addTransaction(bt);

        if (LocalDateTime.now().isAfter(endTime.minusMinutes(1))) {
            this.endTime = this.endTime.plusMinutes(5);
        }
        notifyObservers();
    }

    public void setStatus(AuctionStatus s) { this.status = s; }
    public int getId() { return this.id; }
    public double getCurrentPrice() { return currentPrice; }

    @Override
    public void registerObserver(Observer o) {
        if (observers == null) observers = new ArrayList<>(); // Đề phòng trường hợp Deserialize ra null
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) { observers.remove(o); }

    @Override
    public void notifyObservers() {
        if (observers == null) return;
        for (Observer o : observers) {
            String name = transactions.isEmpty() ? "None" : transactions.get(transactions.size()-1).getBidder().username;
            o.update(this.id, this.currentPrice, name);
        }
    }
}