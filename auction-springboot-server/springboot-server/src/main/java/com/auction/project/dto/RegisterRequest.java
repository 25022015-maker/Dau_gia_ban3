package com.auction.project.dto;

import jakarta.validation.constraints.*;

// ============================================================
//  AUTH DTOs
// ============================================================

/** Đăng ký tài khoản */
public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Size(min = 6) String password,
    @Email @NotBlank String email,
    String role   // BIDDER | SELLER | ADMIN, mặc định BIDDER
) {
    public RegisterRequest {
        if (role == null || role.isBlank()) role = "BIDDER";
    }
}
