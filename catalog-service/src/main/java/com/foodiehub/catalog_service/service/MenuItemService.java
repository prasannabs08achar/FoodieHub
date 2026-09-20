package com.foodiehub.catalog_service.service;

import com.foodiehub.catalog_service.dao.MenuItemDao;
import com.foodiehub.catalog_service.dao.RestaurantDao;
import com.foodiehub.catalog_service.dto.MenuItemRequest;
import com.foodiehub.catalog_service.dto.MenuItemResponse;
import com.foodiehub.catalog_service.exception.MenuItemNotFoundException;
import com.foodiehub.catalog_service.exception.RestaurantNotFoundException;
import com.foodiehub.catalog_service.exception.UnauthorizedRestaurantAccessException;
import com.foodiehub.catalog_service.model.MenuItem;
import com.foodiehub.catalog_service.model.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemDao menuItemRepository;
    private final RestaurantDao restaurantRepository;

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
}