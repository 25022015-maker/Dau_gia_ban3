package com.auction.project.Entities;

public class ArtItem extends Item {
    private String artist;
    private int year;

    public ArtItem(String name, double price, String artist, int year) {
        super(name, price);
        this.artist = artist;
        this.year = year;
    }

    @Override
    public String getDetails() {
        return "Sản phẩm nghệ thuật: " + name + " bởi " + artist + " (" + year + ")";
    }
}