package com.auction.project.Packets;

import java.io.Serializable;

public class BidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private double amount;

    // Constructor mặc định
    public BidRequest() {}

    // Constructor có kiểm tra
    public BidRequest(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Bid amount must be positive");
        }
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "BidRequest{amount=" + amount + "}";
    }
}
