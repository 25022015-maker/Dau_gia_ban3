package com.auction.project.dto;

import jakarta.validation.constraints.*;

public record PlaceBidRequest(
    @NotNull Long auctionId,
    @NotNull @Min(1) Long amount
) {}
