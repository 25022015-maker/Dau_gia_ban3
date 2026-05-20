package com.auction.project.exception;

/** Phiên đấu giá không ở trạng thái RUNNING khi đặt giá */
public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String message) { super(message); }
}
