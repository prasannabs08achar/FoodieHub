package com.foodiehub.catalog_service.controller;

import com.foodiehub.catalog_service.dto.*;
import com.foodiehub.catalog_service.model.IdempotencyRecord;
import com.foodiehub.catalog_service.service.IdempotencyService;
import com.foodiehub.catalog_service.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class MenuItemController {

    private final MenuItemService menuItemService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    public ResponseEntity<?> createMenuItem(

            @PathVariable
            UUID restaurantId,

            @RequestHeader("X-User-Id")
            UUID ownerId,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            MenuItemRequest request
    ) {

        // 1. Validate key
        idempotencyService.validateKey(
                idempotencyKey
        );

        String operation = "CREATE_MENU_ITEM";

        // 2. Include owner + restaurant + request
        //    in the request hash
        String requestHash =
                idempotencyService.generateRequestHash(
                        ownerId,
                        new MenuItemIdempotencyData(
                                restaurantId,
                                request
                        )
                );

        // 3. Check whether key already exists
        var existingRecord =
                idempotencyService.findByKey(
                        idempotencyKey
                );

        if (existingRecord.isPresent()) {

            IdempotencyRecord record =
                    existingRecord.get();

            // 4. Validate key reuse
            idempotencyService.validateExistingRecord(
                    record,
                    operation,
                    requestHash
            );

            // 5. Return original response
            return ResponseEntity
                    .status(record.getResponseStatus())
                    .body(
                            idempotencyService
                                    .getStoredResponse(record)
                    );
        }

        // 6. First request → create menu item
        MenuItemResponse response =
                menuItemService.createMenuItem(
                        restaurantId,
                        ownerId,
                        request
                );

        // 7. Store response
        idempotencyService.saveResponse(
                idempotencyKey,
                operation,
                requestHash,
                200,
                response
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/restaurants/{restaurantId}/menu-items")
    public ResponseEntity<?> getRestaurantMenu(
            @PathVariable UUID restaurantId
    ) {

        return ResponseEntity.ok(
                menuItemService.getRestaurantMenu(
                        restaurantId
                )
        );
    }

    @GetMapping("/menu-items/{menuItemId}")
    public ResponseEntity<?> getMenuItem(
            @PathVariable UUID menuItemId
    ) {

        return ResponseEntity.ok(
                menuItemService.getMenuItem(
                        menuItemId
                )
        );
    }

    @PutMapping("/menu-items/{menuItemId}")
    public ResponseEntity<?> updateMenuItem(
            @PathVariable UUID menuItemId,

            @RequestHeader("X-User-Id")
            UUID ownerId,

            @Valid
            @RequestBody
            MenuItemRequest request
    ) {

        return ResponseEntity.ok(
                menuItemService.updateMenuItem(
                        menuItemId,
                        ownerId,
                        request
                )
        );
    }

    @DeleteMapping("/menu-items/{menuItemId}")
    public ResponseEntity<Void> deleteMenuItem(
            @PathVariable UUID menuItemId,

            @RequestHeader("X-User-Id")
            UUID ownerId
    ) {

        menuItemService.deleteMenuItem(
                menuItemId,
                ownerId
        );

        return ResponseEntity.noContent().build();
    }
    @PostMapping("/menu-items/{menuItemId}/stock/decrement")
    public ResponseEntity<StockOperationResponse> decrementStock(

            @PathVariable
            UUID menuItemId,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            DecrementStockRequest request
    ) {

        return ResponseEntity.ok(
                menuItemService.decrementStock(
                        menuItemId,
                        request.quantity(),
                        idempotencyKey
                )
        );
    }
    @PostMapping("/menu-items/{menuItemId}/stock/restore")
    public ResponseEntity<StockOperationResponse> restoreStock(

            @PathVariable
            UUID menuItemId,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            RestoreStockRequest request
    ) {

        return ResponseEntity.ok(
                menuItemService.restoreStock(
                        menuItemId,
                        request.quantity(),
                        idempotencyKey
                )
        );
    }

    /*
     * Data used only for generating the idempotency hash.
     */
    private record MenuItemIdempotencyData(
            UUID restaurantId,
            MenuItemRequest request
    ) {
    }
}