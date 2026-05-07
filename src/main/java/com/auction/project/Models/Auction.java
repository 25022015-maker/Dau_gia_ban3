package com.auction.project.Models;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model đại diện cho một phiên đấu giá.
 *
 * <p>Vòng đời trạng thái: OPEN → RUNNING → FINISHED → PAID / CANCELED
 *
 * <p><b>Thread-safety:</b> Các phương thức thay đổi trạng thái (placeBid) được
 * đồng bộ hóa bởi {@code AuctionManager} thông qua {@code ReentrantLock}.
 * Class này chỉ chứa data và logic kiểm tra, không tự lock.
 */
public class Auction implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Trạng thái phiên đấu giá ──────────────────────────────────────────────

    public enum Status {
        OPEN,      // Đã tạo, chưa bắt đầu
        RUNNING,   // Đang diễn ra, nhận bid
        FINISHED,  // Hết thời gian, đã có người thắng
        PAID,      // Người thắng đã thanh toán
        CANCELED   // Phiên bị hủy (không có bid nào / vấn đề kỹ thuật)
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private String auctionId;
    private String itemName;
    private String description;
    private String sellerId;

    /** Giá khởi điểm — không thay đổi sau khi tạo phiên */
    private double startingPrice;

    /** Giá hiện tại cao nhất — cập nhật mỗi khi có bid hợp lệ */
    private double currentPrice;

    /** Username của người đang dẫn đầu */
    private String leadingBidder;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Status status;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Auction() {}

    public Auction(
            String auctionId,
            String itemName,
            String description,
            String sellerId,
            double startingPrice,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.description = description;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice; // Giá hiện tại ban đầu = giá khởi điểm
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Status.OPEN;
    }

    // ── Business Logic ────────────────────────────────────────────────────────

    /**
     * Kiểm tra xem phiên có đang nhận bid không.
     *
     * @return true nếu trạng thái là RUNNING và chưa hết thời gian
     */
    public boolean isAcceptingBids() {
        return status == Status.RUNNING && LocalDateTime.now().isBefore(endTime);
    }

    /**
     * Đặt giá vào phiên. Phương thức này chỉ được gọi từ {@code AuctionManager}
     * sau khi đã acquire lock.
     *
     * @param bidderId  username người đặt giá
     * @param amount    số tiền đặt
     * @return true nếu bid hợp lệ và được chấp nhận
     */
    public boolean placeBid(String bidderId, double amount) {
        // Kiểm tra tính hợp lệ: giá mới phải cao hơn giá hiện tại
        if (!isAcceptingBids() || amount <= currentPrice) {
            return false;
        }
        // Cập nhật leader mới
        this.currentPrice = amount;
        this.leadingBidder = bidderId;
        return true;
    }

    /**
     * Kết thúc phiên đấu giá — chuyển sang FINISHED hoặc CANCELED.
     * Gọi khi timer hết hoặc Admin hủy phiên.
     */
    public void closeAuction() {
        if (leadingBidder != null) {
            this.status = Status.FINISHED;
        } else {
            this.status = Status.CANCELED; // Không có ai đặt giá
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getLeadingBidder() {
        return leadingBidder;
    }

    public void setLeadingBidder(String leadingBidder) {
        this.leadingBidder = leadingBidder;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}