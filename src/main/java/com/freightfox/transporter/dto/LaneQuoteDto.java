package com.freightfox.transporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "A transporter's price quote for one lane")
public record LaneQuoteDto(
        @Schema(description = "Id of the quoted lane", example = "1")
        @NotNull @Positive Integer laneId,
        @Schema(description = "Quoted price (positive integer)", example = "5000")
        @NotNull @Positive @Max(1_000_000_000L) Long quote) {
}
