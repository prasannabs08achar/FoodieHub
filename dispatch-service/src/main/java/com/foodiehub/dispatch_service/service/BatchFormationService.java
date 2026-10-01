package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.client.OrderClient;
import com.foodiehub.dispatch_service.dao.BatchDao;
import com.foodiehub.dispatch_service.dao.BatchOrderDao;
import com.foodiehub.dispatch_service.dao.BatchingConfigDao;
import com.foodiehub.dispatch_service.dto.DispatchOrderResponse;
import com.foodiehub.dispatch_service.model.Batch;
import com.foodiehub.dispatch_service.model.BatchingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchFormationService {

    private final OrderClient orderClient;

    private final BatchDao batchDao;

    private final BatchOrderDao batchOrderDao;

    private final BatchingConfigDao batchingConfigDao;

    private final BatchCreationService batchCreationService;

    private final BatchEligibilityService eligibilityService;

    private final BatchRouteValidationService routeValidationService;

    @Scheduled(
            fixedDelayString =
                    "${dispatch.batching.check-interval-ms:5000}"
    )
    public void detectBatches() {

        List<DispatchOrderResponse> readyOrders =
                orderClient.getReadyForPickupOrders();

        if (readyOrders == null
                || readyOrders.isEmpty()) {

            return;
        }

        readyOrders.stream()
                .filter(order ->
                        eligibilityService
                                .checkEligibility(order)
                                .eligible()
                )
                .map(DispatchOrderResponse::restaurantId)
                .distinct()
                .forEach(
                        this::formRestaurantBatch
                );
    }

    private void formRestaurantBatch(
            UUID restaurantId
    ) {

        List<DispatchOrderResponse> restaurantOrders =
                orderClient
                        .getReadyForPickupOrders()
                        .stream()
                        .filter(order ->
                                restaurantId.equals(
                                        order.restaurantId()
                                )
                        )
                        .filter(order ->
                                eligibilityService
                                        .checkEligibility(order)
                                        .eligible()
                        )
                        .toList();

        if (restaurantOrders.size() < 2) {
            return;
        }

        BatchingConfig config =
                batchingConfigDao
                        .findFirstByOrderByUpdatedAtDesc()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Batching configuration not found"
                                )
                        );

        int maxBatchSize =
                config.getMaxBatchSize();

        List<DispatchOrderResponse> candidateOrders =
                new ArrayList<>();

        for (DispatchOrderResponse order :
                restaurantOrders) {

            if (candidateOrders.size()
                    >= maxBatchSize) {

                break;
            }

            List<DispatchOrderResponse> test =
                    new ArrayList<>(
                            candidateOrders
                    );

            test.add(order);

            /*
             * First order is always accepted as the
             * anchor. Once we have two or more orders,
             * both radius and detour validation must pass.
             */
            if (test.size() == 1) {

                candidateOrders.add(order);

                continue;
            }

            boolean withinRadius =
                    routeValidationService
                            .areOrdersWithinBatchRadius(
                                    test
                            );

            if (!withinRadius) {
                continue;
            }

            boolean detourAcceptable =
                    routeValidationService
                            .isDetourAcceptable(
                                    test
                            );

            if (detourAcceptable) {
                candidateOrders.add(order);
            }
        }

        if (candidateOrders.size() < 2) {
            return;
        }

        try {

            Batch batch =
                    batchCreationService.createBatch(
                            candidateOrders
                    );

            log.info(
                    "Batch created. batchId={}, restaurantId={}, orderCount={}",
                    batch.getId(),
                    restaurantId,
                    candidateOrders.size()
            );

        } catch (Exception ex) {

            log.warn(
                    "Unable to create batch for restaurant {}: {}",
                    restaurantId,
                    ex.getMessage()
            );
        }
    }
}