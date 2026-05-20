package com.auction.project.dto;

public record BidTransactionResponse(
    Long id,
    Long auctionId,
    Long bidderId,
    String bidderUsername,
    Long amount,
    String bidTime,
    String bidType
) {}
