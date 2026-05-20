package com.auction.project.Entities;

public class VehicleItem extends Item {
    private String make;

    public VehicleItem() { super(); }

    public VehicleItem(String make) {
        super();
        this.make = make;
    }

    public String getDetails() { return "Phương tiện giao thông: " + make + " " + getName(); }
    public String getMake() { return make; }
}
