package com.auction.project.Entities;


import jakarta.persistence.*;
import java.time.LocalDateTime;

@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false, unique = true)
    private Long itemId;

    @Column(name = "start_price", nullable = false)
    private Long startPrice;

    @Column(name = "min_bid", nullable = false)
    private Long minBid;

    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    @Column(name = "current_winner_id")
    private Long currentWinnerId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'Pending'")
    private String status = "Pending";

    @Column(name = "anti_snipping", columnDefinition = "INT DEFAULT 5")
    private Integer antiSnipping = 5;

    // Default Constructor required by JPA
    public Auction() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(Long startPrice) {
        this.startPrice = startPrice;
    }

    public Long getMinBid() {
        return minBid;
    }

    public void setMinBid(Long minBid) {
        this.minBid = minBid;
    }

    public Long getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Long currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Long getCurrentWinnerId() {
        return currentWinnerId;
    }

    public void setCurrentWinnerId(Long currentWinnerId) {
        this.currentWinnerId = currentWinnerId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAntiSnipping() {
        return antiSnipping;
    }

    public void setAntiSnipping(Integer antiSnipping) {
        this.antiSnipping = antiSnipping;
    }
}