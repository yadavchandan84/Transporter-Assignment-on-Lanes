package com.freightfox.transporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Error response")
public record ErrorResponse(
        @Schema(example = "error") String status,
        @Schema(example = "Validation failed") String message,
        @Schema(description = "Detailed error messages (may be empty)") List<String> errors) {

    public static ErrorResponse of(String message, List<String> errors) {
        return new ErrorResponse("error", message, errors == null ? List.of() : List.copyOf(errors));
    }
}
