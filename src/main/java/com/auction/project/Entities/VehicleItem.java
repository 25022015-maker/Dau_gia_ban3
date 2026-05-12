package com.auction.project.Entities;

public class VehicleItem extends Item {
    private String make;
    public VehicleItem(String name, double price, String make) {
        super(name, price);
        this.make = make;
    }
    @Override public String getDetails() { return "Phương tiện giao thông: " + make + " " + name; }
}
