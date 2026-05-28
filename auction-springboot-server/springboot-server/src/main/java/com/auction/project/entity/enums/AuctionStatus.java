package com.auction.project.entity.enums;

public enum AuctionStatus {
    /** Chờ admin duyệt - chưa hiển thị cho người dùng */
    WAITING_APPROVAL,
    /** Đã duyệt, chờ đến giờ bắt đầu */
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
