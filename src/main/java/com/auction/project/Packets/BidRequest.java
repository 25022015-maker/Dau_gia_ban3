package com.auction.project.Packets;

/**
 * Packet gửi từ Client lên Server khi người dùng thực hiện đặt giá.
 *
 * <p>Server sẽ kiểm tra tính hợp lệ của request này trước khi xử lý:
 * <ul>
 *   <li>bidAmount phải lớn hơn giá hiện tại của phiên</li>
 *   <li>auctionId phải tồn tại và đang ở trạng thái RUNNING</li>
 *   <li>bidderId phải là người dùng đã đăng nhập</li>
 * </ul>
 */
public class BidRequest {

    /** Loại action — luôn là "BID" để Server phân biệt với các request khác */
    private String action;

    /** ID của phiên đấu giá muốn tham gia */
    private String auctionId;

    /** ID của người đặt giá (lấy từ session sau khi đăng nhập) */
    private String bidderId;

    /** Số tiền đặt giá — phải > giá hiện tại của phiên */
    private double bidAmount;

    // ── Constructor ───────────────────────────────────────────────────────────

    public BidRequest() {
        this.action = "BID";
    }

    public BidRequest(String auctionId, String bidderId, double bidAmount) {
        this.action = "BID";
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    @Override
    public String toString() {
        return "BidRequest{"
                + "auctionId='"
                + auctionId
                + '\''
                + ", bidderId='"
                + bidderId
                + '\''
                + ", bidAmount="
                + bidAmount
                + '}';
    }
}