package com.foodiehub.order_service.service;

import com.foodiehub.order_service.client.CatalogClient;
import com.foodiehub.order_service.dao.OrderDao;
import com.foodiehub.order_service.dao.OrderStateHistoryDao;
import com.foodiehub.order_service.model.KitchenQueue;
import com.foodiehub.order_service.model.Order;
import com.foodiehub.order_service.model.OrderStateHistory;
import com.foodiehub.order_service.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KitchenQueuePromotionService {


    private final KitchenQueueService kitchenQueueService;

    private final KitchenCapacityService kitchenCapacityService;

    private final OrderDao orderDao;

    private final OrderStateHistoryDao orderStateHistoryDao;

    private final CatalogClient catalogClient;


    /*
     * =========================================================
     * 11.15 - FIFO KITCHEN QUEUE PROMOTION
     * =========================================================
     *
     * Runs periodically and promotes queued ACCEPTED orders
     * to PREPARING whenever kitchen capacity becomes available.
     *
     * FIFO is maintained per restaurant.
     */
    @Scheduled(
            fixedDelayString =
                    "${order.kitchen-queue.promotion-interval-ms:5000}"
    )
    @Transactional
    public void promoteQueuedOrders() {

        List<UUID> restaurantIds =
                kitchenQueueService
                        .findRestaurantsWithQueuedOrders();

        for (UUID restaurantId : restaurantIds) {

            try {

                promoteOrdersForRestaurant(
                        restaurantId
                );

            } catch (RuntimeException exception) {

                /*
                 * Do not allow one restaurant to stop
                 * processing other restaurants.
                 */
                log.error(
                        "Failed to promote kitchen queue for restaurant {}",
                        restaurantId,
                        exception
                );
            }
        }
    }


    /*
     * =========================================================
     * PROCESS ONE RESTAURANT
     * =========================================================
     */
    private void promoteOrdersForRestaurant(
            UUID restaurantId
    ) {

        /*
         * -----------------------------------------------------
         * 1. Make sure synchronization row exists.
         * -----------------------------------------------------
         */
        kitchenCapacityService.ensureExists(
                restaurantId
        );


        /*
         * -----------------------------------------------------
         * 2. Lock the kitchen capacity row.
         *
         * Every capacity-sensitive ACCEPTED -> PREPARING
         * transition uses this same row.
         *
         * This prevents concurrent workers from exceeding
         * MaxConcurrentOrders.
         * -----------------------------------------------------
         */
        kitchenCapacityService.getLocked(
                restaurantId
        );


        /*
         * -----------------------------------------------------
         * 3. Get restaurant configuration.
         * -----------------------------------------------------
         */
        var restaurant =
                catalogClient.getRestaurant(
                        restaurantId
                );

        Integer maxConcurrentOrders =
                restaurant.maxConcurrentOrders();


        if (maxConcurrentOrders == null
                || maxConcurrentOrders < 1) {

            log.error(
                    "Invalid kitchen capacity {} for restaurant {}",
                    maxConcurrentOrders,
                    restaurantId
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * 4. Count currently PREPARING orders.
         * -----------------------------------------------------
         */
        int preparingOrders =
                orderDao
                        .findByRestaurantIdAndStatus(
                                restaurantId,
                                OrderStatus.PREPARING
                        )
                        .size();


        /*
         * -----------------------------------------------------
         * 5. Calculate available kitchen slots.
         * -----------------------------------------------------
         */
        int availableSlots =
                maxConcurrentOrders
                        - preparingOrders;


        if (availableSlots <= 0) {

            return;
        }


        /*
         * -----------------------------------------------------
         * 6. Get queued orders in FIFO order.
         * -----------------------------------------------------
         */
        List<KitchenQueue> queuedOrders =
                kitchenQueueService
                        .getQueuedOrders(
                                restaurantId
                        );


        if (queuedOrders.isEmpty()) {

            return;
        }


        /*
         * -----------------------------------------------------
         * 7. Promote orders until capacity is full.
         * -----------------------------------------------------
         */
        int promotedCount = 0;

        for (KitchenQueue queueEntry : queuedOrders) {

            if (promotedCount >= availableSlots) {
                break;
            }

            boolean promoted =
                    promoteSingleOrder(
                            queueEntry,
                            restaurantId
                    );

            if (promoted) {

                promotedCount++;
            }
        }


        if (promotedCount > 0) {

            log.info(
                    "Promoted {} queued orders to PREPARING for restaurant {}",
                    promotedCount,
                    restaurantId
            );
        }
    }


    /*
     * =========================================================
     * PROMOTE SINGLE ORDER
     * =========================================================
     */
    private boolean promoteSingleOrder(
            KitchenQueue queueEntry,
            UUID restaurantId
    ) {

        UUID orderId =
                queueEntry.getOrderId();


        /*
         * -----------------------------------------------------
         * Lock the order.
         * -----------------------------------------------------
         */
        Order order =
                orderDao
                        .findByIdForUpdate(
                                orderId
                        )
                        .orElse(null);


        /*
         * Order may have been cancelled/deleted after the
         * queue snapshot was created.
         */
        if (order == null) {

            kitchenQueueService.remove(
                    orderId
            );

            return false;
        }


        /*
         * -----------------------------------------------------
         * Make sure queue and order belong to same restaurant.
         * -----------------------------------------------------
         */
        if (!restaurantId.equals(
                order.getRestaurantId()
        )) {

            log.warn(
                    "Queue restaurant mismatch for order {}",
                    orderId
            );

            kitchenQueueService.remove(
                    orderId
            );

            return false;
        }


        /*
         * -----------------------------------------------------
         * Only ACCEPTED orders can be promoted.
         *
         * If the order was cancelled or otherwise changed
         * while waiting, simply remove the stale queue entry.
         * -----------------------------------------------------
         */
        if (order.getStatus() != OrderStatus.ACCEPTED) {

            kitchenQueueService.remove(
                    orderId
            );

            return false;
        }


        /*
         * -----------------------------------------------------
         * ACCEPTED -> PREPARING
         * -----------------------------------------------------
         */
        OrderStatus previousStatus =
                order.getStatus();

        order.setStatus(
                OrderStatus.PREPARING
        );

        orderDao.save(
                order
        );


        /*
         * -----------------------------------------------------
         * Record state transition.
         * -----------------------------------------------------
         */
        OrderStateHistory history =
                OrderStateHistory.builder()
                        .orderId(orderId)
                        .fromStatus(previousStatus)
                        .toStatus(OrderStatus.PREPARING)
                        .changedBy(null)
                        .reason(
                                "Automatically promoted from kitchen queue"
                        )
                        .build();

        orderStateHistoryDao.save(
                history
        );


        /*
         * -----------------------------------------------------
         * Remove from FIFO queue.
         * -----------------------------------------------------
         */
        kitchenQueueService.remove(
                orderId
        );


        log.info(
                "Kitchen queue order {} promoted to PREPARING for restaurant {}",
                orderId,
                restaurantId
        );

        return true;
    }
}