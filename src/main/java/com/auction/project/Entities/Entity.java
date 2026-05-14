package com.auction.project.Entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int idCounter = 1000;

    protected int id;
    protected LocalDateTime createdAt;

    public Entity() {
        this.id = idCounter++;
        this.createdAt = LocalDateTime.now();
    }

    public int getId() { return id; }
}