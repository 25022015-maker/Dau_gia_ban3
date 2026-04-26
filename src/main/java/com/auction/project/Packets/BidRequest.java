package com.auction.project.Packets;

import java.io.Serializable;

public class BidRequest implements Serializable {
    private double amount;

    public BidRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() { return amount; }
}