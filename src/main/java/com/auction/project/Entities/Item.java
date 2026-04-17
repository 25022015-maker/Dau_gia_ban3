package com.auction.project.Entities;

public abstract class Item {
    protected String id;
    protected String name;
    protected double startingPrice;

    public Item(String id, String name, double startingPrice) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
    }

    public abstract String getItemType();

    public double getStartingPrice() { return startingPrice; }
    public String getName() { return name; }
}