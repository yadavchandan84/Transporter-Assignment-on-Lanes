package com.freightfox.transporter.exception;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.freightfox.transporter.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
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
        // Jackson's own messages contain internal class names, so only a short hint is returned.
        log.debug("Unreadable request body", ex);
        return respond(HttpStatus.BAD_REQUEST, "Malformed JSON request", List.of(describeUnreadable(ex)));
    }

    private static String describeUnreadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return "Request body is missing or unreadable";
        }
        // Syntax errors inside nested values arrive wrapped in a JsonMappingException.
        for (Throwable t = cause; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof JsonParseException) {
                cause = t;
                break;
            }
        }
        if (cause instanceof JsonParseException jpe) {
            JsonLocation loc = jpe.getLocation();
            return loc == null || loc.getLineNr() < 0
                    ? "Request body is not valid JSON"
                    : "Invalid JSON syntax at line %d, column %d".formatted(loc.getLineNr(), loc.getColumnNr());
        }
        if (cause instanceof JsonMappingException jme) {
            String path = fieldPath(jme.getPath());
            if (path.isEmpty()) {
                return "Request body does not match the expected structure";
            }
            Class<?> target = jme instanceof MismatchedInputException mie ? mie.getTargetType() : null;
            return target == null ? path + ": invalid value" : path + ": expected " + describeType(target);
        }
        return "Request body is not valid JSON";
    }

    private static String fieldPath(List<JsonMappingException.Reference> refs) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : refs) {
            if (ref.getFieldName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    private static String describeType(Class<?> type) {
        if (type == int.class || type == long.class || type == short.class || type == byte.class
                || type == Integer.class || type == Long.class || type == Short.class || type == Byte.class
                || type == BigInteger.class) {
            return "an integer";
        }
        if (type == double.class || type == float.class || Number.class.isAssignableFrom(type)) {
            return "a number";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "a boolean";
        }
        if (CharSequence.class.isAssignableFrom(type) || type == char.class || type == Character.class) {
            return "a string";
        }
        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return "an array";
        }
        return "an object";
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
