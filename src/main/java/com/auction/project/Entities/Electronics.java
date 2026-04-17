package com.auction.project.Entities;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, double startingPrice, int warrantyMonths) {
        super(id, name, startingPrice);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getItemType() {
        return "Electronics";
    }
}
