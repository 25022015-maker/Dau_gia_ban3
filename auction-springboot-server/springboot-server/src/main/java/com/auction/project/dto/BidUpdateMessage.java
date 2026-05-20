package com.auction.project.dto;

/**
 * Message broadcast qua WebSocket STOMP khi có bid mới.
 * Client subscribe /topic/auction/{id} sẽ nhận message này.
 *
 * Thay thế hoàn toàn cơ chế BID_UPDATE JSON của SocketServer cũ.
 */
public record BidUpdateMessage(
    Long auctionId,
    Long newPrice,
    String winnerUsername,
    Long winnerId,
    String endTime,
    String status,        // RUNNING | FINISHED | CANCELED
    String bidType,       // MANUAL | AUTO | SYSTEM
    long totalBids,
    String bidTime        // thời điểm bid xảy ra (ISO-8601)
) {}
