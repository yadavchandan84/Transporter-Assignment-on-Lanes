package com.freightfox.transporter.dto;

import com.freightfox.transporter.config.ApiExamples;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Lanes and transporter quotes. Replaces any previously submitted dataset.",
        example = ApiExamples.TEST_CASE_1_INPUT)
public record InputRequest(
        @Schema(description = "Lanes to be covered (1..1000)")
        @NotEmpty @Size(max = 1000) List<@NotNull @Valid LaneDto> lanes,
        @Schema(description = "Transporters with their lane quotes (1..200)")
        @NotEmpty @Size(max = 200) List<@NotNull @Valid TransporterDto> transporters) {
}
