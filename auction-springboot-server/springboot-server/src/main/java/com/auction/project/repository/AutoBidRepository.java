package com.auction.project.repository;

import com.auction.project.entity.AutoBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface AutoBidRepository extends JpaRepository<AutoBid, Long> {

    /**
     * Tất cả auto-bid còn active của 1 phiên.
     * Sort theo createdAt ASC = ưu tiên người đăng ký trước (tie-breaking).
     */
    List<AutoBid> findByAuctionIdAndIsActiveTrueOrderByCreatedAtAsc(Long auctionId);

    Optional<AutoBid> findByAuctionIdAndUserId(Long auctionId, Long userId);

    List<AutoBid> findByUserId(Long userId);

    /** Xóa toàn bộ auto-bid của 1 phiên */
    @Transactional
    @Modifying
    @Query("DELETE FROM AutoBid ab WHERE ab.auction.id = :auctionId")
    void deleteByAuctionId(@Param("auctionId") Long auctionId);
}
