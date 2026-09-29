package com.foodiehub.order_service.service;


import com.foodiehub.order_service.dao.OrderDao;
import com.foodiehub.order_service.model.Order;
import com.foodiehub.order_service.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutService {

    private final OrderDao orderDao;
    private final OrderService orderService;

    @Value("${order.timeout.minutes:10}")
    private long timeoutMinutes;


    @Scheduled(
            fixedDelayString = "${order.timeout.check-interval-ms:60000}"
    )
    public void cancelTimedOutOrders() {

        Instant cutoffTime =
                Instant.now()
                        .minusSeconds(timeoutMinutes * 60);

        List<Order> timedOutOrders =
                orderDao.findByStatusAndUpdatedAtBefore(
                        OrderStatus.READY_FOR_PICKUP,
                        cutoffTime
                );

        if (timedOutOrders.isEmpty()) {
            return;
        }

        log.info(
                "Found {} timed-out orders",
                timedOutOrders.size()
        );

        for (Order order : timedOutOrders) {

            try {

                orderService.systemCancelOrder(
                        order.getId(),
                        "Order was not picked up within the timeout window"
                );

                log.info(
                        "Order {} cancelled due to pickup timeout",
                        order.getId()
                );

            } catch (Exception exception) {

                log.error(
                        "Failed to cancel timed-out order {}",
                        order.getId(),
                        exception
                );
            }
        }
    }
}