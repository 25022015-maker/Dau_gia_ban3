package com.auction.project.dto;

public record UserProfileResponse(
    Long id,
    String username,
    String email,
    String role,
    Long balance,
    String status,
    String createdAt
) {}
