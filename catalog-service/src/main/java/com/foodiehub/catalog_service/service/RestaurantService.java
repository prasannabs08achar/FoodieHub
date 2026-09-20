package com.foodiehub.catalog_service.service;

import com.foodiehub.catalog_service.dao.RestaurantDao;
import com.foodiehub.catalog_service.dto.RestaurantRequest;
import com.foodiehub.catalog_service.dto.RestaurantResponse;
import com.foodiehub.catalog_service.exception.RestaurantNotFoundException;
import com.foodiehub.catalog_service.exception.UnauthorizedRestaurantAccessException;
import com.foodiehub.catalog_service.model.Restaurant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantDao restaurantRepository;

    @Transactional
    public RestaurantResponse createRestaurant(
            UUID ownerId,
            RestaurantRequest request) {

        Restaurant restaurant = Restaurant.builder()
                .ownerId(ownerId)
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .city(request.city())
                .cuisine(request.cuisine())
                .maxConcurrentOrders(request.maxConcurrentOrders())
                .open(true)
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        return toResponse(savedRestaurant);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurant(UUID restaurantId) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found: " + restaurantId
                        ));

        return toResponse(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> getRestaurantsByOwner(UUID ownerId) {

        return restaurantRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RestaurantResponse updateRestaurant(
            UUID restaurantId,
            UUID ownerId,
            RestaurantRequest request) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found: " + restaurantId
                        ));

        // Owner-scoped access
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedRestaurantAccessException(
                    "You are not allowed to modify this restaurant"
            );
        }

        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setAddress(request.address());
        restaurant.setCity(request.city());
        restaurant.setCuisine(request.cuisine());
        restaurant.setMaxConcurrentOrders(
                request.maxConcurrentOrders()
        );

        return toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional
    public void deleteRestaurant(
            UUID restaurantId,
            UUID ownerId) {

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                new RestaurantNotFoundException(
                        "Restaurant not found: " + restaurantId
                ));

        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedRestaurantAccessException(
                    "You are not allowed to modify this restaurant"
            );
        }

        restaurantRepository.delete(restaurant);
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {

        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getOwnerId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                restaurant.getCity(),
                restaurant.getCuisine(),
                restaurant.getMaxConcurrentOrders(),
                restaurant.getOpen(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt()
        );
    }
}