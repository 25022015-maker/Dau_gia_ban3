package com.auction.project.exception;

/** Giá đặt không hợp lệ (thấp hơn currentPrice, không đủ bước giá, ...) */
public class InvalidBidException extends RuntimeException {
    public InvalidBidException(String message) { super(message); }
}
