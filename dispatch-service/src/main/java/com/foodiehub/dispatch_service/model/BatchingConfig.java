package com.foodiehub.dispatch_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batching_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "max_batch_size", nullable = false)
    private Integer maxBatchSize;

    @Column(name = "batch_radius_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal batchRadiusKm;

    @Column(name = "max_detour_percentage", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxDetourPercentage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (maxBatchSize == null) {
            maxBatchSize = 4;
        }

        if (batchRadiusKm == null) {
            batchRadiusKm = BigDecimal.valueOf(1.5);
        }

        if (maxDetourPercentage == null) {
            maxDetourPercentage = BigDecimal.valueOf(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}