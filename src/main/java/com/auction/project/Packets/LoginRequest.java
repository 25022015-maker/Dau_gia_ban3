package com.auction.project.Packets;

import java.io.Serializable;

public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    // Constructor mặc định (cần cho deserialization)
    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username cannot be empty");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password cannot be empty");
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    @Override
    public String toString() {
        // FIX: không in password ra log — tránh lộ thông tin
        return "LoginRequest{username='" + username + "', password='***'}";
    }
}