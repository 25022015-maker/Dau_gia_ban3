package com.auction.project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Thông báo hệ thống gửi cho người dùng.
 * Ví dụ: bị outbid, phiên kết thúc, thanh toán yêu cầu, ...
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** false = chưa đọc, true = đã đọc */
    @Column(name = "is_read")
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Notification() {}

    public Notification(Long userId, String title, String content) {
        this.userId  = userId;
        this.title   = title;
        this.content = content;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public Long getUserId()                      { return userId; }
    public void setUserId(Long userId)           { this.userId = userId; }

    public String getTitle()                     { return title; }
    public void setTitle(String title)           { this.title = title; }

    public String getContent()                   { return content; }
    public void setContent(String content)       { this.content = content; }

    public Boolean getIsRead()                   { return isRead; }
    public void setIsRead(Boolean isRead)        { this.isRead = isRead; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void setCreatedAt(LocalDateTime t)    { this.createdAt = t; }
}
