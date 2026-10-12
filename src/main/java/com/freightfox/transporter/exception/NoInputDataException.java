package com.freightfox.transporter.exception;

/** No dataset has been submitted yet. */
public class NoInputDataException extends RuntimeException {

    public NoInputDataException() {
        super("No input data found. Submit data via POST /api/v1/transporters/input first.");
    }
}
