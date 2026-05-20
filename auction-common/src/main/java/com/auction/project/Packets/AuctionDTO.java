package com.auction.project.Packets;

import com.auction.project.Entities.Auction;

/**
 * Data Transfer Object cho Auction — chỉ chứa các field cần thiết để gửi về client.
 */
public class AuctionDTO {
    private long   id;
    private long   currentPrice;
    private String status;
    private String startTime;
    private String endTime;

    public AuctionDTO() {}

    public static AuctionDTO from(Auction auction) {
        AuctionDTO dto = new AuctionDTO();
        dto.id           = auction.getId() != null ? auction.getId() : 0L;
        dto.currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : 0L;
        dto.status       = auction.getStatus() != null ? auction.getStatus() : "PENDING";
        dto.startTime    = auction.getStartTime() != null ? auction.getStartTime().toString() : "";
        dto.endTime      = auction.getEndTime()   != null ? auction.getEndTime().toString()   : "";
        return dto;
    }

    public long   getId()           { return id; }
    public long   getCurrentPrice() { return currentPrice; }
    public String getStatus()       { return status; }
    public String getStartTime()    { return startTime; }
    public String getEndTime()      { return endTime; }
}
