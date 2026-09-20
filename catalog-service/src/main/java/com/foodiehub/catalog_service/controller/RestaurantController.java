package com.foodiehub.catalog_service.controller;

import com.foodiehub.catalog_service.dto.RestaurantRequest;
import com.foodiehub.catalog_service.dto.RestaurantResponse;
import com.foodiehub.catalog_service.model.IdempotencyRecord;
import com.foodiehub.catalog_service.service.IdempotencyService;
import com.foodiehub.catalog_service.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<?> createRestaurant(

            @RequestHeader("X-User-Id")
            UUID ownerId,

            @RequestHeader("Idempotency-Key")
            String idempotencyKey,

            @Valid
            @RequestBody
            RestaurantRequest request
    ) {

        // 1. Validate key
        idempotencyService.validateKey(
                idempotencyKey
        );

        String operation = "CREATE_RESTAURANT";

        // 2. Generate hash of this request
        String requestHash =
                idempotencyService.generateRequestHash(
                        ownerId,
                        request
                );

        // 3. Check if this key was already processed
        var existingRecord =
                idempotencyService.findByKey(
                        idempotencyKey
                );

        if (existingRecord.isPresent()) {

            IdempotencyRecord record =
                    existingRecord.get();

            // 4. Make sure same key wasn't reused
            //    for a different request
            idempotencyService.validateExistingRecord(
                    record,
                    operation,
                    requestHash
            );

            // 5. Same request → return original response
            return ResponseEntity
                    .status(record.getResponseStatus())
                    .body(
                            idempotencyService
                                    .getStoredResponse(record)
                    );
        }

        // 6. First request → create restaurant
        RestaurantResponse response =
                restaurantService.createRestaurant(
                        ownerId,
                        request
                );

        // 7. Store response for future retries
        idempotencyService.saveResponse(
                idempotencyKey,
                operation,
                requestHash,
                200,
                response
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantResponse> getRestaurant(
            @PathVariable UUID restaurantId
    ) {

        return ResponseEntity.ok(
                restaurantService.getRestaurant(
                        restaurantId
                )
        );
    }

    @GetMapping("/owner")
    public ResponseEntity<?> getOwnerRestaurants(
            @RequestHeader("X-User-Id")
            UUID ownerId
    ) {

        return ResponseEntity.ok(
                restaurantService.getRestaurantsByOwner(
                        ownerId
                )
        );
    }

    @PutMapping("/{restaurantId}")
    public ResponseEntity<?> updateRestaurant(
            @PathVariable UUID restaurantId,

            @RequestHeader("X-User-Id")
            UUID ownerId,

            @Valid
            @RequestBody
            RestaurantRequest request
    ) {

        return ResponseEntity.ok(
                restaurantService.updateRestaurant(
                        restaurantId,
                        ownerId,
                        request
                )
        );
    }

    @DeleteMapping("/{restaurantId}")
    public ResponseEntity<Void> deleteRestaurant(
            @PathVariable UUID restaurantId,

            @RequestHeader("X-User-Id")
            UUID ownerId
    ) {

        restaurantService.deleteRestaurant(
                restaurantId,
                ownerId
        );

        return ResponseEntity.noContent().build();
    }
}