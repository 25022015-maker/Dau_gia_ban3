package com.auction.project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler tự động kiểm tra và cập nhật trạng thái phiên đấu giá.
 * Thay thế logic check-trạng-thái thủ công trong AuctionManager cũ.
 *
 * Chạy mỗi 10 giây:
 *   - Mở phiên PENDING đã đến giờ bắt đầu → RUNNING
 *   - Đóng phiên RUNNING hết giờ → FINISHED hoặc CANCELED
 */
@Component
public class AuctionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

    private final AuctionService auctionService;

    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /** Mở phiên đến giờ bắt đầu */
    @Scheduled(fixedDelay = 10_000)
    public void startPending() {
        try {
            auctionService.startPendingAuctions();
        } catch (Exception e) {
            log.error("Lỗi khi mở phiên: {}", e.getMessage());
        }
    }

    /** Đóng phiên hết giờ */
    @Scheduled(fixedDelay = 10_000)
    public void closeExpired() {
        try {
            auctionService.closeExpiredAuctions();
        } catch (Exception e) {
            log.error("Lỗi khi đóng phiên: {}", e.getMessage());
        }
    }
}
