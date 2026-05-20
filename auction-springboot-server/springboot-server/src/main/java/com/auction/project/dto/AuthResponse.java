package com.auction.project.dto;

public record AuthResponse(
    String token,
    Long userId,
    String username,
    String role
) {}
