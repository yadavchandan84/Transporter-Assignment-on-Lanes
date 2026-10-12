package com.freightfox.transporter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Optimized transporter assignment")
public record AssignmentResponse(
        @Schema(example = "success") String status,
        @Schema(description = "Sum of the assigned quotes", example = "60139") long totalCost,
        @Schema(description = "One entry per lane, sorted by laneId") List<LaneAssignmentDto> assignments,
        @Schema(description = "Distinct transporters used by the assignment, ascending", example = "[1, 4, 5]")
        List<Integer> selectedTransporters,
        @Schema(description = "true when the solver proved optimality; false when the time budget ran out "
                + "and the best solution found is returned", example = "true")
        boolean optimal) {
}
