package com.foodiehub.dispatch_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "order_assignment_lock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderAssignmentLock {

    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
}