package com.freightfox.transporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Currently stored dataset")
public record StoredInputResponse(
        @Schema(example = "success") String status,
        List<LaneDto> lanes,
        List<TransporterDto> transporters) {
}
