package com.foodiehub.dispatch_service.exception;

public class BatchLifecycleException
        extends RuntimeException {

    public BatchLifecycleException(
            String message
    ) {
        super(message);
    }
}
