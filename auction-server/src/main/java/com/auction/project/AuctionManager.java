package com.auction.project;

import com.auction.project.Entities.Auction;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    private AuctionManager() {}

    private static class Holder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() {
        return Holder.INSTANCE;
    }

    public void addAuction(Auction auction) {
        auctions.put(auction.getId(), auction);
    }

    public Auction getAuction(int id) {
        return auctions.get(id);
    }

    public Collection<Auction> getAllAuctions() {
        return auctions.values();
    }
}