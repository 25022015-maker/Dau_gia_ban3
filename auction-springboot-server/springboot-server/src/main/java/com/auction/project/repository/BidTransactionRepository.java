package com.auction.project.repository;

import com.auction.project.entity.BidTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BidTransactionRepository extends JpaRepository<BidTransaction, Long> {

    /** Lịch sử bid của 1 phiên, mới nhất lên đầu */
    List<BidTransaction> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

    /** Đếm số lượt bid của 1 phiên */
    long countByAuctionId(Long auctionId);

    /** Kiểm tra user đã từng bid vào phiên này chưa */
    boolean existsByAuctionIdAndBidderId(Long auctionId, Long bidderId);
}
