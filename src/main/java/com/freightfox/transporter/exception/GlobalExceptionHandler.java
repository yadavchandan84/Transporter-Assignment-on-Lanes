package com.freightfox.transporter.exception;

import com.freightfox.transporter.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Maps every exception to the uniform {@link ErrorResponse} body. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final int MAX_CAUSE_LENGTH = 300;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.add(fe.getField() + ": " + fe.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.add(ge.getObjectName() + ": " + ge.getDefaultMessage()));
        errors.sort(null);
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        List<String> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String param = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fe ? fe.getField() : param;
                errors.add(field + ": " + error.getDefaultMessage());
            }
        });
        errors.sort(null);
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = new ArrayList<>();
        ex.getConstraintViolations().forEach(v -> errors.add(v.getPropertyPath() + ": " + v.getMessage()));
        errors.sort(null);
        return respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String detail = cause.getMessage() == null ? "Request body is missing or unreadable" : cause.getMessage();
        detail = detail.strip();
        if (detail.length() > MAX_CAUSE_LENGTH) {
            detail = detail.substring(0, MAX_CAUSE_LENGTH);
        }
        return respond(HttpStatus.BAD_REQUEST, "Malformed JSON request", List.of(detail));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Invalid input data", ex.getErrors());
    }

    @ExceptionHandler(NoInputDataException.class)
    public ResponseEntity<ErrorResponse> handleNoData(NoInputDataException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler(InfeasibleAssignmentException.class)
    public ResponseEntity<ErrorResponse> handleInfeasible(InfeasibleAssignmentException ex) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "Resource not found", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", List.of());
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, List<String> errors) {
        return ResponseEntity.status(status).body(ErrorResponse.of(message, errors));
    }
}
