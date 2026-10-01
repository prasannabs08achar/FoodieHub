package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.client.OrderClient;
import com.foodiehub.dispatch_service.dao.BatchDao;
import com.foodiehub.dispatch_service.dao.BatchOrderDao;
import com.foodiehub.dispatch_service.dao.OrderAssignmentDao;
import com.foodiehub.dispatch_service.dto.BatchOrderResponse;
import com.foodiehub.dispatch_service.dto.BatchResponse;
import com.foodiehub.dispatch_service.dto.OrderStatusUpdateRequest;
import com.foodiehub.dispatch_service.exception.BatchLifecycleException;
import com.foodiehub.dispatch_service.model.AssignmentStatus;
import com.foodiehub.dispatch_service.model.Batch;
import com.foodiehub.dispatch_service.model.BatchOrder;
import com.foodiehub.dispatch_service.model.BatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchLifecycleService {

    private final BatchDao batchDao;

    private final BatchOrderDao batchOrderDao;

    private final OrderClient orderClient;
    private final OrderAssignmentDao orderAssignmentDao;

    /*
     * =========================================================
     * GET BATCH
     * =========================================================
     */

    @Transactional(readOnly = true)
    public BatchResponse getBatch(
            UUID batchId
    ) {

        Batch batch =
                getBatchEntity(batchId);

        List<BatchOrder> batchOrders =
                batchOrderDao
                        .findByBatchIdOrderByStopSequenceAsc(
                                batchId
                        );

        return mapToResponse(
                batch,
                batchOrders
        );
    }

    /*
     * =========================================================
     * MARK OFFERED
     * =========================================================
     *
     * Step 8 will decide WHICH agent receives the offer.
     *
     * This method only changes the lifecycle state.
     */

    @Transactional
    public BatchResponse markOffered(
            UUID batchId,
            UUID agentId,
            LocalDateTime expiresAt
    ) {

        if (agentId == null) {
            throw new BatchLifecycleException(
                    "Agent id is required"
            );
        }

        if (expiresAt == null) {
            throw new BatchLifecycleException(
                    "Batch offer expiry time is required"
            );
        }

        Batch batch =
                getBatchForUpdate(batchId);

        requireStatus(
                batch,
                BatchStatus.FORMING
        );

        if (expiresAt.isBefore(
                LocalDateTime.now()
        )) {

            throw new BatchLifecycleException(
                    "Batch offer expiry must be in the future"
            );
        }

        batch.setAgentId(agentId);
        batch.setStatus(
                BatchStatus.OFFERED
        );
        batch.setOfferedAt(
                LocalDateTime.now()
        );
        batch.setExpiresAt(
                expiresAt
        );

        return saveAndMap(batch);
    }

    /*
     * =========================================================
     * ACCEPT
     * =========================================================
     */

    @Transactional
    public BatchResponse acceptBatch(
            UUID batchId,
            UUID agentId
    ) {

        Batch batch =
                getBatchForUpdate(batchId);

        requireStatus(
                batch,
                BatchStatus.OFFERED
        );

        validateAssignedAgent(
                batch,
                agentId
        );

        if (batch.getExpiresAt() == null
                || !batch.getExpiresAt()
                .isAfter(LocalDateTime.now())) {

            throw new BatchLifecycleException(
                    "Batch offer has expired"
            );
        }

        batch.setStatus(
                BatchStatus.ACCEPTED
        );

        batch.setAcceptedAt(
                LocalDateTime.now()
        );

        return saveAndMap(batch);
    }

    /*
     * =========================================================
     * DISSOLVE
     * =========================================================
     *
     * Used when a batch offer is declined.
     *
     * Step 10 will perform the actual re-offering of the
     * individual orders.
     */

    @Transactional
    public BatchResponse dissolveBatch(
            UUID batchId
    ) {

        Batch batch =
                getBatchForUpdate(batchId);

        if (batch.getStatus() != BatchStatus.FORMING
                && batch.getStatus() != BatchStatus.OFFERED
                && batch.getStatus() != BatchStatus.ACCEPTED) {

            throw new BatchLifecycleException(
                    "Batch cannot be dissolved from status "
                            + batch.getStatus()
            );
        }

        batch.setStatus(
                BatchStatus.DISSOLVED
        );

        batch.setAgentId(null);
        batch.setExpiresAt(null);

        return saveAndMap(batch);
    }

    /*
     * =========================================================
     * PICK UP ENTIRE BATCH
     * =========================================================
     *
     * The agent picks up the batch as a whole.
     */

    @Transactional
    public BatchResponse pickupBatch(
            UUID batchId,
            UUID agentId
    ) {

        Batch batch =
                getBatchForUpdate(batchId);

        requireStatus(
                batch,
                BatchStatus.ACCEPTED
        );

        validateAssignedAgent(
                batch,
                agentId
        );

        List<BatchOrder> batchOrders =
                batchOrderDao
                        .findByBatchIdForUpdate(
                                batchId
                        );

        if (batchOrders.size() < 2) {

            throw new BatchLifecycleException(
                    "A batch must contain at least two orders"
            );
        }

        for (BatchOrder batchOrder :
                batchOrders) {

            orderClient.updateStatus(
                    batchOrder.getOrderId(),
                    agentId,
                    new OrderStatusUpdateRequest(
                            "PICKED_UP",
                            "Batch picked up by assigned agent"
                    )
            );
        }

        batch.setStatus(
                BatchStatus.PICKED_UP
        );

        batch.setPickedUpAt(
                LocalDateTime.now()
        );

        return saveAndMap(
                batch,
                batchOrders
        );
    }

    /*
     * =========================================================
     * DELIVER ONE STOP
     * =========================================================
     *
     * Delivery is deliberately stop-by-stop.
     */

    @Transactional
    public BatchResponse deliverStop(
            UUID batchId,
            UUID orderId,
            UUID agentId
    ) {

        Batch batch =
                getBatchForUpdate(batchId);

        requireStatus(
                batch,
                BatchStatus.PICKED_UP
        );

        validateAssignedAgent(
                batch,
                agentId
        );

        BatchOrder batchOrder =
                batchOrderDao
                        .findByBatchIdAndOrderId(
                                batchId,
                                orderId
                        )
                        .orElseThrow(() ->
                                new BatchLifecycleException(
                                        "Order does not belong to this batch"
                                )
                        );

        if (batchOrder.getDeliveredAt() != null) {

            throw new BatchLifecycleException(
                    "Order has already been delivered"
            );
        }

        /*
         * Enforce stop-by-stop delivery.
         *
         * The next stop can only be delivered after all
         * previous stops have been delivered.
         */

        List<BatchOrder> batchOrders =
                batchOrderDao
                        .findByBatchIdForUpdate(
                                batchId
                        );

        for (BatchOrder previous :
                batchOrders) {

            if (previous.getStopSequence()
                    >= batchOrder.getStopSequence()) {

                break;
            }

            if (previous.getDeliveredAt() == null) {

                throw new BatchLifecycleException(
                        "Previous batch stop "
                                + previous.getStopSequence()
                                + " must be delivered first"
                );
            }
        }

        orderClient.updateStatus(
                orderId,
                agentId,
                new OrderStatusUpdateRequest(
                        "DELIVERED",
                        "Batch stop delivered"
                )
        );
        orderAssignmentDao
                .findByOrderIdAndStatus(
                        orderId,
                        AssignmentStatus.ACCEPTED
                )
                .ifPresent(assignment -> {

                    assignment.setCompletedAt(
                            LocalDateTime.now()
                    );

                    orderAssignmentDao.save(
                            assignment
                    );
                });

        batchOrder.setDeliveredAt(
                LocalDateTime.now()
        );

        batchOrderDao.save(
                batchOrder
        );
        boolean allDelivered =
                batchOrders.stream()
                        .allMatch(order ->
                                order.getDeliveredAt() != null
                        );

        if (allDelivered) {

            batch.setStatus(
                    BatchStatus.COMPLETED
            );

            batch.setCompletedAt(
                    LocalDateTime.now()
            );
        }

        return saveAndMap(
                batch,
                batchOrders
        );
    }

    /*
     * =========================================================
     * CANCEL BATCH
     * =========================================================
     *
     * This represents cancellation of the batch itself.
     *
     * Order-specific cancellation/refund behavior remains
     * owned by Order Service.
     */

    @Transactional
    public BatchResponse cancelBatch(
            UUID batchId
    ) {

        Batch batch =
                getBatchForUpdate(batchId);

        if (batch.getStatus()
                == BatchStatus.COMPLETED) {

            throw new BatchLifecycleException(
                    "Completed batch cannot be cancelled"
            );
        }

        if (batch.getStatus()
                == BatchStatus.CANCELLED) {

            throw new BatchLifecycleException(
                    "Batch is already cancelled"
            );
        }

        if (batch.getStatus()
                == BatchStatus.DISSOLVED) {

            throw new BatchLifecycleException(
                    "Dissolved batch cannot be cancelled"
            );
        }

        batch.setStatus(
                BatchStatus.CANCELLED
        );

        batch.setExpiresAt(null);

        return saveAndMap(batch);
    }

    /*
     * =========================================================
     * INTERNAL HELPERS
     * =========================================================
     */

    private Batch getBatchEntity(
            UUID batchId
    ) {

        return batchDao
                .findById(batchId)
                .orElseThrow(() ->
                        new BatchLifecycleException(
                                "Batch not found: "
                                        + batchId
                        )
                );
    }

    private Batch getBatchForUpdate(
            UUID batchId
    ) {

        return batchDao
                .findByIdForUpdate(batchId)
                .orElseThrow(() ->
                        new BatchLifecycleException(
                                "Batch not found: "
                                        + batchId
                        )
                );
    }

    private void requireStatus(
            Batch batch,
            BatchStatus expected
    ) {

        if (batch.getStatus() != expected) {

            throw new BatchLifecycleException(
                    "Invalid batch transition from "
                            + batch.getStatus()
                            + " to "
                            + expected
            );
        }
    }

    private void validateAssignedAgent(
            Batch batch,
            UUID agentId
    ) {

        if (agentId == null) {

            throw new BatchLifecycleException(
                    "Agent id is required"
            );
        }

        if (batch.getAgentId() == null) {

            throw new BatchLifecycleException(
                    "No agent is assigned to this batch"
            );
        }

        if (!batch.getAgentId()
                .equals(agentId)) {

            throw new BatchLifecycleException(
                    "Agent is not assigned to this batch"
            );
        }
    }

    private BatchResponse saveAndMap(
            Batch batch
    ) {

        Batch saved =
                batchDao.save(batch);

        List<BatchOrder> orders =
                batchOrderDao
                        .findByBatchIdOrderByStopSequenceAsc(
                                saved.getId()
                        );

        return mapToResponse(
                saved,
                orders
        );
    }

    private BatchResponse saveAndMap(
            Batch batch,
            List<BatchOrder> orders
    ) {

        Batch saved =
                batchDao.save(batch);

        return mapToResponse(
                saved,
                orders
        );
    }

    private BatchResponse mapToResponse(
            Batch batch,
            List<BatchOrder> orders
    ) {

        List<BatchOrderResponse> orderResponses =
                orders.stream()
                        .map(order ->
                                new BatchOrderResponse(
                                        order.getId(),
                                        order.getOrderId(),
                                        order.getStopSequence(),
                                        order.getDeliveredAt()
                                )
                        )
                        .toList();

        return new BatchResponse(
                batch.getId(),
                batch.getRestaurantId(),
                batch.getAgentId(),
                batch.getStatus(),
                batch.getOfferedAt(),
                batch.getExpiresAt(),
                batch.getAcceptedAt(),
                batch.getPickedUpAt(),
                batch.getCompletedAt(),
                batch.getCreatedAt(),
                batch.getUpdatedAt(),
                orderResponses
        );
    }
}