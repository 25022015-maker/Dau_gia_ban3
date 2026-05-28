package com.auction.project.entity;

import com.auction.project.entity.enums.AuctionStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity phiên đấu giá - trung tâm của hệ thống.
 *
 * Vòng đời: PENDING → RUNNING → FINISHED / CANCELED → PAID
 *
 * Tính năng:
 *   - Anti-sniping: gia hạn thời gian khi có bid trong X giây cuối
 *   - refreshStatus(): tự cập nhật trạng thái theo thời gian thực
 *   - Liên kết 1-1 với Item (sản phẩm), N-N với User (winner)
 */
@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sản phẩm được đấu giá - quan hệ 1-1 */
    @OneToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "item_id", nullable = false, unique = true)
    private Item item;

    @Column(name = "start_price", nullable = false)
    private Long startPrice;

    /** Bước giá tối thiểu mỗi lần đặt (đơn vị: VND) */
    @Column(name = "min_bid", nullable = false)
    private Long minBid = 1000L;

    /** Giá cao nhất hiện tại */
    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    /** Người đang dẫn đầu (null nếu chưa ai bid) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_winner_id")
    private User currentWinner;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status = AuctionStatus.PENDING;

    /**
     * Anti-sniping: số giây gia hạn khi có bid trong giai đoạn cuối.
     * Mặc định 60 giây (= 1 phút).
     */
    @Column(name = "anti_snipe_ext_seconds")
    private Integer antiSnipeExtSeconds = 60;

    /** ID người tạo phiên (Seller) */
    @Column(name = "seller_id")
    private Long sellerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Auction() {}

    // ── Business Logic ────────────────────────────────────────────────────────

    /**
     * Tự cập nhật trạng thái dựa theo thời gian hiện tại.
     * Gọi phương thức này trước khi trả dữ liệu về client.
     *
     * PENDING  → RUNNING   : khi đến startTime
     * RUNNING  → FINISHED  : khi qua endTime và có winner
     * RUNNING  → CANCELED  : khi qua endTime mà không có ai bid
     */
    public void refreshStatus() {
        if (status == AuctionStatus.WAITING_APPROVAL) return;
        LocalDateTime now = LocalDateTime.now();
        if (status == AuctionStatus.PENDING && !now.isBefore(startTime)) {
            status = AuctionStatus.RUNNING;
        }
        if (status == AuctionStatus.RUNNING && now.isAfter(endTime)) {
            status = (currentWinner != null) ? AuctionStatus.FINISHED : AuctionStatus.CANCELED;
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                              { return id; }
    public void setId(Long id)                       { this.id = id; }

    public Item getItem()                            { return item; }
    public void setItem(Item item)                   { this.item = item; }

    public Long getStartPrice()                      { return startPrice; }
    public void setStartPrice(Long startPrice)       { this.startPrice = startPrice; }

    public Long getMinBid()                          { return minBid; }
    public void setMinBid(Long minBid)               { this.minBid = minBid; }

    public Long getCurrentPrice()                    { return currentPrice; }
    public void setCurrentPrice(Long currentPrice)   { this.currentPrice = currentPrice; }

    public User getCurrentWinner()                   { return currentWinner; }
    public void setCurrentWinner(User currentWinner) { this.currentWinner = currentWinner; }

    public LocalDateTime getStartTime()              { return startTime; }
    public void setStartTime(LocalDateTime startTime){ this.startTime = startTime; }

    public LocalDateTime getEndTime()                { return endTime; }
    public void setEndTime(LocalDateTime endTime)    { this.endTime = endTime; }

    public AuctionStatus getStatus()                 { return status; }
    public void setStatus(AuctionStatus status)      { this.status = status; }

    public Integer getAntiSnipeExtSeconds()                    { return antiSnipeExtSeconds; }
    public void setAntiSnipeExtSeconds(Integer s)              { this.antiSnipeExtSeconds = s; }

    public Long getSellerId()                        { return sellerId; }
    public void setSellerId(Long sellerId)           { this.sellerId = sellerId; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime t)        { this.createdAt = t; }
}
