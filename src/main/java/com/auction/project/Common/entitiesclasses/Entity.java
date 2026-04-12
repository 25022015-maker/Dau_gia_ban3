package com.auction.project.Common.entitiesclasses;
import java.time.LocalDateTime;

public abstract class Entity {
    //attributes
    private static int nextId = 1;
    private final int id;
    private final LocalDateTime createdAt;
    //constructor
    public Entity(){
        this.id = nextId++;
        this.createdAt = LocalDateTime.now();
    }

    public int getId(){return id;}
    public LocalDateTime getTime(){return createdAt;}
    public abstract <T> T getInfo();
}
