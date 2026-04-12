package com.auction.project.entitiesclasses;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    private static AuctionManager instance;

    private List<Seller> sellers;

    private AuctionManager() {
        this.sellers = new ArrayList<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addSeller(Seller seller) {
        sellers.add(seller);
    }
}