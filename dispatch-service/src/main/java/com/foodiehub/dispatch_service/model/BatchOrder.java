package com.foodiehub.dispatch_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "batch_order",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_batch_order_batch_order",
                        columnNames = {
                                "batch_id",
                                "order_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_batch_order_batch_id",
                        columnList = "batch_id"
                ),
                @Index(
                        name = "idx_batch_order_order_id",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_batch_order_sequence",
                        columnList = "batch_id, stop_sequence"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}