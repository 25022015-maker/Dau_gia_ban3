package com.auction.project.service;

import com.auction.project.dto.AutoBidRequest;
import com.auction.project.dto.PlaceBidRequest;
import com.auction.project.entity.*;
import com.auction.project.entity.enums.AuctionStatus;
import com.auction.project.entity.enums.Role;
import com.auction.project.exception.AuctionClosedException;
import com.auction.project.exception.InvalidBidException;
import com.auction.project.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho AuctionService.
 * Dùng Mockito mock toàn bộ repository và WebSocket template.
 * Chạy nhanh, không cần kết nối DB hay Spring context.
 *
 * Test cases:
 *   1. Bid hợp lệ → cập nhật giá và broadcast
 *   2. Bid thấp hơn currentPrice → InvalidBidException
 *   3. Bid không đủ bước giá tối thiểu → InvalidBidException
 *   4. Bid khi phiên FINISHED → AuctionClosedException
 *   5. Bid khi phiên CANCELED → AuctionClosedException
 *   6. Seller tự bid sản phẩm của mình → InvalidBidException
 *   7. Người đang dẫn đầu bid lại → InvalidBidException
 *   8. Anti-sniping gia hạn endTime khi bid gần cuối
 *   9. Phiên hết giờ có winner → FINISHED
 *  10. Phiên hết giờ không có winner → CANCELED
 *  11. Auto-bid được kích hoạt sau bid thủ công
 *  12. Auto-bid không kích hoạt khi đã đạt maxBid
 *  13. Đăng ký auto-bid khi phiên không RUNNING → AuctionClosedException
 *  14. Bid hợp lệ → BidTransaction được lưu DB
 */
@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    // ── Mocks ─────────────────────────────────────────────────────────────────
    @Mock private AuctionRepository        auctionRepo;
    @Mock private BidTransactionRepository txRepo;
    @Mock private AutoBidRepository        autoBidRepo;
    @Mock private UserRepository           userRepo;
    @Mock private ItemRepository           itemRepo;
    @Mock private NotificationRepository   notifRepo;
    @Mock private SimpMessagingTemplate    ws;

    @InjectMocks
    private AuctionService service;

    // ── Test fixtures ─────────────────────────────────────────────────────────
    private Auction auction;
    private User    seller;
    private User    bidder;
    private User    bidder2;

    @BeforeEach
    void setUp() {
        // Inject @Value fields thủ công (không có Spring context)
        ReflectionTestUtils.setField(service, "antiSnipeTriggerSeconds", 60);

        seller = makeUser(1L, "seller01", Role.SELLER);
        bidder = makeUser(2L, "bidder01", Role.BIDDER);
        bidder2 = makeUser(3L, "bidder02", Role.BIDDER);

        ElectronicsItem item = new ElectronicsItem(1L, "iPhone 15", "Điện thoại", "Apple", 12);
        item.setId(10L);

        auction = new Auction();
        auction.setId(100L);
        auction.setItem(item);
        auction.setStartPrice(5_000_000L);
        auction.setCurrentPrice(5_000_000L);
        auction.setMinBid(500_000L);
        auction.setStartTime(LocalDateTime.now().minusHours(1));
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setSellerId(seller.getId());
        auction.setAntiSnipeExtSeconds(60);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 1: Bid hợp lệ
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bid hợp lệ → cập nhật currentPrice, lưu DB, broadcast WebSocket")
    void placeBid_valid_shouldUpdateAndBroadcast() {
        // Arrange
        long bidAmount = 6_000_000L;
        stubBid(bidAmount);

        // Act
        service.placeBid(new PlaceBidRequest(100L, bidAmount), "bidder01");

        // Assert
        assertEquals(bidAmount, auction.getCurrentPrice());
        assertEquals(bidder, auction.getCurrentWinner());
        verify(txRepo, times(1)).save(any(BidTransaction.class));
        verify(ws, atLeastOnce()).convertAndSend(anyString(), any(Object.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 2: Bid thấp hơn currentPrice
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bid thấp hơn currentPrice → InvalidBidException")
    void placeBid_lowerThanCurrent_throwsInvalidBid() {
        when(auctionRepo.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(userRepo.findByUsername("bidder01")).thenReturn(Optional.of(bidder));

        assertThrows(InvalidBidException.class,
                () -> service.placeBid(new PlaceBidRequest(100L, 4_000_000L), "bidder01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 3: Bid không đủ bước giá tối thiểu
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bid chỉ hơn 100k (minBid=500k) → InvalidBidException")
    void placeBid_belowMinBid_throwsInvalidBid() {
        when(auctionRepo.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(userRepo.findByUsername("bidder01")).thenReturn(Optional.of(bidder));

        // currentPrice = 5_000_000, minBid = 500_000 → cần >= 5_500_000
        assertThrows(InvalidBidException.class,
                () -> service.placeBid(new PlaceBidRequest(100L, 5_100_000L), "bidder01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 4: Bid khi phiên FINISHED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phiên FINISHED → AuctionClosedException")
    void placeBid_finished_throwsClosed() {
        auction.setStatus(AuctionStatus.FINISHED);
        when(auctionRepo.findByIdWithLock(100L)).thenReturn(Optional.of(auction));

        assertThrows(AuctionClosedException.class,
                () -> service.placeBid(new PlaceBidRequest(100L, 6_000_000L), "bidder01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 5: Bid khi phiên CANCELED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phiên CANCELED → AuctionClosedException")
    void placeBid_canceled_throwsClosed() {
        auction.setStatus(AuctionStatus.CANCELED);
        when(auctionRepo.findByIdWithLock(100L)).thenReturn(Optional.of(auction));

        assertThrows(AuctionClosedException.class,
                () -> service.placeBid(new PlaceBidRequest(100L, 6_000_000L), "bidder01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 6: Seller tự bid sản phẩm của mình
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Seller tự bid → InvalidBidException")
    void placeBid_sellerBidsOwnAuction_throwsInvalidBid() {
        when(auctionRepo.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(userRepo.findByUsername("seller01")).thenReturn(Optional.of(seller));

        assertThrows(InvalidBidException.class,
                () -> service.placeBid(new PlaceBidRequest(100L, 6_000_000L), "seller01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 7: Người đang dẫn đầu bid lại
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Người đang dẫn đầu bid lại → InvalidBidException")
    void placeBid_currentWinnerBidsAgain_throwsInvalidBid() {
        auction.setCurrentWinner(bidder); // bidder01 đang dẫn đầu

        when(auctionRepo.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(userRepo.findByUsername("bidder01")).thenReturn(Optional.of(bidder));

        assertThrows(InvalidBidException.class,
                () -> service.placeBid(new PlaceBidRequest(100L, 6_000_000L), "bidder01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 8: Anti-sniping gia hạn endTime
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bid trong 60 giây cuối → endTime bị gia hạn thêm 60 giây")
    void placeBid_antiSnipe_extendsEndTime() {
        LocalDateTime nearEnd = LocalDateTime.now().plusSeconds(30); // còn 30s
        auction.setEndTime(nearEnd);
        stubBid(6_000_000L);

        service.placeBid(new PlaceBidRequest(100L, 6_000_000L), "bidder01");

        assertTrue(auction.getEndTime().isAfter(nearEnd),
                "endTime phải được gia hạn sau anti-sniping");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 9: Đóng phiên có winner → FINISHED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phiên hết giờ có winner → status FINISHED, gửi notification")
    void closeExpiredAuctions_withWinner_setsFinished() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));
        auction.setCurrentWinner(bidder);

        when(auctionRepo.findExpiredAuctions(any())).thenReturn(List.of(auction));
        when(auctionRepo.save(any())).thenReturn(auction);
        when(txRepo.countByAuctionId(any())).thenReturn(3L);

        service.closeExpiredAuctions();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        verify(notifRepo, times(1)).save(any(Notification.class));
        verify(ws, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 10: Đóng phiên không có winner → CANCELED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Phiên hết giờ không có winner → status CANCELED")
    void closeExpiredAuctions_noWinner_setsCanceled() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(1));
        auction.setCurrentWinner(null);

        when(auctionRepo.findExpiredAuctions(any())).thenReturn(List.of(auction));
        when(auctionRepo.save(any())).thenReturn(auction);
        when(txRepo.countByAuctionId(any())).thenReturn(0L);

        service.closeExpiredAuctions();

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        verify(notifRepo, never()).save(any()); // không gửi notification khi không có winner
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 11: Auto-bid được kích hoạt sau bid thủ công
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sau bid thủ công → auto-bid của bidder2 tự động kích hoạt")
    void triggerAutoBids_shouldFireAutoBid() {
        // Arrange: bidder2 có auto-bid maxBid=8_000_000, increment=500_000
        auction.setCurrentPrice(6_000_000L);
        auction.setCurrentWinner(bidder);

        AutoBid ab = new AutoBid(auction, bidder2, 8_000_000L, 500_000L);
        ab.setIsActive(true);

        when(autoBidRepo.findByAuctionIdAndIsActiveTrueOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(ab))
                .thenReturn(List.of()); // lần 2 đệ quy trả rỗng để dừng
        when(auctionRepo.save(any())).thenReturn(auction);
        when(txRepo.save(any())).thenReturn(new BidTransaction());
        when(txRepo.countByAuctionId(any())).thenReturn(2L);

        // Act
        service.triggerAutoBids(auction, bidder.getId());

        // Assert: giá tăng thêm increment
        assertEquals(6_500_000L, auction.getCurrentPrice());
        assertEquals(bidder2, auction.getCurrentWinner());
        verify(txRepo, times(1)).save(any(BidTransaction.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 12: Auto-bid đã đạt maxBid → vô hiệu hóa
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Auto-bid nextBid > maxBid → isActive=false, không bid")
    void triggerAutoBids_maxBidReached_deactivates() {
        auction.setCurrentPrice(7_800_000L);
        auction.setCurrentWinner(bidder);

        AutoBid ab = new AutoBid(auction, bidder2, 8_000_000L, 500_000L);
        // nextBid = 7_800_000 + 500_000 = 8_300_000 > maxBid=8_000_000 → deactivate
        ab.setIsActive(true);

        when(autoBidRepo.findByAuctionIdAndIsActiveTrueOrderByCreatedAtAsc(100L))
                .thenReturn(List.of(ab));

        service.triggerAutoBids(auction, bidder.getId());

        assertFalse(ab.getIsActive());
        verify(txRepo, never()).save(any());
        // Giá không đổi
        assertEquals(7_800_000L, auction.getCurrentPrice());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 13: Đăng ký auto-bid khi phiên không RUNNING
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Đăng ký auto-bid khi phiên FINISHED → AuctionClosedException")
    void registerAutoBid_notRunning_throwsClosed() {
        auction.setStatus(AuctionStatus.FINISHED);

        when(userRepo.findByUsername("bidder01")).thenReturn(Optional.of(bidder));
        when(auctionRepo.findById(100L)).thenReturn(Optional.of(auction));

        assertThrows(AuctionClosedException.class,
                () -> service.registerAutoBid(
                        new AutoBidRequest(100L, 8_000_000L, 500_000L), "bidder01"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Test 14: BidTransaction được lưu DB sau bid hợp lệ
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Bid hợp lệ → BidTransaction được lưu vào database")
    void placeBid_valid_savesTransaction() {
        stubBid(6_000_000L);

        service.placeBid(new PlaceBidRequest(100L, 6_000_000L), "bidder01");

        verify(txRepo, times(1)).save(argThat(tx ->
                tx.getAmount() == 6_000_000L &&
                tx.getBidType().equals("MANUAL") &&
                tx.getBidder().getUsername().equals("bidder01")
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Setup mock thông dụng cho một lần bid hợp lệ */
    private void stubBid(long amount) {
        when(auctionRepo.findByIdWithLock(100L)).thenReturn(Optional.of(auction));
        when(userRepo.findByUsername("bidder01")).thenReturn(Optional.of(bidder));
        when(auctionRepo.save(any())).thenReturn(auction);
        when(txRepo.save(any())).thenReturn(new BidTransaction());
        when(txRepo.countByAuctionId(any())).thenReturn(1L);
        when(autoBidRepo.findByAuctionIdAndIsActiveTrueOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());
    }

    private User makeUser(Long id, String username, Role role) {
        User u = new User(username, "hashed", username + "@test.com", role);
        u.setId(id);
        return u;
    }
}
