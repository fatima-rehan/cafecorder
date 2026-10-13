package com.cafecorder.exception;

public class VisitNotFoundException extends RuntimeException {

    public VisitNotFoundException(Long id) {
        super("No visit found with id " + id);
    }
}
