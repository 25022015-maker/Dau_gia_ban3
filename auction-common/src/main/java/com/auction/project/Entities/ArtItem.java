package com.auction.project.Entities;

public class ArtItem extends Item {
    private String artist;
    private int year;

    public ArtItem() { super(); }

    public ArtItem(String artist, int year) {
        super();
        this.artist = artist;
        this.year = year;
    }

    public String getDetails() {
        return "Sản phẩm nghệ thuật: " + getName() + " bởi " + artist + " (" + year + ")";
    }

    public String getArtist() { return artist; }
    public int getYear() { return year; }
}
