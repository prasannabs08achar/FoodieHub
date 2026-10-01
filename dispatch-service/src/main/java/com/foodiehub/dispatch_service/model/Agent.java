package com.foodiehub.dispatch_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "agent",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_agent_user_id",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private Boolean online;

    @Column(nullable = false)
    private Boolean active;

    /*
     * =========================================================
     * CURRENT AGENT LOCATION
     * =========================================================
     *
     * These fields represent the latest location received
     * through the heartbeat endpoint.
     */
    @Column(
            name = "current_latitude",
            precision = 10,
            scale = 7
    )
    private BigDecimal currentLatitude;

    @Column(
            name = "current_longitude",
            precision = 10,
            scale = 7
    )
    private BigDecimal currentLongitude;

    /*
     * =========================================================
     * LAST HEARTBEAT
     * =========================================================
     *
     * Used later by StaleAgentDetectionService.
     */
    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "assignment_cooldown_until")
    private Instant assignmentCooldownUntil;
    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (online == null) {
            online = false;
        }

        if (active == null) {
            active = true;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}