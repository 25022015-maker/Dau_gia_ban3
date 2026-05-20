package com.auction.project.dto;

import jakarta.validation.constraints.*;

public record CreateAuctionRequest(
    @NotBlank String itemName,
    String description,
    String imageBase64,
    /** ELECTRONICS | ART | VEHICLE */
    String itemType,
    @NotNull @Min(1000) Long startPrice,
    @Min(1000) Long minBid,
    @NotBlank String startTime,   // ISO format: 2024-01-15T10:00:00
    @NotBlank String endTime,
    Integer antiSnipeExtSeconds,  // mặc định 60

    // Electronics
    String brand,
    Integer warrantyMonths,

    // Art
    String artist,
    Integer yearCreated,

    // Vehicle
    String vehicleBrand,
    Integer manufactureYear,
    Integer mileageKm
) {
    public CreateAuctionRequest {
        if (itemType == null || itemType.isBlank()) itemType = "ELECTRONICS";
        if (minBid == null) minBid = 1000L;
        if (antiSnipeExtSeconds == null) antiSnipeExtSeconds = 60;
    }
}
