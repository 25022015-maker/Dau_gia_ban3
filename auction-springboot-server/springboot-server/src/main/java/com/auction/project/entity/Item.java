package com.auction.project.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Abstract entity Item - lớp cha cho mọi loại sản phẩm đấu giá.
 *
 * Áp dụng OOP:
 *   - Inheritance (Kế thừa): ElectronicsItem, ArtItem, VehicleItem kế thừa Item
 *   - Abstraction (Trừu tượng): getDetails() là abstract, mỗi con override
 *   - Polymorphism: cùng gọi getDetails() nhưng kết quả khác nhau theo loại
 *
 * Database strategy: SINGLE_TABLE - tất cả loại item dùng chung 1 bảng,
 * phân biệt nhau qua cột item_type (discriminator).
 */
@Entity
@Table(name = "items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID người bán tạo ra item này */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Ảnh sản phẩm lưu dạng Base64 - giữ nguyên như thiết kế cũ của nhóm */
    @Column(name = "image_base64", columnDefinition = "LONGTEXT")
    private String imageBase64;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Constructor ───────────────────────────────────────────────────────────

    protected Item() {}

    protected Item(Long ownerId, String name, String description) {
        this.ownerId     = ownerId;
        this.name        = name;
        this.description = description;
    }

    /**
     * Polymorphism: mỗi subclass override để trả về thông tin đặc thù của loại đó.
     * Tương đương getDetails() trong thiết kế cũ của nhóm.
     */
    public abstract String getDetails();

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public Long getOwnerId()                    { return ownerId; }
    public void setOwnerId(Long ownerId)        { this.ownerId = ownerId; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getDescription()              { return description; }
    public void setDescription(String desc)     { this.description = desc; }

    public String getImageBase64()              { return imageBase64; }
    public void setImageBase64(String img)      { this.imageBase64 = img; }

    public LocalDateTime getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }
}
