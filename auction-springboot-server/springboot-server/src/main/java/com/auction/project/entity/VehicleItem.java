package com.auction.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Phương tiện giao thông - kế thừa Item.
 * Ví dụ: ô tô, xe máy, tàu thuyền, ...
 */
@Entity
@DiscriminatorValue("VEHICLE")
public class VehicleItem extends Item {

    @Column(name = "vehicle_brand", length = 100)
    private String vehicleBrand;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "mileage_km")
    private Integer mileageKm;

    // ── Constructor ───────────────────────────────────────────────────────────

    public VehicleItem() {}

    public VehicleItem(Long ownerId, String name, String description,
                       String vehicleBrand, Integer manufactureYear, Integer mileageKm) {
        super(ownerId, name, description);
        this.vehicleBrand    = vehicleBrand;
        this.manufactureYear = manufactureYear;
        this.mileageKm       = mileageKm;
    }

    // ── Polymorphism ──────────────────────────────────────────────────────────

    @Override
    public String getDetails() {
        return "Phương tiện giao thông: " + vehicleBrand + " " + getName()
                + (manufactureYear != null ? " | Năm SX: " + manufactureYear : "")
                + (mileageKm       != null ? " | Số km: "  + mileageKm + " km" : "");
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getVehicleBrand()                    { return vehicleBrand; }
    public void setVehicleBrand(String vehicleBrand)   { this.vehicleBrand = vehicleBrand; }

    public Integer getManufactureYear()                { return manufactureYear; }
    public void setManufactureYear(Integer y)          { this.manufactureYear = y; }

    public Integer getMileageKm()                      { return mileageKm; }
    public void setMileageKm(Integer km)               { this.mileageKm = km; }
}
