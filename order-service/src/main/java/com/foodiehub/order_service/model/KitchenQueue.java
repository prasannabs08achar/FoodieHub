package com.foodiehub.order_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "kitchen_queue",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kitchen_queue_order",
                        columnNames = "order_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitchenQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @PrePersist
    protected void onCreate() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (queuedAt == null) {
            queuedAt = Instant.now();
        }
    }
}