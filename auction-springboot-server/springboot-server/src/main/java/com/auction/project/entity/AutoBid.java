package com.auction.project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Đấu giá tự động (Auto-Bidding).
 *
 * Người dùng đăng ký maxBid và increment:
 *   - Khi có người khác bid → hệ thống tự bid bằng (currentPrice + increment)
 *   - Dừng lại khi đã đạt maxBid
 *   - Ưu tiên người đăng ký trước (sort theo createdAt ASC)
 *
 * Unique: mỗi user chỉ có 1 auto-bid active trên mỗi phiên.
 */
@Entity
@Table(name = "auto_bids",
       uniqueConstraints = @UniqueConstraint(columnNames = {"auction_id", "user_id"}))
public class AutoBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Giá tối đa người dùng chấp nhận trả */
    @Column(name = "max_bid", nullable = false)
    private Long maxBid;

    /** Bước tăng mỗi lần auto-bid */
    @Column(nullable = false)
    private Long increment;

    /** false khi đã đạt maxBid hoặc người dùng tắt */
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Constructor ───────────────────────────────────────────────────────────

    public AutoBid() {}

    public AutoBid(Auction auction, User user, Long maxBid, Long increment) {
        this.auction   = auction;
        this.user      = user;
        this.maxBid    = maxBid;
        this.increment = increment;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public Auction getAuction()                  { return auction; }
    public void setAuction(Auction auction)      { this.auction = auction; }

    public User getUser()                        { return user; }
    public void setUser(User user)               { this.user = user; }

    public Long getMaxBid()                      { return maxBid; }
    public void setMaxBid(Long maxBid)           { this.maxBid = maxBid; }

    public Long getIncrement()                   { return increment; }
    public void setIncrement(Long increment)     { this.increment = increment; }

    public Boolean getIsActive()                 { return isActive; }
    public void setIsActive(Boolean isActive)    { this.isActive = isActive; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDateTime t)    { this.createdAt = t; }
}
