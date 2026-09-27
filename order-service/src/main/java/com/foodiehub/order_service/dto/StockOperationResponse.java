package com.foodiehub.order_service.dto;

import java.util.UUID;

public record StockOperationResponse(

        UUID menuItemId,

        Integer quantity,

        Integer remainingToday
) {
}
