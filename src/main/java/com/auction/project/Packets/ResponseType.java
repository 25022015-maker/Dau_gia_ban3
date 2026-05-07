package com.auction.project.Packets;

/**
 * Enum định nghĩa tất cả các loại phản hồi từ Server đến Client.
 * Mỗi type tương ứng với một hành động cụ thể trong hệ thống đấu giá.
 */
public enum ResponseType {

    // ── Authentication ────────────────────────────────────────────────────────
    /** Đăng nhập thành công */
    LOGIN_SUCCESS,
    /** Đăng nhập thất bại (sai tài khoản/mật khẩu) */
    LOGIN_FAILURE,

    // ── Auction List ──────────────────────────────────────────────────────────
    /** Server trả về danh sách phiên đấu giá */
    AUCTION_LIST,

    // ── Bid Results ───────────────────────────────────────────────────────────
    /** Đặt giá thành công */
    BID_SUCCESS,
    /** Đặt giá thất bại (giá thấp hơn hiện tại, phiên đã đóng, v.v.) */
    BID_FAILURE,

    // ── Broadcast (Server → All Clients) ─────────────────────────────────────
    /**
     * Server broadcast giá mới tới TẤT CẢ client đang kết nối.
     * Đây là trái tim của Observer Pattern trong hệ thống.
     */
    BID_UPDATE,

    // ── Session State ─────────────────────────────────────────────────────────
    /** Phiên đấu giá đã kết thúc, kèm thông tin người thắng */
    AUCTION_ENDED,

    // ── Error ─────────────────────────────────────────────────────────────────
    /** Lỗi chung từ phía server */
    ERROR
}