package com.foodiehub.order_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "order_idempotency_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_order_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderIdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true
    )
    private String idempotencyKey;

    @Column(
            name = "customer_id",
            nullable = false
    )
    private UUID customerId;

    @Column(
            name = "request_hash",
            nullable = false
    )
    private String requestHash;

    @Column(
            name = "order_id",
            nullable = false
    )
    private UUID orderId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}