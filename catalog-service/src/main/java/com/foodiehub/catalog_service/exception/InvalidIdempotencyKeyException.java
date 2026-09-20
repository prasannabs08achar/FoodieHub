package com.foodiehub.catalog_service.exception;

public class InvalidIdempotencyKeyException
        extends RuntimeException {

    public InvalidIdempotencyKeyException(String message) {
        super(message);
    }
}