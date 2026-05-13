package com.auction.project.Entities;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startPrice;

    public Item(String name, double startPrice) {
        super();
        this.name = name;
        this.startPrice = startPrice;
    }
    public abstract String getDetails();
    public String getName() { return name; }
}