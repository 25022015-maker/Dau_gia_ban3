package com.auction.project.Packets;

/**
 * Packet Client → Server khi đặt giá.
 *
 * Lưu ý: auctionId là String để tương thích với JSON.
 * ServerController parse sang int khi cần (vì Entity.id là int).
 */
public class BidRequest {

    private String action;
    private String auctionId; // String trong JSON, parse sang int trong ServerController
    private String bidderId;  // username của người đặt giá
    private double bidAmount;

    public BidRequest() {
        this.action = "BID";
    }

    public BidRequest(String auctionId, String bidderId, double bidAmount) {
        this.action    = "BID";
        this.auctionId = auctionId;
        this.bidderId  = bidderId;
        this.bidAmount = bidAmount;
    }

    public String getAction()    { return action; }
    public String getAuctionId() { return auctionId; }
    public String getBidderId()  { return bidderId; }
    public double getBidAmount() { return bidAmount; }

    public void setAction(String action)       { this.action = action; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public void setBidderId(String bidderId)   { this.bidderId = bidderId; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }
}