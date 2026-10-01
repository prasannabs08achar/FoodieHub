package com.foodiehub.dispatch_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "order_assignment",
        indexes = {
                @Index(
                        name = "idx_order_assignment_order_id",
                        columnList = "order_id"
                ),
                @Index(
                        name = "idx_order_assignment_agent_id",
                        columnList = "agent_id"
                ),
                @Index(
                        name = "idx_order_assignment_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "offered_at", nullable = false)
    private LocalDateTime offeredAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /*
     * Represents completion of the delivery handled by
     * this accepted assignment.
     *
     * AssignmentStatus remains ACCEPTED because the
     * assignment history itself is still an accepted offer.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (offeredAt == null) {
            offeredAt = now;
        }

        if (attemptNumber == null) {
            attemptNumber = 1;
        }
    }
}