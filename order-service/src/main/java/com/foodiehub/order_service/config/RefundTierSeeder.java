package com.foodiehub.order_service.config;


import com.foodiehub.order_service.dao.RefundTierDao;
import com.foodiehub.order_service.model.OrderStatus;
import com.foodiehub.order_service.model.RefundActor;
import com.foodiehub.order_service.model.RefundTier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class RefundTierSeeder implements CommandLineRunner {

    private final RefundTierDao refundTierDao;

    @Override
    public void run(String... args) {

        seed(
                RefundActor.CUSTOMER,
                OrderStatus.PLACED,
                "100.00"
        );

        seed(
                RefundActor.CUSTOMER,
                OrderStatus.ACCEPTED,
                "100.00"
        );

        seed(
                RefundActor.CUSTOMER,
                OrderStatus.PREPARING,
                "50.00"
        );

        seed(
                RefundActor.CUSTOMER,
                OrderStatus.READY_FOR_PICKUP,
                "0.00"
        );

        seed(
                RefundActor.CUSTOMER,
                OrderStatus.PICKED_UP,
                "0.00"
        );

        seed(
                RefundActor.RESTAURANT,
                OrderStatus.PLACED,
                "100.00"
        );

        seed(
                RefundActor.RESTAURANT,
                OrderStatus.ACCEPTED,
                "100.00"
        );

        seed(
                RefundActor.RESTAURANT,
                OrderStatus.PREPARING,
                "100.00"
        );

        seed(
                RefundActor.SYSTEM,
                OrderStatus.READY_FOR_PICKUP,
                "100.00"
        );

        seed(
                RefundActor.SYSTEM,
                OrderStatus.PICKED_UP,
                "100.00"
        );
    }

    private void seed(
            RefundActor actor,
            OrderStatus orderStatus,
            String percentage
    ) {

        if (refundTierDao
                .findByActorAndOrderStatusAndActiveTrue(
                        actor,
                        orderStatus
                )
                .isPresent()) {

            return;
        }

        RefundTier refundTier = RefundTier.builder()
                .actor(actor)
                .orderStatus(orderStatus)
                .refundPercentage(new BigDecimal(percentage))
                .active(true)
                .build();

        refundTierDao.save(refundTier);
    }
}