package com.auction.project.Entities;

import java.io.Serializable;
import java.time.LocalDateTime;

abstract class AbstractEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int idCounter = 1000;

    protected int id;
    protected LocalDateTime createdAt;

    public AbstractEntity() {
        this.id = idCounter++;
        this.createdAt = LocalDateTime.now();
    }
    public void setId(int id) {
        this.id = id;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public int getId() { return id; }
}