package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.dao.BatchDao;
import com.foodiehub.dispatch_service.dao.BatchOrderDao;
import com.foodiehub.dispatch_service.dao.BatchingConfigDao;
import com.foodiehub.dispatch_service.dto.BatchEligibilityResponse;
import com.foodiehub.dispatch_service.dto.DispatchOrderResponse;
import com.foodiehub.dispatch_service.model.Batch;
import com.foodiehub.dispatch_service.model.BatchOrder;
import com.foodiehub.dispatch_service.model.BatchStatus;
import com.foodiehub.dispatch_service.model.BatchingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchEligibilityService {

    private static final long PREPARING_LOOKAHEAD_MINUTES = 5;

    private static final Set<BatchStatus> ACTIVE_BATCH_STATUSES = Set.of(
            BatchStatus.FORMING,
            BatchStatus.OFFERED,
            BatchStatus.ACCEPTED,
            BatchStatus.PICKED_UP
    );

    private final BatchDao batchDao;
    private final BatchOrderDao batchOrderDao;
    private final BatchingConfigDao batchingConfigDao;

    @Transactional(readOnly = true)
    public BatchEligibilityResponse checkEligibility(
            DispatchOrderResponse order
    ) {

        if (order == null) {
            return new BatchEligibilityResponse(
                    null,
                    false,
                    "Order is missing"
            );
        }

        if (!isCurrentlyBatchable(order)) {
            return new BatchEligibilityResponse(
                    order.id(),
                    false,
                    getNonBatchableStatusReason(order)
            );
        }

        if (isAlreadyInActiveBatch(order.id())) {
            return new BatchEligibilityResponse(
                    order.id(),
                    false,
                    "Order is already in an active batch"
            );
        }

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

            return new BatchEligibilityResponse(
                    order.id(),
                    false,
                    "Invalid maximum batch size configuration"
            );
        }

        return new BatchEligibilityResponse(
                order.id(),
                true,
                "Order is eligible for batching"
        );
    }

    private boolean isCurrentlyBatchable(
            DispatchOrderResponse order
    ) {

        if (order.status() == null) {
            return false;
        }

        if ("READY_FOR_PICKUP".equalsIgnoreCase(order.status())) {
            return true;
        }

        if ("PREPARING".equalsIgnoreCase(order.status())) {

            if (order.predictedReadyAt() == null) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();

            LocalDateTime lookaheadLimit =
                    now.plusMinutes(
                            PREPARING_LOOKAHEAD_MINUTES
                    );

            return !order.predictedReadyAt().isBefore(now)
                    && !order.predictedReadyAt().isAfter(lookaheadLimit);
        }

        return false;
    }

    private String getNonBatchableStatusReason(
            DispatchOrderResponse order
    ) {

        if (order.status() == null) {
            return "Order status is missing";
        }

        if ("PREPARING".equalsIgnoreCase(order.status())
                && order.predictedReadyAt() == null) {

            return "Preparing order has no predicted ready time";
        }

        if ("PREPARING".equalsIgnoreCase(order.status())) {
            return "Preparing order is not predicted to be ready within 5 minutes";
        }

        return "Order is not in a batchable state: "
                + order.status();
    }

    private boolean isAlreadyInActiveBatch(
            UUID orderId
    ) {

        List<BatchOrder> batchOrders =
                batchOrderDao.findByOrderId(orderId);

        if (batchOrders.isEmpty()) {
            return false;
        }

        List<UUID> batchIds =
                batchOrders.stream()
                        .map(BatchOrder::getBatchId)
                        .filter(id -> id != null)
                        .distinct()
                        .toList();

        if (batchIds.isEmpty()) {
            return false;
        }

        return batchDao
                .findByIdIn(batchIds)
                .stream()
                .anyMatch(batch ->
                        ACTIVE_BATCH_STATUSES.contains(
                                batch.getStatus()
                        )
                );
    }
}