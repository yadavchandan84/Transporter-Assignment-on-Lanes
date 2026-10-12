package com.freightfox.transporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lane assigned to a transporter")
public record LaneAssignmentDto(
        @Schema(example = "1") int laneId,
        @Schema(example = "5") int transporterId) {
}
