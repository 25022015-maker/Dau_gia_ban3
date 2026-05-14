package com.auction.project.Entities;

public class User extends Entity {
    protected String username;
    protected String passwordHash;

    public User(String username, String passwordHash) {
        super();
        this.username = username;
        this.passwordHash = passwordHash;
    }
    public String getUsername() {
        return this.username;
    }
    public boolean login() { return true; }
    public void logout() { }
}
