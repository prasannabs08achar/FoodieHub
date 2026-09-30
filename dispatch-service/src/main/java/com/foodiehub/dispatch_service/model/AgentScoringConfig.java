package com.foodiehub.dispatch_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_scoring_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentScoringConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "distance_weight",
            nullable = false,
            precision = 5,
            scale = 4
    )
    private BigDecimal distanceWeight;

    @Column(
            name = "load_weight",
            nullable = false,
            precision = 5,
            scale = 4
    )
    private BigDecimal loadWeight;

    @Column(
            name = "acceptance_weight",
            nullable = false,
            precision = 5,
            scale = 4
    )
    private BigDecimal acceptanceWeight;

    @Column(
            name = "idle_time_weight",
            nullable = false,
            precision = 5,
            scale = 4
    )
    private BigDecimal idleTimeWeight;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}