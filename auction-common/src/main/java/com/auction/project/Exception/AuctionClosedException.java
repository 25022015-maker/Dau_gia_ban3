package com.auction.project.Exception;

public class AuctionClosedException extends Exception {

    public AuctionClosedException(String message) {
        super(message);
    }
    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}