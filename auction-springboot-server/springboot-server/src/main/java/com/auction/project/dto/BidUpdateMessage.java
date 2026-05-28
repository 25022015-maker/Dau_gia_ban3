package com.auction.project.dto;

public record BidUpdateMessage(
    Long   auctionId,
    Long   newPrice,
    String winnerUsername,
    Long   winnerId,
    String endTime,
    String status,
    String bidType,
    long   totalBids,
    String bidTime,
    boolean antiSnipeTriggered,
    String  antiSnipeUsername
) {}