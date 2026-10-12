package com.freightfox.transporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "A transporter and its lane quotes")
public record TransporterDto(
        @Schema(description = "Unique transporter id", example = "1")
        @NotNull @Positive Integer id,
        @Schema(description = "Transporter name", example = "Transporter T1")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "Quotes for the lanes this transporter serves (may be empty)")
        @NotNull @Size(max = 1000) List<@NotNull @Valid LaneQuoteDto> laneQuotes) {
}
