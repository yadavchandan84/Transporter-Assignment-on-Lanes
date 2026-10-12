package com.freightfox.transporter.exception;

import java.util.List;

/** Business-rule violations in submitted input (duplicate ids, unknown lane references, ...). */
public class InvalidInputException extends RuntimeException {

    private final List<String> errors;

    public InvalidInputException(List<String> errors) {
        super("Invalid input data");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
