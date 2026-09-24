package com.foodiehub.order_service.dao;


import com.foodiehub.order_service.model.OrderStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStateHistoryDao
        extends JpaRepository<OrderStateHistory, UUID> {

    List<OrderStateHistory> findByOrderIdOrderByChangedAtAsc(
            UUID orderId
    );
}