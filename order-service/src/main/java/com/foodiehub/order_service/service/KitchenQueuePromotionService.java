package com.foodiehub.order_service.service;

import com.foodiehub.order_service.client.CatalogClient;
import com.foodiehub.order_service.dao.KitchenQueueDao;
import com.foodiehub.order_service.dao.OrderDao;
import com.foodiehub.order_service.dao.OrderStateHistoryDao;
import com.foodiehub.order_service.dto.CatalogRestaurantResponse;
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

    private final KitchenQueueDao kitchenQueueDao;
    private final OrderDao orderDao;
    private final OrderStateHistoryDao orderStateHistoryDao;

    private final KitchenCapacityService kitchenCapacityService;
    private final CatalogClient catalogClient;


    /*
     * =========================================================
     * 11.10.5 - AUTOMATIC FIFO PROMOTION
     * =========================================================
     *
     * Runs periodically and checks queued orders.
     */
    @Scheduled(
            fixedDelayString =
                    "${kitchen.queue.promotion-interval-ms:5000}"
    )
    public void promoteQueuedOrders() {

        /*
         * Get queue entries in FIFO order.
         */
        List<KitchenQueue> queuedOrders =
                kitchenQueueDao
                        .findAllByOrderByQueuedAtAsc();

        for (KitchenQueue queueEntry :
                queuedOrders) {

            try {

                promoteOrderIfCapacityAvailable(
                        queueEntry
                );

            } catch (Exception exception) {

                /*
                 * One failed order should not stop the
                 * promotion worker from processing the
                 * remaining queue entries.
                 */
                log.error(
                        "Failed to process queued order {}",
                        queueEntry.getOrderId(),
                        exception
                );
            }
        }
    }


    /*
     * =========================================================
     * PROCESS ONE QUEUED ORDER
     * =========================================================
     */
    @Transactional
    public void promoteOrderIfCapacityAvailable(
            KitchenQueue queueEntry
    ) {

        UUID orderId =
                queueEntry.getOrderId();

        UUID restaurantId =
                queueEntry.getRestaurantId();


        /*
         * -----------------------------------------------------
         * 1. LOCK KITCHEN CAPACITY
         * -----------------------------------------------------
         *
         * This is the synchronization point.
         *
         * Because this method is @Transactional and
         * getLocked() does NOT start its own transaction,
         * the pessimistic lock remains held until this
         * method finishes.
         */
        kitchenCapacityService.ensureExists(
                restaurantId
        );

        kitchenCapacityService.getLocked(
                restaurantId
        );


        /*
         * -----------------------------------------------------
         * 2. LOCK THE ORDER
         * -----------------------------------------------------
         */
        Order order =
                orderDao
                        .findByIdForUpdate(
                                orderId
                        )
                        .orElse(null);


        /*
         * The order no longer exists.
         *
         * Remove the stale queue entry.
         */
        if (order == null) {

            kitchenQueueDao.deleteByOrderId(
                    orderId
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * 3. VERIFY ORDER IS STILL ACCEPTED
         * -----------------------------------------------------
         *
         * The order may have been cancelled while waiting.
         *
         * Example:
         *
         * ACCEPTED
         *    ↓
         * CANCELLED
         *
         * In that case it must not be promoted.
         */
        if (order.getStatus()
                != OrderStatus.ACCEPTED) {

            kitchenQueueDao.deleteByOrderId(
                    orderId
            );

            return;
        }


        /*
         * -----------------------------------------------------
         * 4. GET RESTAURANT CAPACITY
         * -----------------------------------------------------
         */
        CatalogRestaurantResponse restaurant =
                catalogClient.getRestaurant(
                        restaurantId
                );

        Integer maxConcurrentOrders =
                restaurant.maxConcurrentOrders();


        if (maxConcurrentOrders == null
                || maxConcurrentOrders < 1) {

            throw new IllegalStateException(
                    "Invalid kitchen capacity configured for restaurant: "
                            + restaurantId
            );
        }


        /*
         * -----------------------------------------------------
         * 5. COUNT CURRENT PREPARING ORDERS
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
         * 6. KITCHEN STILL FULL
         * -----------------------------------------------------
         *
         * Leave the order in the queue.
         */
        if (preparingOrders
                >= maxConcurrentOrders) {

            return;
        }


        /*
         * -----------------------------------------------------
         * 7. PROMOTE ORDER
         * -----------------------------------------------------
         *
         * ACCEPTED → PREPARING
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
         * 8. CREATE STATE HISTORY
         * -----------------------------------------------------
         */
        OrderStateHistory history =
                OrderStateHistory.builder()
                        .orderId(
                                order.getId()
                        )
                        .fromStatus(
                                previousStatus
                        )
                        .toStatus(
                                OrderStatus.PREPARING
                        )
                        .changedBy(
                                null
                        )
                        .reason(
                                "Automatically promoted from kitchen queue"
                        )
                        .build();

        orderStateHistoryDao.save(
                history
        );


        /*
         * -----------------------------------------------------
         * 9. REMOVE FROM QUEUE
         * -----------------------------------------------------
         *
         * Only remove after successfully changing
         * the order to PREPARING.
         */
        kitchenQueueDao.deleteByOrderId(
                orderId
        );


        log.info(
                "Queued order {} promoted to PREPARING for restaurant {}",
                orderId,
                restaurantId
        );
    }
}