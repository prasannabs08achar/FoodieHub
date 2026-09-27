package com.foodiehub.order_service.service;

import com.foodiehub.order_service.dao.RefundTierDao;
import com.foodiehub.order_service.exception.InvalidOrderStateTransitionException;
import com.foodiehub.order_service.model.OrderStatus;
import com.foodiehub.order_service.model.RefundActor;
import com.foodiehub.order_service.model.RefundTier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RefundTierService {

    private final RefundTierDao refundTierDao;

    @Transactional(readOnly = true)
    public RefundTier getRefundTier(
            RefundActor actor,
            OrderStatus orderStatus
    ) {

        return refundTierDao
                .findByActorAndOrderStatusAndActiveTrue(
                        actor,
                        orderStatus
                )
                .orElseThrow(() ->
                        new InvalidOrderStateTransitionException(
                                "No active refund tier configured for actor "
                                        + actor
                                        + " and order status "
                                        + orderStatus
                        )
                );
    }
    @Transactional
    public RefundTier updateRefundPercentage(
            RefundActor actor,
            OrderStatus orderStatus,
            BigDecimal refundPercentage
    ) {

        RefundTier refundTier =
                refundTierDao
                        .findByActorAndOrderStatusAndActiveTrue(
                                actor,
                                orderStatus
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Refund tier not found"
                                )
                        );

        refundTier.setRefundPercentage(refundPercentage);

        return refundTierDao.save(refundTier);
    }
}