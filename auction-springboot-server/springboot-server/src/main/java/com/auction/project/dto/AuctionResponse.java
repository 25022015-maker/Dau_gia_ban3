package com.auction.project.dto;

/** Dữ liệu phiên đấu giá trả về cho client */
public record AuctionResponse(
    Long id,
    String itemName,
    String itemType,
    String itemDetails,
    String description,
    String imageBase64,
    Long startPrice,
    Long minBid,
    Long currentPrice,
    String currentWinnerUsername,
    Long currentWinnerId,
    String startTime,
    String endTime,
    String status,
    Long sellerId,
    String sellerUsername,
    long totalBids
) {}
