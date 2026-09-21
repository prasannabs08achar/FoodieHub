package com.foodiehub.order_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(

        UUID id,

        UUID menuItemId,

        Integer quantity,

        BigDecimal unitPrice,

        BigDecimal totalPrice

) {
}