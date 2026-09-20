package com.foodiehub.catalog_service.exception;

public class UnauthorizedRestaurantAccessException
        extends RuntimeException {

    public UnauthorizedRestaurantAccessException(String message) {
        super(message);
    }
}