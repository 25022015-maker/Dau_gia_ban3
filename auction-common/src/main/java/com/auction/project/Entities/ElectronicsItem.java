package com.auction.project.Entities;

public class ElectronicsItem extends Item {
    private String brand;
    public ElectronicsItem(String name, double price, String brand) {
        super(name, price);
        this.brand = brand;
    }
    @Override public String getDetails() { return " Đồ điện tử " + brand + " " + name; }
}