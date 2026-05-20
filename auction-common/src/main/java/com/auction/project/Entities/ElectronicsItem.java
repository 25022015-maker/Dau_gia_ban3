package com.auction.project.Entities;

public class ElectronicsItem extends Item {
    private String brand;

    public ElectronicsItem() { super(); }

    public ElectronicsItem(String brand) {
        super();
        this.brand = brand;
    }

    public String getDetails() { return "Đồ điện tử " + brand + " " + getName(); }
    public String getBrand() { return brand; }
}
