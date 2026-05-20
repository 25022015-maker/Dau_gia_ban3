package com.auction.project.controller;

import com.auction.project.dto.UserProfileResponse;
import com.auction.project.entity.Notification;
import com.auction.project.service.AuctionService;
import com.auction.project.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cho người dùng đã đăng nhập.
 *
 * GET  /api/users/me                → xem profile của mình
 * POST /api/users/me/top-up         → nạp tiền
 * GET  /api/users/me/notifications  → danh sách thông báo
 * PUT  /api/users/me/notifications/read → đánh dấu đã đọc tất cả
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService    userService;
    private final AuctionService auctionService;

    public UserController(UserService userService, AuctionService auctionService) {
        this.userService    = userService;
        this.auctionService = auctionService;
    }

    /** Lấy thông tin profile của người dùng đang đăng nhập */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getUsername()));
    }

    /**
     * Nạp tiền vào tài khoản.
     * Body: { "amount": 1000000 }
     */
    @PostMapping("/me/top-up")
    public ResponseEntity<UserProfileResponse> topUp(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody Map<String, Long> body) {
        Long amount = body.get("amount");
        if (amount == null || amount <= 0) {
            throw new RuntimeException("Số tiền nạp không hợp lệ");
        }
        return ResponseEntity.ok(userService.topUp(principal.getUsername(), amount));
    }

    /** Lấy danh sách thông báo (mới nhất lên đầu) */
    @GetMapping("/me/notifications")
    public ResponseEntity<List<Notification>> getNotifications(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(auctionService.getNotifications(principal.getUsername()));
    }

    /** Đánh dấu tất cả thông báo là đã đọc */
    @PutMapping("/me/notifications/read")
    public ResponseEntity<String> markRead(
            @AuthenticationPrincipal UserDetails principal) {
        auctionService.markNotificationsRead(principal.getUsername());
        return ResponseEntity.ok("Đã đánh dấu tất cả thông báo là đã đọc");
    }
}
