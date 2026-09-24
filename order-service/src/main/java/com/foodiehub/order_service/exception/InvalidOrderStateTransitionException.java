package com.foodiehub.order_service.exception;

public class InvalidOrderStateTransitionException
        extends RuntimeException {

    public InvalidOrderStateTransitionException(String message) {
        super(message);
    }
}