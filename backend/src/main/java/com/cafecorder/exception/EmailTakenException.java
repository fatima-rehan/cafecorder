package com.cafecorder.exception;

public class EmailTakenException extends RuntimeException {

    public EmailTakenException() {
        super("An account with that email already exists");
    }
}
