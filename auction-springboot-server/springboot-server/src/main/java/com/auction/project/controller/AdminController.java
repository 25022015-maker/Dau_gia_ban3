package com.auction.project.controller;

import com.auction.project.dto.UserProfileResponse;
import com.auction.project.entity.enums.AuctionStatus;
import com.auction.project.repository.AuctionRepository;
import com.auction.project.service.AuctionService;
import com.auction.project.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller dành riêng cho Admin.
 * Tất cả endpoint đều yêu cầu role ADMIN (kiểm tra qua @PreAuthorize).
 *
 * GET    /api/admin/stats             → thống kê tổng quan
 * GET    /api/admin/users             → danh sách tất cả user
 * PUT    /api/admin/users/{id}/status → khóa / mở tài khoản
 * PUT    /api/admin/auctions/{id}/cancel → hủy phiên đấu giá
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService       userService;
    private final AuctionService    auctionService;
    private final AuctionRepository auctionRepo;

    public AdminController(UserService userService,
                           AuctionService auctionService,
                           AuctionRepository auctionRepo) {
        this.userService    = userService;
        this.auctionService = auctionService;
        this.auctionRepo    = auctionRepo;
    }

    /** Dashboard: thống kê tổng quan */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalUsers    = userService.getAllUsers().size();
        long totalAuctions = auctionRepo.count();
        long running       = auctionRepo.findByStatus(AuctionStatus.RUNNING).size();
        long finished      = auctionRepo.findByStatus(AuctionStatus.FINISHED).size();
        long pending       = auctionRepo.findByStatus(AuctionStatus.PENDING).size();

        return ResponseEntity.ok(Map.of(
                "totalUsers",    totalUsers,
                "totalAuctions", totalAuctions,
                "running",       running,
                "finished",      finished,
                "pending",       pending
        ));
    }

    /** Danh sách tất cả người dùng */
    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Khóa hoặc mở tài khoản.
     * Body: { "status": "BANNED" } hoặc { "status": "ACTIVE" }
     */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<UserProfileResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "ACTIVE");
        return ResponseEntity.ok(userService.updateStatus(id, status));
    }

    /** Admin hủy phiên đấu giá bất kỳ */
    @PutMapping("/auctions/{id}/cancel")
    public ResponseEntity<String> cancelAuction(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        auctionService.cancelAuction(id, principal.getUsername());
        return ResponseEntity.ok("Phiên đấu giá #" + id + " đã bị hủy bởi Admin.");
    }
}
