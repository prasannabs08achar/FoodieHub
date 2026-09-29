package com.foodiehub.order_service.service;

import com.foodiehub.order_service.dao.KitchenCapacityDao;
import com.foodiehub.order_service.model.KitchenCapacity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenCapacityService {

    private final KitchenCapacityDao kitchenCapacityDao;

    /*
     * Creates the KitchenCapacity synchronization record
     * if it does not already exist.
     */
    @Transactional
    public KitchenCapacity ensureExists(
            UUID restaurantId
    ) {

        return kitchenCapacityDao
                .findByRestaurantId(restaurantId)
                .orElseGet(() ->
                        createCapacityRecord(
                                restaurantId
                        )
                );
    }

    private KitchenCapacity createCapacityRecord(
            UUID restaurantId
    ) {

        KitchenCapacity capacity =
                KitchenCapacity.builder()
                        .restaurantId(restaurantId)
                        .version(0L)
                        .build();

        return kitchenCapacityDao.save(
                capacity
        );
    }

    /*
     * IMPORTANT:
     *
     * Do NOT put @Transactional here.
     *
     * This method must execute inside the transaction
     * of the caller.
     *
     * The caller will keep this pessimistic lock until
     * its transaction finishes.
     */
    public KitchenCapacity getLocked(
            UUID restaurantId
    ) {

        return kitchenCapacityDao
                .findByRestaurantIdForUpdate(
                        restaurantId
                )
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Kitchen capacity record not found for restaurant: "
                                        + restaurantId
                        )
                );
    }
}