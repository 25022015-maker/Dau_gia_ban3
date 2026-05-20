package com.auction.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point của Spring Boot Server - thay thế ServerApp.java cũ.
 *
 * @EnableScheduling: bật @Scheduled để AuctionScheduler tự động
 *   mở/đóng phiên đấu giá theo thời gian thực.
 */
@SpringBootApplication
@EnableScheduling
public class AuctionServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionServerApplication.class, args);
    }
}
