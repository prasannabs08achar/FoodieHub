package com.foodiehub.order_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "refund_tier",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refund_tier_actor_status",
                        columnNames = {"actor", "order_status"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundActor actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(
            name = "refund_percentage",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal refundPercentage;

    @Column(nullable = false)
    private Boolean active;
}