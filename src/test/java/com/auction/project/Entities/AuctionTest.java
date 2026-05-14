package com.auction.project.Entities;


import com.auction.project.Entities.enums.AuctionStatus;
import com.auction.project.Exception.AuctionClosedException;
import com.auction.project.Exception.InvalidBidException;
import com.auction.project.Observer.Observer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionTest {

    private Auction auction;
    private Bidder bidder;
    private Product item;

    @BeforeEach
    void setUp() {
        item = new ElectronicsItem("X7",200,"freewolf"); // dùng constructor mặc định nếu có
        bidder = new Bidder("thao","thao@gmail.com"); // giống vậy

        auction = new Auction(
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(10),
                100.0,
                item
        );
    }

    // ✅ Test bid hợp lệ
    @Test
    void testValidBid() throws Exception {
        auction.placeBid(bidder, 150);
        assertEquals(150, auction.getCurrentPrice());
    }

    // ❌ Bid thấp hơn
    @Test
    void testInvalidBid() {
        assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(bidder, 50);
        });
    }

    // ❌ Auction bị đóng (status)
    @Test
    void testClosedAuctionByStatus() {
        auction.setStatus(AuctionStatus.FINISHED);

        assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(bidder, 200);
        });
    }

    // ❌ Auction hết hạn theo thời gian
    @Test
    void testClosedAuctionByTime() {
        Auction expiredAuction = new Auction(
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(1),
                100,
                item
        );

        assertThrows(AuctionClosedException.class, () -> {
            expiredAuction.placeBid(bidder, 200);
        });
    }

    // 🔥 Test observer (không đụng internal list)
    @Test
    void testObserverNotified() throws Exception {
        TestObserver observer = new TestObserver();
        auction.registerObserver(observer);

        auction.placeBid(bidder, 200);

        assertTrue(observer.called);
    }

    // Mock observer đúng interface
    static class TestObserver implements Observer {
        boolean called = false;

        @Override
        public void update(int auctionId, double price, String name) {
            called = true;
        }
    }
}