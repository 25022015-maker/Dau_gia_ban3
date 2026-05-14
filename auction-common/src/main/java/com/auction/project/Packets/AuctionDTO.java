package com.auction.project.Packets;

import com.auction.project.Entities.Auction;
import com.auction.project.Entities.enums.AuctionStatus;

/**
 * Data Transfer Object cho Auction — chỉ chứa các field cần thiết để gửi về client.
 * Tránh vấn đề Gson không serialize được LocalDateTime trong module system.
 */
public class AuctionDTO {
    private int id;
    private String itemName;
    private double currentPrice;
    private String leadingBidder;
    private String status;
    private String startTime;
    private String endTime;

    public AuctionDTO() {}

    /** Tạo DTO từ Auction entity */
    public static AuctionDTO from(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        dto.id            = auction.getId();
        dto.itemName      = auction.getItemName();
        dto.currentPrice  = auction.getCurrentPrice();
        dto.leadingBidder = auction.getLeadingBidder();
        dto.status        = auction.getStatus() != null ? auction.getStatus().name() : "OPEN";
        dto.startTime     = auction.getStartTime() != null ? auction.getStartTime().toString() : "";
        dto.endTime       = auction.getEndTime()   != null ? auction.getEndTime().toString()   : "";
        return dto;
    }

    // Getters
    public int    getId()             { return id; }
    public String getItemName()       { return itemName; }
    public double getCurrentPrice()   { return currentPrice; }
    public String getLeadingBidder()  { return leadingBidder; }
    public String getStatus()         { return status; }
    public String getStartTime()      { return startTime; }
    public String getEndTime()        { return endTime; }
}