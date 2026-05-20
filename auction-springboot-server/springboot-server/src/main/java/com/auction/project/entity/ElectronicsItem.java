package com.auction.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Sản phẩm điện tử - kế thừa Item.
 * Ví dụ: iPhone, Laptop, TV, ...
 *
 * Tương tự ElectronicsItem trong auction-common của nhóm nhưng được
 * bổ sung JPA annotations cho Spring Boot.
 */
@Entity
@DiscriminatorValue("ELECTRONICS")
public class ElectronicsItem extends Item {

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ElectronicsItem() {}

    public ElectronicsItem(Long ownerId, String name, String description,
                           String brand, Integer warrantyMonths) {
        super(ownerId, name, description);
        this.brand          = brand;
        this.warrantyMonths = warrantyMonths;
    }

    // ── Polymorphism ──────────────────────────────────────────────────────────

    @Override
    public String getDetails() {
        return "Đồ điện tử " + brand + " - " + getName()
                + (warrantyMonths != null ? " | Bảo hành: " + warrantyMonths + " tháng" : "");
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getBrand()                          { return brand; }
    public void setBrand(String brand)                { this.brand = brand; }

    public Integer getWarrantyMonths()                { return warrantyMonths; }
    public void setWarrantyMonths(Integer wm)         { this.warrantyMonths = wm; }
}
