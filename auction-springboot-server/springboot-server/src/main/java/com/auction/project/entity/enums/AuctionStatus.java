package com.auction.project.entity.enums;

public enum AuctionStatus {
    /** Chờ đến giờ bắt đầu */
    PENDING,
    /** Đang diễn ra - nhận bid */
    RUNNING,
    /** Kết thúc, có người thắng */
    FINISHED,
    /** Người thắng đã thanh toán */
    PAID,
    /** Hủy - không ai đặt giá hoặc admin hủy */
    CANCELED
}
