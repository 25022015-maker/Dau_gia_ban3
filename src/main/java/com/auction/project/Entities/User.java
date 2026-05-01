package com.auction.project.Entities;

public abstract class User extends Entity {
    protected String username;
    protected String passwordHash;
    protected String email;

    public User(String username, String email) {
        super();
        this.username = username;
        this.email = email;
    }

    public boolean login() { return true; }
    public void logout() { }
}
