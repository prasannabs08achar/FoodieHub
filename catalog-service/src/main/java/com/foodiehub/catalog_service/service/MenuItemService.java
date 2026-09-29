package com.foodiehub.catalog_service.service;

import com.foodiehub.catalog_service.dao.IdempotencyRecordDao;
import com.foodiehub.catalog_service.dao.MenuItemDao;
import com.foodiehub.catalog_service.dao.RestaurantDao;
import com.foodiehub.catalog_service.dto.MenuItemAvailabilityStatus;
import com.foodiehub.catalog_service.dto.MenuItemRequest;
import com.foodiehub.catalog_service.dto.MenuItemResponse;
import com.foodiehub.catalog_service.dto.StockOperationResponse;
import com.foodiehub.catalog_service.exception.InsufficientStockException;
import com.foodiehub.catalog_service.exception.MenuItemNotFoundException;

import com.foodiehub.catalog_service.exception.RestaurantNotFoundException;
import com.foodiehub.catalog_service.exception.UnauthorizedRestaurantAccessException;
import com.foodiehub.catalog_service.model.IdempotencyRecord;
import com.foodiehub.catalog_service.model.MenuItem;
import com.foodiehub.catalog_service.model.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private static final String DECREMENT_STOCK =
            "DECREMENT_STOCK";
    private static final String RESTORE_STOCK =
            "RESTORE_STOCK";

    private final MenuItemDao menuItemRepository;
    private final RestaurantDao restaurantRepository;
    private final IdempotencyService idempotencyService;
    private final IdempotencyRecordDao idempotencyRecordDao;


    // =========================================================
    // CREATE MENU ITEM
    // =========================================================

    @Transactional
    public MenuItemResponse createMenuItem(
            UUID restaurantId,
            UUID ownerId,
            MenuItemRequest request
    ) {

        Restaurant restaurant =
                getRestaurant(restaurantId);

        validateOwner(
                restaurant,
                ownerId
        );

        validateTimeWindow(request);

        MenuItem menuItem =
                MenuItem.builder()
                        .restaurantId(restaurantId)
                        .name(request.name())
                        .description(request.description())
                        .price(request.price())
                        .dailyQuantity(request.dailyQuantity())
                        .remainingToday(request.dailyQuantity())
                        .availableFrom(request.availableFrom())
                        .availableTo(request.availableTo())
                        .active(true)
                        .build();

        return toResponse(
                menuItemRepository.save(menuItem)
        );
    }


    // =========================================================
    // GET RESTAURANT MENU
    // =========================================================

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getRestaurantMenu(
            UUID restaurantId
    ) {

        return menuItemRepository
                .findByRestaurantId(restaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // =========================================================
    // GET MENU ITEM
    // =========================================================

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItem(
            UUID menuItemId
    ) {

        MenuItem menuItem =
                menuItemRepository.findById(menuItemId)
                        .orElseThrow(() ->
                                new MenuItemNotFoundException(
                                        "Menu item not found: "
                                                + menuItemId
                                )
                        );

        return toResponse(menuItem);
    }


    // =========================================================
    // UPDATE MENU ITEM
    // =========================================================

    @Transactional
    public MenuItemResponse updateMenuItem(
            UUID menuItemId,
            UUID ownerId,
            MenuItemRequest request
    ) {

        MenuItem menuItem =
                menuItemRepository.findById(menuItemId)
                        .orElseThrow(() ->
                                new MenuItemNotFoundException(
                                        "Menu item not found: "
                                                + menuItemId
                                )
                        );

        Restaurant restaurant =
                getRestaurant(
                        menuItem.getRestaurantId()
                );

        validateOwner(
                restaurant,
                ownerId
        );

        validateTimeWindow(request);

        menuItem.setName(
                request.name()
        );

        menuItem.setDescription(
                request.description()
        );

        menuItem.setPrice(
                request.price()
        );

        menuItem.setDailyQuantity(
                request.dailyQuantity()
        );

        menuItem.setAvailableFrom(
                request.availableFrom()
        );

        menuItem.setAvailableTo(
                request.availableTo()
        );

        /*
         * Changing DailyQuantity resets
         * today's remaining quantity.
         */
        menuItem.setRemainingToday(
                request.dailyQuantity()
        );

        return toResponse(
                menuItemRepository.save(menuItem)
        );
    }


    // =========================================================
    // DELETE MENU ITEM
    // =========================================================

    @Transactional
    public void deleteMenuItem(
            UUID menuItemId,
            UUID ownerId
    ) {

        MenuItem menuItem =
                menuItemRepository.findById(menuItemId)
                        .orElseThrow(() ->
                                new MenuItemNotFoundException(
                                        "Menu item not found: "
                                                + menuItemId
                                )
                        );

        Restaurant restaurant =
                getRestaurant(
                        menuItem.getRestaurantId()
                );

        validateOwner(
                restaurant,
                ownerId
        );

        menuItemRepository.delete(
                menuItem
        );
    }


    // =========================================================
    // GET RESTAURANT
    // =========================================================

    private Restaurant getRestaurant(
            UUID restaurantId
    ) {

        return restaurantRepository
                .findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found: "
                                        + restaurantId
                        )
                );
    }


    // =========================================================
    // VALIDATE OWNER
    // =========================================================

    private void validateOwner(
            Restaurant restaurant,
            UUID ownerId
    ) {

        if (!restaurant.getOwnerId().equals(
                ownerId
        )) {

            throw new UnauthorizedRestaurantAccessException(
                    "You are not allowed to modify this restaurant"
            );
        }
    }


    // =========================================================
    // VALIDATE TIME WINDOW
    // =========================================================

    private void validateTimeWindow(
            MenuItemRequest request
    ) {

        if (request.availableFrom() != null
                && request.availableTo() != null
                && !request.availableTo()
                .isAfter(request.availableFrom())) {

            throw new IllegalArgumentException(
                    "AvailableTo must be after AvailableFrom"
            );
        }
    }


    // =========================================================
    // MAP ENTITY → RESPONSE
    // =========================================================

    private MenuItemResponse toResponse(
            MenuItem item
    ) {

        return new MenuItemResponse(
                item.getId(),
                item.getRestaurantId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getDailyQuantity(),
                item.getRemainingToday(),
                item.getAvailableFrom(),
                item.getAvailableTo(),
                item.getActive(),
                calculateAvailabilityStatus(item),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }


    // =========================================================
    // CALCULATE AVAILABILITY STATUS
    // =========================================================

    private MenuItemAvailabilityStatus calculateAvailabilityStatus(
            MenuItem item
    ) {

        /*
         * -----------------------------------------------------
         * GATE 1
         * -----------------------------------------------------
         *
         * RemainingToday must be greater than zero.
         */
        if (item.getRemainingToday() == null
                || item.getRemainingToday() <= 0) {

            return MenuItemAvailabilityStatus.OUT_OF_STOCK;
        }


        /*
         * -----------------------------------------------------
         * ACTIVE CHECK
         * -----------------------------------------------------
         *
         * Inactive items cannot be ordered.
         *
         * The current availability enum does not contain
         * INACTIVE, so it is represented as unavailable.
         */
        if (!Boolean.TRUE.equals(
                item.getActive()
        )) {

            return MenuItemAvailabilityStatus.OUTSIDE_TIME_WINDOW;
        }


        /*
         * -----------------------------------------------------
         * GATE 2
         * -----------------------------------------------------
         *
         * No time window means the item is available
         * throughout the day.
         */
        if (item.getAvailableFrom() == null
                || item.getAvailableTo() == null) {

            return MenuItemAvailabilityStatus.AVAILABLE;
        }


        LocalTime now =
                LocalTime.now();


        /*
         * -----------------------------------------------------
         * TIME WINDOW
         * -----------------------------------------------------
         *
         * Valid interval:
         *
         * AvailableFrom <= current time <= AvailableTo
         */
        boolean insideTimeWindow =
                !now.isBefore(
                        item.getAvailableFrom()
                )
                        &&
                        !now.isAfter(
                                item.getAvailableTo()
                        );


        if (!insideTimeWindow) {

            return MenuItemAvailabilityStatus.OUTSIDE_TIME_WINDOW;
        }


        /*
         * Both gates passed.
         */
        return MenuItemAvailabilityStatus.AVAILABLE;
    }


    // =========================================================
    // DECREMENT STOCK
    // =========================================================

    @Transactional
    public StockOperationResponse decrementStock(
            UUID menuItemId,
            Integer quantity,
            String idempotencyKey
    ) {

        // -----------------------------------------------------
        // 1. Validate Idempotency-Key
        // -----------------------------------------------------

        idempotencyService.validateKey(
                idempotencyKey
        );


        // -----------------------------------------------------
        // 2. Generate request hash
        // -----------------------------------------------------

        String operation =
                DECREMENT_STOCK;

        String requestData =
                menuItemId + ":" + quantity;

        String requestHash =
                idempotencyService.generateRequestHash(
                        requestData
                );


        // -----------------------------------------------------
        // 3. Check existing idempotency record
        // -----------------------------------------------------

        Optional<IdempotencyRecord> existingRecord =
                idempotencyService.findByKey(
                        idempotencyKey
                );


        if (existingRecord.isPresent()) {

            IdempotencyRecord record =
                    existingRecord.get();


            // -------------------------------------------------
            // 4. Validate same request
            // -------------------------------------------------

            idempotencyService.validateExistingRecord(
                    record,
                    operation,
                    requestHash
            );


            // -------------------------------------------------
            // 5. Return original response
            // -------------------------------------------------

            return idempotencyService.getStoredResponse(
                    record,
                    StockOperationResponse.class
            );
        }


        // -----------------------------------------------------
        // 6. Lock menu item
        // -----------------------------------------------------

        MenuItem menuItem =
                menuItemRepository
                        .findByIdForUpdate(
                                menuItemId
                        )
                        .orElseThrow(() ->
                                new MenuItemNotFoundException(
                                        "Menu item not found: "
                                                + menuItemId
                                )
                        );


        // -----------------------------------------------------
        // 7. Validate active status
        // -----------------------------------------------------

        if (!Boolean.TRUE.equals(
                menuItem.getActive()
        )) {

            throw new MenuItemNotFoundException(
                    "Menu item is inactive"
            );
        }


        // -----------------------------------------------------
        // 8. Validate time window
        // -----------------------------------------------------

        LocalTime now =
                LocalTime.now();

        if (menuItem.getAvailableFrom() != null
                && menuItem.getAvailableTo() != null
                && (
                now.isBefore(
                        menuItem.getAvailableFrom()
                )
                        ||
                        now.isAfter(
                                menuItem.getAvailableTo()
                        )
        )) {

            throw new MenuItemNotFoundException(
                    "Menu item is not available at this time"
            );
        }


        // -----------------------------------------------------
        // 9. Validate stock
        // -----------------------------------------------------

        if (menuItem.getRemainingToday() == null
                || menuItem.getRemainingToday() < quantity) {

            throw new InsufficientStockException(
                    "Insufficient stock for menu item: "
                            + menuItemId
            );
        }


        // -----------------------------------------------------
        // 10. Decrement stock
        // -----------------------------------------------------

        menuItem.setRemainingToday(
                menuItem.getRemainingToday()
                        - quantity
        );

        menuItemRepository.save(
                menuItem
        );


        // -----------------------------------------------------
        // 11. Create response
        // -----------------------------------------------------

        StockOperationResponse response =
                new StockOperationResponse(
                        menuItem.getId(),
                        quantity,
                        menuItem.getRemainingToday()
                );


        // -----------------------------------------------------
        // 12. Store idempotency response
        // -----------------------------------------------------

        idempotencyService.saveResponse(
                idempotencyKey,
                operation,
                requestHash,
                200,
                response
        );


        return response;
    }


    // =========================================================
    // RESTORE STOCK
    // =========================================================


    @Transactional
    public StockOperationResponse restoreStock(
            UUID menuItemId,
            Integer quantity,
            String idempotencyKey
    ) {

        // -----------------------------------------------------
        // 1. Validate Idempotency-Key
        // -----------------------------------------------------

        idempotencyService.validateKey(
                idempotencyKey
        );


        // -----------------------------------------------------
        // 2. Generate request hash
        // -----------------------------------------------------

        String operation =
                RESTORE_STOCK;

        String requestData =
                menuItemId + ":" + quantity;

        String requestHash =
                idempotencyService.generateRequestHash(
                        requestData
                );


        // -----------------------------------------------------
        // 3. Check existing idempotency record
        // -----------------------------------------------------

        Optional<IdempotencyRecord> existingRecord =
                idempotencyService.findByKey(
                        idempotencyKey
                );


        if (existingRecord.isPresent()) {

            IdempotencyRecord record =
                    existingRecord.get();


            // -------------------------------------------------
            // 4. Validate same request
            // -------------------------------------------------

            idempotencyService.validateExistingRecord(
                    record,
                    operation,
                    requestHash
            );


            // -------------------------------------------------
            // 5. Return original response
            // -------------------------------------------------

            return idempotencyService.getStoredResponse(
                    record,
                    StockOperationResponse.class
            );
        }


        // -----------------------------------------------------
        // 6. Lock menu item
        // -----------------------------------------------------

        MenuItem menuItem =
                menuItemRepository
                        .findByIdForUpdate(
                                menuItemId
                        )
                        .orElseThrow(() ->
                                new MenuItemNotFoundException(
                                        "Menu item not found: "
                                                + menuItemId
                                )
                        );


        // -----------------------------------------------------
        // 7. Calculate restored stock
        // -----------------------------------------------------

        int currentStock =
                menuItem.getRemainingToday();

        int restoredStock =
                currentStock + quantity;


        // -----------------------------------------------------
        // 8. Do not exceed daily quantity
        // -----------------------------------------------------

        if (restoredStock >
                menuItem.getDailyQuantity()) {

            throw new IllegalArgumentException(
                    "Restored stock cannot exceed daily quantity"
            );
        }


        // -----------------------------------------------------
        // 9. Update stock
        // -----------------------------------------------------

        menuItem.setRemainingToday(
                restoredStock
        );

        menuItemRepository.save(
                menuItem
        );


        // -----------------------------------------------------
        // 10. Create response
        // -----------------------------------------------------

        StockOperationResponse response =
                new StockOperationResponse(
                        menuItem.getId(),
                        quantity,
                        restoredStock
                );


        // -----------------------------------------------------
        // 11. Store idempotency response
        // -----------------------------------------------------

        idempotencyService.saveResponse(
                idempotencyKey,
                operation,
                requestHash,
                200,
                response
        );


        return response;
    }
}