package com.foodiehub.order_service.service;

import com.foodiehub.order_service.dao.OrderDao;
import com.foodiehub.order_service.dao.OrderItemDao;
import com.foodiehub.order_service.dao.OrderStateHistoryDao;
import com.foodiehub.order_service.dto.OrderItemResponse;
import com.foodiehub.order_service.dto.OrderResponse;
import com.foodiehub.order_service.dto.OrderStateHistoryResponse;
import com.foodiehub.order_service.dto.UpdateOrderStatusRequest;
import com.foodiehub.order_service.exception.OrderNotFoundException;
import com.foodiehub.order_service.model.Order;
import com.foodiehub.order_service.model.OrderItem;
import com.foodiehub.order_service.model.OrderStateHistory;
import com.foodiehub.order_service.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final OrderStateHistoryDao orderStateHistoryDao;
    private final OrderStateTransitionValidator transitionValidator;

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {

        Order order = getOrderEntity(orderId);

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(
            UUID customerId
    ) {

        return orderDao
                .findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getRestaurantOrders(
            UUID restaurantId,
            OrderStatus status
    ) {

        return orderDao
                .findByRestaurantIdAndStatus(
                        restaurantId,
                        status
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(
            UUID orderId,
            UUID changedBy,
            UpdateOrderStatusRequest request
    ) {

        Order order = getOrderEntity(orderId);

        OrderStatus currentStatus = order.getStatus();

        OrderStatus newStatus = request.status();

        transitionValidator.validate(
                currentStatus,
                newStatus
        );

        order.setStatus(newStatus);

        orderDao.save(order);

        OrderStateHistory history =
                OrderStateHistory.builder()
                        .orderId(order.getId())
                        .fromStatus(currentStatus)
                        .toStatus(newStatus)
                        .changedBy(changedBy)
                        .reason(request.reason())
                        .build();

        orderStateHistoryDao.save(history);

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStateHistoryResponse> getStateHistory(
            UUID orderId
    ) {

        // Make sure the order exists.
        getOrderEntity(orderId);

        return orderStateHistoryDao
                .findByOrderIdOrderByChangedAtAsc(orderId)
                .stream()
                .map(this::mapHistoryToResponse)
                .toList();
    }

    private Order getOrderEntity(UUID orderId) {

        return orderDao
                .findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found: " + orderId
                        )
                );
    }

    private OrderResponse mapToResponse(Order order) {

        List<OrderItemResponse> items =
                orderItemDao
                        .findByOrderId(order.getId())
                        .stream()
                        .map(this::mapItemToResponse)
                        .toList();

        List<OrderStateHistoryResponse> history =
                orderStateHistoryDao
                        .findByOrderIdOrderByChangedAtAsc(
                                order.getId()
                        )
                        .stream()
                        .map(this::mapHistoryToResponse)
                        .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude(),
                order.getTotalAmount(),
                order.getStatus(),
                items,
                history,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponse mapItemToResponse(
            OrderItem item
    ) {

        return new OrderItemResponse(
                item.getId(),
                item.getMenuItemId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }

    private OrderStateHistoryResponse mapHistoryToResponse(
            OrderStateHistory history
    ) {

        return new OrderStateHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getChangedAt(),
                history.getReason()
        );
    }
}