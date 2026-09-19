package com.foodiehub.wallet_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_key",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_key_operation",
                        columnNames = {"user_id", "idempotency_key", "operation"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String key;

    @Column(nullable = false, length = 50)
    private String operation;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        createdAt = Instant.now();
    }
}
