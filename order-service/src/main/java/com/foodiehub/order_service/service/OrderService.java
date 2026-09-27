package com.foodiehub.order_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodiehub.order_service.client.CatalogClient;
import com.foodiehub.order_service.client.WalletClient;
import com.foodiehub.order_service.dao.*;
import com.foodiehub.order_service.dto.*;
import com.foodiehub.order_service.exception.CartNotFoundException;
import com.foodiehub.order_service.exception.OrderNotFoundException;
import com.foodiehub.order_service.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final RefundTierService refundTierService;
    private static final String ORDER_PLACEMENT =
            "ORDER_PLACEMENT";

    private static final String STOCK_DECREMENT_PREFIX =
            "ORDER-STOCK-";

    private static final String WALLET_DEBIT_PREFIX =
            "ORDER-WALLET-";

    private static final String STOCK_RESTORE_PREFIX =
            "ORDER-RESTORE-";

    private static final String WALLET_REFUND_PREFIX =
            "ORDER-REFUND-";

    private final CartDao cartDao;
    private final CartItemDao cartItemDao;

    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final OrderStateHistoryDao orderStateHistoryDao;

    private final OrderIdempotencyRecordDao orderIdempotencyRecordDao;

    private final CatalogClient catalogClient;
    private final WalletClient walletClient;

    private final OrderStateTransitionValidator transitionValidator;

    private final ObjectMapper objectMapper;


    // =========================================================
    // 10.3 - 10.10 PLACE ORDER
    // =========================================================

    @Transactional
    public OrderResponse placeOrder(
            UUID customerId,
            PlaceOrderRequest request,
            String idempotencyKey
    ) {

        validateIdempotencyKey(idempotencyKey);

        /*
         * -----------------------------------------------------
         * 10.2 - ORDER IDEMPOTENCY
         * -----------------------------------------------------
         */

        String requestHash =
                generateRequestHash(
                        customerId,
                        request
                );

        var existingRecord =
                orderIdempotencyRecordDao
                        .findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {

            OrderIdempotencyRecord record = getOrderIdempotencyRecord(customerId, existingRecord, requestHash);

            return mapToResponse(
                    getOrderEntity(record.getOrderId())
            );
        }


        /*
         * -----------------------------------------------------
         * 10.3 - LOAD ACTIVE CART
         * -----------------------------------------------------
         */

        Cart cart =
                cartDao
                        .findByCustomerIdAndRestaurantId(
                                customerId,
                                request.restaurantId()
                        )
                        .orElseThrow(() ->
                                new CartNotFoundException(
                                        "Active cart not found for customer and restaurant"
                                )
                        );

        List<CartItem> cartItems =
                cartItemDao.findByCartId(cart.getId());

        if (cartItems.isEmpty()) {

            throw new IllegalArgumentException(
                    "Cannot place order with an empty cart"
            );
        }


        /*
         * -----------------------------------------------------
         * 10.4 - RESTAURANT VALIDATION
         * -----------------------------------------------------
         */

        CatalogRestaurantResponse restaurant =
                catalogClient.getRestaurant(
                        request.restaurantId()
                );

        if (!Boolean.TRUE.equals(restaurant.open())) {

            throw new IllegalArgumentException(
                    "Restaurant is currently closed"
            );
        }

        /*
         * Make sure the cart itself belongs to the restaurant
         * requested for this order.
         */
        if (!cart.getRestaurantId()
                .equals(request.restaurantId())) {

            throw new IllegalArgumentException(
                    "Cart does not belong to the requested restaurant"
            );
        }


        /*
         * -----------------------------------------------------
         * 10.5 - REVALIDATE CATALOG ITEMS + PRICES
         * -----------------------------------------------------
         */

        List<CatalogMenuItemResponse> catalogItems =
                new ArrayList<>();

        BigDecimal totalAmount =
                BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {

            CatalogMenuItemResponse menuItem =
                    catalogClient.getMenuItem(
                            cartItem.getMenuItemId()
                    );

            /*
             * Item must belong to the same restaurant.
             */
            if (!menuItem.restaurantId()
                    .equals(request.restaurantId())) {

                throw new IllegalArgumentException(
                        "Menu item "
                                + cartItem.getMenuItemId()
                                + " does not belong to this restaurant"
                );
            }

            /*
             * Item must be active.
             */
            if (!Boolean.TRUE.equals(menuItem.active())) {

                throw new IllegalArgumentException(
                        "Menu item is inactive: "
                                + menuItem.name()
                );
            }

            /*
             * The actual stock + time-window check will be
             * performed atomically by Catalog during
             * stock decrement in Step 10.7.
             *
             * Here we revalidate the current price.
             */
            cartItem.setUnitPrice(
                    menuItem.price()
            );

            cartItemDao.save(cartItem);

            BigDecimal lineTotal =
                    menuItem.price()
                            .multiply(
                                    BigDecimal.valueOf(
                                            cartItem.getQuantity()
                                    )
                            );

            totalAmount =
                    totalAmount.add(lineTotal);

            catalogItems.add(menuItem);
        }


        /*
         * -----------------------------------------------------
         * 10.6 - WALLET DEBIT
         * -----------------------------------------------------
         */

        DebitWalletResponse debitResponse;

        try {

            DebitWalletRequest debitRequest =
                    new DebitWalletRequest(
                            totalAmount,
                            idempotencyKey
                    );

            debitResponse =
                    walletClient.debitWallet(
                            customerId,
                            WALLET_DEBIT_PREFIX + idempotencyKey,
                            debitRequest
                    );

        } catch (RuntimeException walletException) {

            throw walletException;
        }


        /*
         * Keep track of successful stock operations.
         * If something fails later, we restore them.
         */
        List<CatalogMenuItemResponse> decrementedItems =
                new ArrayList<>();


        try {

            /*
             * -------------------------------------------------
             * 10.7 - STOCK DECREMENT
             * -------------------------------------------------
             */

            for (CatalogMenuItemResponse menuItem :
                    catalogItems) {

                CartItem cartItem =
                        cartItems.stream()
                                .filter(item ->
                                        item.getMenuItemId()
                                                .equals(
                                                        menuItem.id()
                                                )
                                )
                                .findFirst()
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "Cart item not found"
                                        )
                                );

                DecrementStockRequest stockRequest =
                        new DecrementStockRequest(
                                cartItem.getQuantity()
                        );

                catalogClient.decrementStock(
                        menuItem.id(),
                        STOCK_DECREMENT_PREFIX
                                + idempotencyKey
                                + "-"
                                + menuItem.id(),
                        stockRequest
                );

                decrementedItems.add(menuItem);
            }


            /*
             * -------------------------------------------------
             * 10.8 - CREATE ORDER
             * -------------------------------------------------
             */

            Order order =
                    Order.builder()
                            .customerId(customerId)
                            .restaurantId(
                                    request.restaurantId()
                            )
                            .deliveryLatitude(
                                    request.deliveryLatitude()
                            )
                            .deliveryLongitude(
                                    request.deliveryLongitude()
                            )
                            .totalAmount(totalAmount)
                            .status(OrderStatus.PLACED)
                            .build();

            order =
                    orderDao.save(order);


            /*
             * Create OrderItems using the server-side
             * Catalog prices.
             */
            for (CartItem cartItem : cartItems) {

                BigDecimal lineTotal =
                        cartItem.getUnitPrice()
                                .multiply(
                                        BigDecimal.valueOf(
                                                cartItem.getQuantity()
                                        )
                                );

                OrderItem orderItem =
                        OrderItem.builder()
                                .orderId(order.getId())
                                .menuItemId(
                                        cartItem.getMenuItemId()
                                )
                                .quantity(
                                        cartItem.getQuantity()
                                )
                                .unitPrice(
                                        cartItem.getUnitPrice()
                                )
                                .totalPrice(lineTotal)
                                .build();

                orderItemDao.save(orderItem);
            }


            /*
             * Create initial state history.
             */
            OrderStateHistory history =
                    OrderStateHistory.builder()
                            .orderId(order.getId())
                            .fromStatus(null)
                            .toStatus(OrderStatus.PLACED)
                            .changedBy(customerId)
                            .reason("Order placed")
                            .build();

            orderStateHistoryDao.save(history);


            /*
             * -------------------------------------------------
             * 10.9 - CONSUME CART
             * -------------------------------------------------
             */

            cartItemDao.deleteByCartId(
                    cart.getId()
            );


            /*
             * -------------------------------------------------
             * Save Order Idempotency Record
             * -------------------------------------------------
             */

            OrderIdempotencyRecord idempotencyRecord =
                    OrderIdempotencyRecord.builder()
                            .idempotencyKey(idempotencyKey)
                            .customerId(customerId)
                            .requestHash(requestHash)
                            .orderId(order.getId())
                            .build();

            orderIdempotencyRecordDao.save(
                    idempotencyRecord
            );


            return mapToResponse(order);

        } catch (RuntimeException placementException) {

            /*
             * -------------------------------------------------
             * 10.10 - COMPENSATION
             * -------------------------------------------------
             *
             * At this point wallet debit already succeeded.
             *
             * Therefore:
             *
             * 1. Restore successfully decremented stock.
             * 2. Refund wallet.
             */

            compensateStock(
                    decrementedItems,
                    cartItems,
                    idempotencyKey
            );

            compensateWallet(
                    customerId,
                    totalAmount,
                    idempotencyKey
            );

            throw placementException;
        }
    }

    private static OrderIdempotencyRecord getOrderIdempotencyRecord(UUID customerId, Optional<OrderIdempotencyRecord> existingRecord, String requestHash) {
        OrderIdempotencyRecord record =
                existingRecord.get();

        if (!record.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "Idempotency key belongs to another customer"
            );
        }

        if (!record.getRequestHash().equals(requestHash)) {
            throw new IllegalArgumentException(
                    "Idempotency key cannot be reused with a different request"
            );
        }
        return record;
    }


    // =========================================================
    // 10.10 - STOCK COMPENSATION
    // =========================================================

    private void compensateStock(
            List<CatalogMenuItemResponse> decrementedItems,
            List<CartItem> cartItems,
            String originalIdempotencyKey
    ) {

        for (CatalogMenuItemResponse menuItem :
                decrementedItems) {

            CartItem cartItem =
                    cartItems.stream()
                            .filter(item ->
                                    item.getMenuItemId()
                                            .equals(menuItem.id())
                            )
                            .findFirst()
                            .orElse(null);

            if (cartItem == null) {
                continue;
            }

            try {

                catalogClient.restoreStock(
                        menuItem.id(),

                        STOCK_RESTORE_PREFIX
                                + originalIdempotencyKey
                                + "-"
                                + menuItem.id(),

                        new RestoreStockRequest(
                                cartItem.getQuantity()
                        )
                );

            } catch (RuntimeException compensationException) {

                /*
                 * Do not hide the original placement failure.
                 *
                 * In production this should additionally be
                 * logged/alerted because compensation failed.
                 */
            }
        }
    }


    // =========================================================
    // 10.10 - WALLET COMPENSATION
    // =========================================================

    private void compensateWallet(
            UUID customerId,
            BigDecimal amount,
            String originalIdempotencyKey
    ) {

        try {

            walletClient.refundWallet(
                    customerId,

                    WALLET_REFUND_PREFIX
                            + originalIdempotencyKey,

                    new RefundWalletRequest(
                            amount,
                            originalIdempotencyKey
                    )
            );

        } catch (RuntimeException compensationException) {

            /*
             * Do not hide the original placement failure.
             *
             * In production this should be logged/alerted
             * because the wallet compensation failed.
             */
        }
    }


    // =========================================================
    // IDEMPOTENCY HELPERS
    // =========================================================

    private void validateIdempotencyKey(
            String idempotencyKey
    ) {

        if (idempotencyKey == null ||
                idempotencyKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Idempotency-Key header is required"
            );
        }

        if (idempotencyKey.length() > 100) {

            throw new IllegalArgumentException(
                    "Idempotency-Key must not exceed 100 characters"
            );
        }
    }


    private String generateRequestHash(
            UUID customerId,
            PlaceOrderRequest request
    ) {

        try {

            String json =
                    objectMapper.writeValueAsString(
                            new OrderRequestHashData(
                                    customerId,
                                    request
                            )
                    );

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            json.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            for (byte b : hash) {

                result.append(
                        String.format(
                                "%02x",
                                b
                        )
                );
            }

            return result.toString();

        } catch (
                NoSuchAlgorithmException |
                JsonProcessingException e
        ) {

            throw new RuntimeException(
                    "Unable to generate order request hash",
                    e
            );
        }
    }


    private record OrderRequestHashData(
            UUID customerId,
            PlaceOrderRequest request
    ) {
    }


    // =========================================================
    // EXISTING ORDER METHODS
    // =========================================================

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

        OrderStatus currentStatus =
                order.getStatus();

        OrderStatus newStatus =
                request.status();

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


    private OrderResponse mapToResponse(
            Order order
    ) {

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
    @Transactional
    public OrderResponse cancelOrder(
            UUID orderId,
            UUID customerId,
            String reason
    ) {

        Order order = getOrderEntity(orderId);

        // 1. Customer can cancel only their own order
        if (!order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "Customer is not allowed to cancel this order"
            );
        }

        OrderStatus currentStatus = order.getStatus();

        // 2. Delivered and already cancelled are terminal
        if (currentStatus == OrderStatus.DELIVERED ||
                currentStatus == OrderStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Order cannot be cancelled in status: "
                            + currentStatus
            );
        }

        // 3. Get configured refund tier
        RefundTier refundTier =
                refundTierService.getRefundTier(
                        RefundActor.CUSTOMER,
                        currentStatus
                );

        // 4. Calculate refund
        BigDecimal refundAmount =
                order.getTotalAmount()
                        .multiply(
                                refundTier.getRefundPercentage()
                                        .divide(
                                                BigDecimal.valueOf(100)
                                        )
                        );

        // 5. Refund wallet if refund > 0
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {

            walletClient.refundWallet(
                    customerId,

                    WALLET_REFUND_PREFIX
                            + "CANCEL-"
                            + orderId,

                    new RefundWalletRequest(
                            refundAmount,
                            orderId.toString()
                    )
            );
        }

        // 6. Restore stock
        restoreOrderStock(order, orderId);

        // 7. Change order state
        order.setStatus(OrderStatus.CANCELLED);

        orderDao.save(order);

        // 8. Save state history
        OrderStateHistory history =
                OrderStateHistory.builder()
                        .orderId(order.getId())
                        .fromStatus(currentStatus)
                        .toStatus(OrderStatus.CANCELLED)
                        .changedBy(customerId)
                        .reason(reason)
                        .build();

        orderStateHistoryDao.save(history);

        return mapToResponse(order);
    }
    private void restoreOrderStock(
            Order order,
            UUID orderId
    ) {

        List<OrderItem> orderItems =
                orderItemDao.findByOrderId(order.getId());

        for (OrderItem orderItem : orderItems) {

            catalogClient.restoreStock(
                    orderItem.getMenuItemId(),

                    STOCK_RESTORE_PREFIX
                            + "CANCEL-"
                            + orderId
                            + "-"
                            + orderItem.getMenuItemId(),

                    new RestoreStockRequest(
                            orderItem.getQuantity()
                    )
            );
        }
    }
    @Transactional
    public OrderResponse restaurantCancelOrder(
            UUID orderId,
            UUID restaurantOwnerId,
            String reason
    ) {

        Order order = getOrderEntity(orderId);

        // 1. Get restaurant details from Catalog Service
        CatalogRestaurantResponse restaurant =
                catalogClient.getRestaurant(
                        order.getRestaurantId()
                );

        // 2. Verify that the caller owns this restaurant
        if (!restaurant.ownerId().equals(restaurantOwnerId)) {
            throw new IllegalArgumentException(
                    "Restaurant owner is not allowed to cancel this order"
            );
        }

        OrderStatus currentStatus = order.getStatus();

        // 3. Restaurant can cancel only in these states
        if (currentStatus != OrderStatus.PLACED &&
                currentStatus != OrderStatus.ACCEPTED &&
                currentStatus != OrderStatus.PREPARING) {

            throw new IllegalArgumentException(
                    "Restaurant cannot cancel order in status: "
                            + currentStatus
            );
        }

        // 4. Reason is mandatory for restaurant cancellation
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required"
            );
        }

        // 5. Get configured restaurant refund tier
        RefundTier refundTier =
                refundTierService.getRefundTier(
                        RefundActor.RESTAURANT,
                        currentStatus
                );

        // 6. Calculate refund
        BigDecimal refundAmount =
                order.getTotalAmount()
                        .multiply(
                                refundTier.getRefundPercentage()
                                        .divide(BigDecimal.valueOf(100))
                        );

        // 7. Refund customer wallet
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {

            walletClient.refundWallet(
                    order.getCustomerId(),

                    WALLET_REFUND_PREFIX
                            + "RESTAURANT-CANCEL-"
                            + orderId,

                    new RefundWalletRequest(
                            refundAmount,
                            orderId.toString()
                    )
            );
        }

        // 8. Restore stock
        restoreOrderStock(order, orderId);

        // 9. Change order status
        order.setStatus(OrderStatus.CANCELLED);

        orderDao.save(order);

        // 10. Save state history
        OrderStateHistory history =
                OrderStateHistory.builder()
                        .orderId(order.getId())
                        .fromStatus(currentStatus)
                        .toStatus(OrderStatus.CANCELLED)
                        .changedBy(restaurantOwnerId)
                        .reason(reason)
                        .build();

        orderStateHistoryDao.save(history);

        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse systemCancelOrder(
            UUID orderId,
            String reason
    ) {

        Order order = getOrderEntity(orderId);

        OrderStatus currentStatus = order.getStatus();

        // 1. System cancellation is not allowed for terminal states
        if (currentStatus == OrderStatus.DELIVERED ||
                currentStatus == OrderStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "Order cannot be cancelled in status: "
                            + currentStatus
            );
        }

        // 2. Get the configured SYSTEM refund tier
        RefundTier refundTier =
                refundTierService.getRefundTier(
                        RefundActor.SYSTEM,
                        currentStatus
                );

        // 3. Calculate refund
        BigDecimal refundAmount =
                order.getTotalAmount()
                        .multiply(
                                refundTier.getRefundPercentage()
                                        .divide(BigDecimal.valueOf(100))
                        );

        // 4. Refund the customer
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {

            walletClient.refundWallet(
                    order.getCustomerId(),

                    WALLET_REFUND_PREFIX
                            + "SYSTEM-CANCEL-"
                            + orderId,

                    new RefundWalletRequest(
                            refundAmount,
                            orderId.toString()
                    )
            );
        }

        // 5. Restore stock
        restoreOrderStock(order, orderId);

        // 6. Change order status
        order.setStatus(OrderStatus.CANCELLED);

        orderDao.save(order);

        // 7. Save state history
        OrderStateHistory history =
                OrderStateHistory.builder()
                        .orderId(order.getId())
                        .fromStatus(currentStatus)
                        .toStatus(OrderStatus.CANCELLED)
                        .changedBy(null)
                        .reason(reason)
                        .build();

        orderStateHistoryDao.save(history);

        return mapToResponse(order);
    }
}