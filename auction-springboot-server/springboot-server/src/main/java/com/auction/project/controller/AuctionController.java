package com.auction.project.controller;

import com.auction.project.dto.*;
import com.auction.project.service.AuctionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller xử lý toàn bộ nghiệp vụ đấu giá.
 *
 * Endpoint map:
 *   GET  /api/auctions              → danh sách tất cả phiên (PUBLIC)
 *   GET  /api/auctions/{id}         → chi tiết 1 phiên (PUBLIC)
 *   POST /api/auctions              → tạo phiên mới (SELLER / ADMIN)
 *   POST /api/auctions/{id}/bid     → đặt giá thủ công (đăng nhập)
 *   POST /api/auctions/{id}/auto-bid → đăng ký auto-bid (đăng nhập)
 *   GET  /api/auctions/{id}/bids    → lịch sử bid (PUBLIC)
 *   DELETE /api/auctions/{id}       → hủy phiên (SELLER / ADMIN)
 */
@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    // ── GET all ──────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> getAll() {
        return ResponseEntity.ok(auctionService.getAll());
    }

    // ── GET by id ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getById(id));
    }

    // ── CREATE (Seller / Admin) ───────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<AuctionResponse> create(
            @Valid @RequestBody CreateAuctionRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(auctionService.createAuction(req, principal.getUsername()));
    }

    // ── PLACE BID ─────────────────────────────────────────────────────────────

    /**
     * Đặt giá thủ công.
     * Body: { auctionId, amount }
     *
     * Server xử lý:
     *   1. Pessimistic Lock trên auction row
     *   2. Validate giá, trạng thái phiên
     *   3. Anti-sniping
     *   4. Lưu DB → Broadcast WebSocket → Trigger auto-bids
     */
    @PostMapping("/{id}/bid")
    public ResponseEntity<AuctionResponse> placeBid(
            @PathVariable Long id,
            @Valid @RequestBody PlaceBidRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        // Ưu tiên id từ path variable
        PlaceBidRequest finalReq = new PlaceBidRequest(id, req.amount());
        return ResponseEntity.ok(auctionService.placeBid(finalReq, principal.getUsername()));
    }

    // ── AUTO-BID ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/auto-bid")
    public ResponseEntity<String> registerAutoBid(
            @PathVariable Long id,
            @Valid @RequestBody AutoBidRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        AutoBidRequest finalReq = new AutoBidRequest(id, req.maxBid(), req.increment());
        auctionService.registerAutoBid(finalReq, principal.getUsername());
        return ResponseEntity.ok("Đã đăng ký auto-bid thành công");
    }

    // ── BID HISTORY ───────────────────────────────────────────────────────────

    @GetMapping("/{id}/bids")
    public ResponseEntity<List<BidTransactionResponse>> getBidHistory(@PathVariable Long id) {
        return ResponseEntity.ok(auctionService.getBidHistory(id));
    }

    // ── DELETE (Seller / Admin) ───────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<String> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        auctionService.cancelAuction(id, principal.getUsername());
        return ResponseEntity.ok("Phiên đấu giá #" + id + " đã bị hủy.");
    }
}
