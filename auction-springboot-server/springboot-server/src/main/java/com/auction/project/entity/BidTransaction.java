package com.auction.project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Giao dịch đặt giá - lưu toàn bộ lịch sử bid.
 *
 * Tương đương bảng bid_transactions trong schema.sql cũ.
 * Thêm trường bid_type để phân biệt bid thủ công vs auto-bid.
 */
@Entity
@Table(name = "bid_transactions")
public class BidTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(nullable = false)
    private Long amount;

    @CreationTimestamp
    @Column(name = "bid_time", updatable = false)
    private LocalDateTime bidTime;

    /** MANUAL: người dùng tự bid | AUTO: hệ thống auto-bid */
    @Column(name = "bid_type", length = 20)
    private String bidType = "MANUAL";

    // ── Constructor ───────────────────────────────────────────────────────────

    public BidTransaction() {}

    public BidTransaction(Auction auction, User bidder, Long amount, String bidType) {
        this.auction = auction;
        this.bidder  = bidder;
        this.amount  = amount;
        this.bidType = bidType;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public Auction getAuction()                  { return auction; }
    public void setAuction(Auction auction)      { this.auction = auction; }

    public User getBidder()                      { return bidder; }
    public void setBidder(User bidder)           { this.bidder = bidder; }

    public Long getAmount()                      { return amount; }
    public void setAmount(Long amount)           { this.amount = amount; }

    public LocalDateTime getBidTime()            { return bidTime; }
    public void setBidTime(LocalDateTime t)      { this.bidTime = t; }

    public String getBidType()                   { return bidType; }
    public void setBidType(String bidType)       { this.bidType = bidType; }
}
