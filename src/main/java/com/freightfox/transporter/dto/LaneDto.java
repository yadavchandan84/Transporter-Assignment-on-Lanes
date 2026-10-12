package com.freightfox.transporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "A lane (origin -> destination route)")
public record LaneDto(
        @Schema(description = "Unique lane id", example = "1")
        @NotNull @Positive Integer id,
        @Schema(description = "Origin city", example = "Mumbai")
        @NotBlank @Size(max = 100) String origin,
        @Schema(description = "Destination city", example = "Delhi")
        @NotBlank @Size(max = 100) String destination) {
}
