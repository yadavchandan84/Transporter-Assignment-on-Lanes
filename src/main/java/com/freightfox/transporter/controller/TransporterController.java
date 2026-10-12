package com.freightfox.transporter.controller;

import com.freightfox.transporter.config.ApiExamples;
import com.freightfox.transporter.dto.AssignmentRequest;
import com.freightfox.transporter.dto.AssignmentResponse;
import com.freightfox.transporter.dto.ErrorResponse;
import com.freightfox.transporter.dto.InputRequest;
import com.freightfox.transporter.dto.InputResponse;
import com.freightfox.transporter.dto.StoredInputResponse;
import com.freightfox.transporter.service.AssignmentService;
import com.freightfox.transporter.service.TransporterInputService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transporters")
@Tag(name = "Transporter Assignment", description = "Submit lanes/quotes and compute optimized assignments")
public class TransporterController {

    private final TransporterInputService inputService;
    private final AssignmentService assignmentService;

    public TransporterController(TransporterInputService inputService, AssignmentService assignmentService) {
        this.inputService = inputService;
        this.assignmentService = assignmentService;
    }

    @Operation(summary = "Submit input data",
            description = "Stores lanes and transporter quotes. Each call replaces the previously stored dataset. "
                    + "Lane and transporter ids must be unique, quotes must reference existing lanes and be "
                    + "positive integers. A transporter may have an empty quote list.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = InputRequest.class),
            examples = {
                    @ExampleObject(name = "Test Case 1", summary = "Test Case 1 (5 lanes, 7 transporters)",
                            value = ApiExamples.TEST_CASE_1_INPUT),
                    @ExampleObject(name = "Test Case 2", summary = "Test Case 2 (5 lanes, 4 transporters)",
                            value = ApiExamples.TEST_CASE_2_INPUT),
                    @ExampleObject(name = "Main example", summary = "Main example (10 lanes, 7 transporters)",
                            value = ApiExamples.MAIN_EXAMPLE_INPUT)
            }))
    @ApiResponse(responseCode = "200", description = "Input saved", content = @Content(
            schema = @Schema(implementation = InputResponse.class),
            examples = @ExampleObject(value = ApiExamples.INPUT_SUCCESS)))
    @ApiResponse(responseCode = "400", description = "Validation failed or invalid input data", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = {
                    @ExampleObject(name = "Validation failed", value = ApiExamples.ERROR_VALIDATION),
                    @ExampleObject(name = "Invalid input data", value = ApiExamples.ERROR_INVALID_INPUT)
            }))
    @ApiResponse(responseCode = "415", description = "Content-Type is not application/json",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/input", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public InputResponse submitInput(@Valid @RequestBody InputRequest request) {
        inputService.replaceInput(request);
        return InputResponse.saved();
    }

    @Operation(summary = "Get stored input data",
            description = "Returns the currently stored lanes and transporter quotes, sorted by id.")
    @ApiResponse(responseCode = "200", description = "Stored dataset",
            content = @Content(schema = @Schema(implementation = StoredInputResponse.class)))
    @ApiResponse(responseCode = "404", description = "No data submitted yet", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.ERROR_NO_DATA)))
    @GetMapping(value = "/input", produces = MediaType.APPLICATION_JSON_VALUE)
    public StoredInputResponse getInput() {
        return inputService.getInput();
    }

    @Operation(summary = "Get optimized assignment",
            description = "Selects at most maxTransporters transporters covering every lane, assigns each lane to "
                    + "the cheapest selected transporter and minimizes total cost. Ties: more distinct "
                    + "transporters first, then the lexicographically smallest id set. Assignments are sorted by "
                    + "laneId and selectedTransporters ascending. `optimal` is true when optimality was proven "
                    + "within the solver time budget.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = AssignmentRequest.class),
            examples = @ExampleObject(name = "maxTransporters = 3", value = ApiExamples.ASSIGNMENT_REQUEST)))
    @ApiResponse(responseCode = "200", description = "Optimized assignment", content = @Content(
            schema = @Schema(implementation = AssignmentResponse.class),
            examples = @ExampleObject(name = "Test Case 1, maxTransporters = 3",
                    value = ApiExamples.ASSIGNMENT_SUCCESS_TC1)))
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No data submitted yet", content = @Content(
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = ApiExamples.ERROR_NO_DATA)))
    @ApiResponse(responseCode = "422", description = "No assignment can cover all lanes under the constraint",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = ApiExamples.ERROR_INFEASIBLE)))
    @PostMapping(value = "/assignment", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AssignmentResponse assign(@Valid @RequestBody AssignmentRequest request) {
        return assignmentService.assign(request.maxTransporters());
    }
}
