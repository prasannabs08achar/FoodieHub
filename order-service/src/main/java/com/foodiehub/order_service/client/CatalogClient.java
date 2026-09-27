package com.foodiehub.order_service.client;


import com.foodiehub.order_service.dto.CatalogMenuItemResponse;
import com.foodiehub.order_service.dto.CatalogRestaurantResponse;
import com.foodiehub.order_service.dto.DecrementStockRequest;
import com.foodiehub.order_service.dto.RestoreStockRequest;
import com.foodiehub.order_service.dto.StockOperationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/api/catalog/menu-items/{menuItemId}")
    CatalogMenuItemResponse getMenuItem(
            @PathVariable UUID menuItemId
    );

    @GetMapping("/api/catalog/restaurants/{restaurantId}")
    CatalogRestaurantResponse getRestaurant(
            @PathVariable UUID restaurantId
    );

    @PostMapping("/api/catalog/menu-items/{menuItemId}/stock/decrement")
    StockOperationResponse decrementStock(

            @PathVariable UUID menuItemId,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @RequestBody
            DecrementStockRequest request
    );

    @PostMapping("/api/catalog/menu-items/{menuItemId}/stock/restore")
    StockOperationResponse restoreStock(

            @PathVariable UUID menuItemId,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @RequestBody
            RestoreStockRequest request
    );
}