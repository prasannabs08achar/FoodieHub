package com.foodiehub.catalog_service.service;

import com.foodiehub.catalog_service.dao.IdempotencyRecordDao;
import com.foodiehub.catalog_service.dao.MenuItemDao;
import com.foodiehub.catalog_service.dao.RestaurantDao;
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

    private static final String DECREMENT_STOCK = "DECREMENT_STOCK";
    private final MenuItemDao menuItemRepository;
    private final RestaurantDao restaurantRepository;
    private final IdempotencyService idempotencyService;
    private final IdempotencyRecordDao idempotencyRecordDao;

    @Transactional
    public MenuItemResponse createMenuItem(
            UUID restaurantId,
            UUID ownerId,
            MenuItemRequest request) {

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

    @Transactional(readOnly = true)
    public List<MenuItemResponse> getRestaurantMenu(
            UUID restaurantId) {

        return menuItemRepository
                .findByRestaurantId(restaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse getMenuItem(
            UUID menuItemId) {

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

    @Transactional
    public MenuItemResponse updateMenuItem(
            UUID menuItemId,
            UUID ownerId,
            MenuItemRequest request) {

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

        menuItem.setName(request.name());
        menuItem.setDescription(request.description());
        menuItem.setPrice(request.price());
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
         * For now, changing DailyQuantity resets
         * today's remaining quantity.
         */
        menuItem.setRemainingToday(
                request.dailyQuantity()
        );

        return toResponse(
                menuItemRepository.save(menuItem)
        );
    }

    @Transactional
    public void deleteMenuItem(
            UUID menuItemId,
            UUID ownerId) {

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

        menuItemRepository.delete(menuItem);
    }

    private Restaurant getRestaurant(
            UUID restaurantId) {

        return restaurantRepository
                .findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found: "
                                        + restaurantId
                        )
                );
    }

    private void validateOwner(
            Restaurant restaurant,
            UUID ownerId) {

        if (!restaurant.getOwnerId().equals(ownerId)) {

            throw new UnauthorizedRestaurantAccessException(
                    "You are not allowed to modify this restaurant"
            );
        }
    }

    private void validateTimeWindow(
            MenuItemRequest request) {

        if (request.availableFrom() != null
                && request.availableTo() != null
                && !request.availableTo()
                .isAfter(request.availableFrom())) {

            throw new IllegalArgumentException(
                    "AvailableTo must be after AvailableFrom"
            );
        }
    }

    private MenuItemResponse toResponse(
            MenuItem item) {

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
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }


    @Transactional
    public StockOperationResponse decrementStock(
            UUID menuItemId,
            Integer quantity,
            String idempotencyKey
    ) {

        // 1. Validate Idempotency-Key
        idempotencyService.validateKey(
                idempotencyKey
        );

        String operation = "DECREMENT_STOCK";

        // 2. Generate request hash
        //
        // For stock decrement, the logical request is:
        // menuItemId + quantity
        //
        String requestData =
                menuItemId + ":" + quantity;

        String requestHash =
                idempotencyService.generateRequestHash(
                        requestData
                );

        // 3. Check whether this idempotency key
        //    was already processed
        Optional<IdempotencyRecord> existingRecord =
                idempotencyService.findByKey(
                        idempotencyKey
                );

        if (existingRecord.isPresent()) {

            IdempotencyRecord record =
                    existingRecord.get();

            // 4. Validate that the same key
            //    represents the same operation/request
            idempotencyService.validateExistingRecord(
                    record,
                    operation,
                    requestHash
            );

            // 5. Return the original response
            return idempotencyService.getStoredResponse(
                    record,
                    StockOperationResponse.class
            );
        }

        // 6. First request → lock the menu item row
        MenuItem menuItem =
                menuItemRepository
                        .findByIdForUpdate(menuItemId)
                        .orElseThrow(() ->
                                new MenuItemNotFoundException(
                                        "Menu item not found: "
                                                + menuItemId
                                )
                        );

        // 7. Validate active status
        if (!Boolean.TRUE.equals(
                menuItem.getActive()
        )) {

            throw new MenuItemNotFoundException(
                    "Menu item is inactive"
            );
        }

        // 8. Validate time window
        LocalTime now = LocalTime.now();

        if (menuItem.getAvailableFrom() != null
                && menuItem.getAvailableTo() != null
                && (now.isBefore(
                menuItem.getAvailableFrom()
        )
                || now.isAfter(
                menuItem.getAvailableTo()
        ))) {

            throw new MenuItemNotFoundException(
                    "Menu item is not available at this time"
            );
        }

        // 9. Validate stock
        if (menuItem.getRemainingToday() < quantity) {

            throw new InsufficientStockException(
                    "Insufficient stock for menu item: "
                            + menuItemId
            );
        }

        // 10. Decrement stock
        menuItem.setRemainingToday(
                menuItem.getRemainingToday() - quantity
        );

        menuItemRepository.save(menuItem);

        // 11. Create response
        StockOperationResponse response =
                new StockOperationResponse(
                        menuItem.getId(),
                        quantity,
                        menuItem.getRemainingToday()
                );

        // 12. Store response for idempotent retry
        idempotencyService.saveResponse(
                idempotencyKey,
                operation,
                requestHash,
                200,
                response
        );

        return response;
    }
    @Transactional
    public StockOperationResponse restoreStock(
            UUID menuItemId,
            Integer quantity
    ) {

        MenuItem menuItem =
                menuItemRepository
                        .findByIdForUpdate(menuItemId)
                        .orElseThrow(() ->
                                new MenuItemNotFoundException(
                                        "Menu item not found: " + menuItemId
                                )
                        );

        int currentStock = menuItem.getRemainingToday();

        int restoredStock = currentStock + quantity;

        /*
         * Do not allow today's stock to exceed
         * the configured daily quantity.
         */
        if (restoredStock > menuItem.getDailyQuantity()) {
            throw new IllegalArgumentException(
                    "Restored stock cannot exceed daily quantity"
            );
        }

        menuItem.setRemainingToday(restoredStock);

        menuItemRepository.save(menuItem);

        return new StockOperationResponse(
                menuItem.getId(),
                quantity,
                restoredStock
        );
    }
}