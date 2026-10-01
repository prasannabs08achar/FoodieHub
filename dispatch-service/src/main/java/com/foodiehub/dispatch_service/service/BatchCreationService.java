package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.dao.BatchDao;
import com.foodiehub.dispatch_service.dao.BatchOrderDao;
import com.foodiehub.dispatch_service.dao.BatchingConfigDao;
import com.foodiehub.dispatch_service.dto.DispatchOrderResponse;
import com.foodiehub.dispatch_service.model.Batch;
import com.foodiehub.dispatch_service.model.BatchOrder;
import com.foodiehub.dispatch_service.model.BatchStatus;
import com.foodiehub.dispatch_service.model.BatchingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchCreationService {

    private static final List<BatchStatus> ACTIVE_BATCH_STATUSES =
            List.of(
                    BatchStatus.FORMING,
                    BatchStatus.OFFERED,
                    BatchStatus.ACCEPTED,
                    BatchStatus.PICKED_UP
            );

    private final BatchDao batchDao;

    private final BatchOrderDao batchOrderDao;

    private final BatchingConfigDao batchingConfigDao;

    private final BatchRouteValidationService routeValidationService;

    @Transactional
    public Batch createBatch(
            List<DispatchOrderResponse> orders
    ) {

        validateOrders(orders);

        BatchingConfig config =
                batchingConfigDao
                        .findFirstByOrderByUpdatedAtDesc()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Batching configuration not found"
                                )
                        );

        if (config.getMaxBatchSize() == null
                || config.getMaxBatchSize() < 2) {

            throw new IllegalStateException(
                    "Invalid maximum batch size configuration"
            );
        }

        if (orders.size()
                > config.getMaxBatchSize()) {

            throw new IllegalArgumentException(
                    "Batch exceeds configured maximum size"
            );
        }

        UUID restaurantId =
                orders.get(0).restaurantId();

        boolean sameRestaurant =
                orders.stream()
                        .allMatch(order ->
                                restaurantId.equals(
                                        order.restaurantId()
                                )
                        );

        if (!sameRestaurant) {

            throw new IllegalArgumentException(
                    "All orders in a batch must belong to the same restaurant"
            );
        }

        /*
         * =====================================================
         * ACTIVE BATCH PROTECTION
         * =====================================================
         *
         * Lock existing BatchOrder rows belonging to each
         * candidate order before checking active batches.
         */

        validateOrdersNotInActiveBatch(
                orders
        );

        /*
         * =====================================================
         * ROUTE VALIDATION
         * =====================================================
         */

        if (!routeValidationService
                .areOrdersWithinBatchRadius(orders)) {

            throw new IllegalArgumentException(
                    "Orders are outside the configured batch radius"
            );
        }

        if (!routeValidationService
                .isDetourAcceptable(orders)) {

            throw new IllegalArgumentException(
                    "Batch route exceeds configured detour threshold"
            );
        }

        /*
         * =====================================================
         * CREATE BATCH
         * =====================================================
         */

        Batch batch =
                Batch.builder()
                        .restaurantId(
                                restaurantId
                        )
                        .status(
                                BatchStatus.FORMING
                        )
                        .build();

        Batch savedBatch =
                batchDao.save(batch);

        /*
         * =====================================================
         * CREATE BATCH STOPS
         * =====================================================
         */

        int sequence = 1;

        for (DispatchOrderResponse order :
                orders) {

            BatchOrder batchOrder =
                    BatchOrder.builder()
                            .batchId(
                                    savedBatch.getId()
                            )
                            .orderId(
                                    order.id()
                            )
                            .stopSequence(
                                    sequence++
                            )
                            .build();

            batchOrderDao.save(
                    batchOrder
            );
        }

        return savedBatch;
    }

    private void validateOrders(
            List<DispatchOrderResponse> orders
    ) {

        if (orders == null
                || orders.size() < 2) {

            throw new IllegalArgumentException(
                    "At least two orders are required to create a batch"
            );
        }

        if (orders.stream()
                .anyMatch(order ->
                        order == null)) {

            throw new IllegalArgumentException(
                    "Batch cannot contain null orders"
            );
        }

        long uniqueOrderCount =
                orders.stream()
                        .map(DispatchOrderResponse::id)
                        .distinct()
                        .count();

        if (uniqueOrderCount
                != orders.size()) {

            throw new IllegalArgumentException(
                    "A batch cannot contain the same order more than once"
            );
        }
    }

    private void validateOrdersNotInActiveBatch(
            List<DispatchOrderResponse> orders
    ) {

        for (DispatchOrderResponse order :
                orders) {

            List<BatchOrder> existingMemberships =
                    batchOrderDao
                            .findByOrderIdForUpdate(
                                    order.id()
                            );

            if (existingMemberships.isEmpty()) {
                continue;
            }

            List<UUID> batchIds =
                    existingMemberships.stream()
                            .map(BatchOrder::getBatchId)
                            .filter(id -> id != null)
                            .distinct()
                            .toList();

            if (batchIds.isEmpty()) {
                continue;
            }

            boolean alreadyActive =
                    batchDao
                            .findByIdIn(batchIds)
                            .stream()
                            .anyMatch(batch ->
                                    ACTIVE_BATCH_STATUSES
                                            .contains(
                                                    batch.getStatus()
                                            )
                            );

            if (alreadyActive) {

                throw new IllegalArgumentException(
                        "Order "
                                + order.id()
                                + " already belongs to an active batch"
                );
            }
        }
    }
}