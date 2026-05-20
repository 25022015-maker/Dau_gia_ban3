package com.auction.project.repository;

import com.auction.project.entity.Auction;
import com.auction.project.entity.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatus(AuctionStatus status);

    List<Auction> findBySellerId(Long sellerId);

    /**
     * Pessimistic Write Lock - tránh race condition khi nhiều client bid cùng 1 phiên.
     * Chỉ 1 transaction được đọc+ghi cùng lúc trên bản ghi này.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(Long id);

    /** Phiên PENDING đã đến giờ bắt đầu - Scheduler dùng để mở phiên */
    @Query("SELECT a FROM Auction a WHERE a.status = 'PENDING' AND a.startTime <= :now")
    List<Auction> findAuctionsToStart(LocalDateTime now);

    /** Phiên RUNNING đã hết giờ - Scheduler dùng để đóng phiên */
    @Query("SELECT a FROM Auction a WHERE a.status = 'RUNNING' AND a.endTime <= :now")
    List<Auction> findExpiredAuctions(LocalDateTime now);
}
