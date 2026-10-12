package com.freightfox.transporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Assignment request", example = "{\"maxTransporters\":3}")
public record AssignmentRequest(
        @Schema(description = "Maximum number of distinct transporters that may be selected", example = "3")
        @NotNull @Min(1) Integer maxTransporters) {
}
