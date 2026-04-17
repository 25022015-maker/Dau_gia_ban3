package com.auction.project.Manager;

import com.auction.project.Entities.Bidder;
import com.auction.project.Entities.Item;
import com.auction.project.Entities.enums.AuctionState;
import com.auction.project.Exception.AuctionClosedException;
import com.auction.project.Exception.InvalidBidException;
import com.auction.project.Observer.AuctionObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionManager {
    private static AuctionManager instance;

    private AuctionState currentState;
    private double currentHighestBid;
    private Bidder currentHighestBidder;

    private final ReentrantLock lock = new ReentrantLock();

    private List<AuctionObserver> observers = new ArrayList<>();

    private AuctionManager() {
        this.currentState = AuctionState.OPEN;
        this.currentHighestBid = 0;
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(Item item) {
        for (AuctionObserver obs : observers) {
            obs.onNewBidPlaced(item.getName(), currentHighestBid, currentHighestBidder.getName());
        }
    }

    public void setItemToAuction(Item item) {
        this.currentHighestBid = item.getStartingPrice();
        this.currentState = AuctionState.RUNNING;
    }

    public void placeBid(Bidder bidder, Item item, double bidAmount)
            throws InvalidBidException, AuctionClosedException {

        lock.lock();
        try {
            if (currentState != AuctionState.RUNNING) {
                throw new AuctionClosedException("Phiên đấu giá đã kết thúc hoặc chưa bắt đầu!");
            }

            if (bidAmount <= currentHighestBid) {
                throw new InvalidBidException("Giá đặt phải cao hơn giá hiện tại: " + currentHighestBid);
            }

            this.currentHighestBid = bidAmount;
            this.currentHighestBidder = bidder;

            System.out.println(bidder.getName() + " đặt giá thành công: " + bidAmount);

            notifyObservers(item);

        } finally {
            lock.unlock();
        }
    }

    public void finishAuction() {
        this.currentState = AuctionState.FINISHED;
    }
}