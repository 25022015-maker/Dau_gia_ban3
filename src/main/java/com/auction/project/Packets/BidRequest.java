package com.auction.project.Packets;

import java.io.Serializable;
import java.time.Instant;

public class BidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private double amount;
    private String username;   // ai đặt giá
    private Instant timestamp; // khi nào đặt

    // Constructor mặc định (cần cho deserialization)
    public BidRequest() {}

    public BidRequest(double amount, String username) {
        if (amount <= 0) throw new IllegalArgumentException("Bid amount must be positive");
        this.amount    = amount;
        this.username  = username;
        this.timestamp = Instant.now();
    }

    public double  getAmount()    { return amount; }
    public String  getUsername()  { return username; }
    public Instant getTimestamp() { return timestamp; }

    // Setter cho amount (phòng khi cần deserialize thủ công)
    public void setAmount(double amount) { this.amount = amount; }

    @Override
    public String toString() {
        return String.format("BidRequest{amount=%.2f, user='%s', time=%s}",
                amount, username, timestamp);
    }
}