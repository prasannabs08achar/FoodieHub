package com.foodiehub.order_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "kitchen_capacity",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kitchen_capacity_restaurant",
                        columnNames = "restaurant_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KitchenCapacity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    /*
     * Used as the optimistic version field.
     *
     * The actual capacity synchronization is done
     * using the pessimistic lock in KitchenCapacityDao.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}