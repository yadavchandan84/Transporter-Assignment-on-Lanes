package com.freightfox.transporter.exception;

import java.util.List;

/** The stored data cannot be fully covered under the requested constraints. */
public class InfeasibleAssignmentException extends RuntimeException {

    private final List<String> errors;

    public InfeasibleAssignmentException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
