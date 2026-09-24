package com.foodiehub.order_service.service;

import com.foodiehub.order_service.exception.InvalidOrderStateTransitionException;
import com.foodiehub.order_service.model.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderStateTransitionValidator {

    public void validate(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {

        if (currentStatus == null) {
            throw new InvalidOrderStateTransitionException(
                    "Current order status cannot be null"
            );
        }

        if (newStatus == null) {
            throw new InvalidOrderStateTransitionException(
                    "New order status cannot be null"
            );
        }

        boolean valid = switch (currentStatus) {

            case PLACED ->
                    newStatus == OrderStatus.ACCEPTED
                            || newStatus == OrderStatus.CANCELLED;

            case ACCEPTED ->
                    newStatus == OrderStatus.PREPARING
                            || newStatus == OrderStatus.CANCELLED;

            case PREPARING ->
                    newStatus == OrderStatus.READY_FOR_PICKUP
                            || newStatus == OrderStatus.CANCELLED;

            case READY_FOR_PICKUP ->
                    newStatus == OrderStatus.PICKED_UP
                            || newStatus == OrderStatus.CANCELLED;

            case PICKED_UP ->
                    newStatus == OrderStatus.DELIVERED
                            || newStatus == OrderStatus.CANCELLED;

            case DELIVERED, CANCELLED ->
                    false;
        };

        if (!valid) {
            throw new InvalidOrderStateTransitionException(
                    "Invalid order state transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }
}