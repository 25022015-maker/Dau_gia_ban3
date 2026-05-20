package com.auction.project.controller;

import com.auction.project.dto.AuthResponse;
import com.auction.project.dto.LoginRequest;
import com.auction.project.dto.RegisterRequest;
import com.auction.project.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller xử lý đăng ký / đăng nhập.
 * Endpoints này PUBLIC - không cần JWT token.
 *
 * POST /api/auth/register  →  tạo tài khoản mới
 * POST /api/auth/login     →  đăng nhập, nhận JWT token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Đăng ký tài khoản.
     * Body: { username, password, email, role }
     * Response: { token, userId, username, role }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    /**
     * Đăng nhập.
     * Body: { username, password }
     * Response: { token, userId, username, role }
     * Client lưu token vào SessionManager rồi đính kèm vào mọi request sau.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
