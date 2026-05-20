package com.auction.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Tác phẩm nghệ thuật - kế thừa Item.
 * Ví dụ: tranh, tượng, đồ cổ, ...
 */
@Entity
@DiscriminatorValue("ART")
public class ArtItem extends Item {

    @Column(name = "artist", length = 100)
    private String artist;

    @Column(name = "year_created")
    private Integer yearCreated;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ArtItem() {}

    public ArtItem(Long ownerId, String name, String description,
                   String artist, Integer yearCreated) {
        super(ownerId, name, description);
        this.artist      = artist;
        this.yearCreated = yearCreated;
    }

    // ── Polymorphism ──────────────────────────────────────────────────────────

    @Override
    public String getDetails() {
        return "Sản phẩm nghệ thuật: " + getName()
                + " bởi " + artist
                + (yearCreated != null ? " (" + yearCreated + ")" : "");
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getArtist()                    { return artist; }
    public void setArtist(String artist)         { this.artist = artist; }

    public Integer getYearCreated()              { return yearCreated; }
    public void setYearCreated(Integer y)        { this.yearCreated = y; }
}
