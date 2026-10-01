package com.foodiehub.dispatch_service.controller;

import com.foodiehub.dispatch_service.dto.BatchResponse;
import com.foodiehub.dispatch_service.service.BatchLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchLifecycleService batchLifecycleService;

    /*
     * =========================================================
     * GET BATCH
     * =========================================================
     */

    @GetMapping("/{batchId}")
    public ResponseEntity<BatchResponse> getBatch(
            @PathVariable UUID batchId
    ) {

        return ResponseEntity.ok(
                batchLifecycleService.getBatch(
                        batchId
                )
        );
    }

    /*
     * =========================================================
     * MARK OFFERED
     * =========================================================
     *
     * Agent scoring / selecting the agent is Step 7.
     * Offer workflow is Step 8.
     *
     * This endpoint only performs the lifecycle transition.
     */

    @PostMapping("/{batchId}/offer")
    public ResponseEntity<BatchResponse> markOffered(
            @PathVariable UUID batchId,
            @RequestParam UUID agentId,
            @RequestParam LocalDateTime expiresAt
    ) {

        return ResponseEntity.ok(
                batchLifecycleService.markOffered(
                        batchId,
                        agentId,
                        expiresAt
                )
        );
    }

    /*
     * =========================================================
     * ACCEPT
     * =========================================================
     */

    @PostMapping("/{batchId}/accept")
    public ResponseEntity<BatchResponse> acceptBatch(
            @PathVariable UUID batchId,
            @RequestHeader("X-User-Id") UUID agentId
    ) {

        return ResponseEntity.ok(
                batchLifecycleService.acceptBatch(
                        batchId,
                        agentId
                )
        );
    }

    /*
     * =========================================================
     * DISSOLVE
     * =========================================================
     */

    @PostMapping("/{batchId}/dissolve")
    public ResponseEntity<BatchResponse> dissolveBatch(
            @PathVariable UUID batchId
    ) {

        return ResponseEntity.ok(
                batchLifecycleService.dissolveBatch(
                        batchId
                )
        );
    }

    /*
     * =========================================================
     * PICKUP
     * =========================================================
     */

    @PostMapping("/{batchId}/pickup")
    public ResponseEntity<BatchResponse> pickupBatch(
            @PathVariable UUID batchId,
            @RequestHeader("X-User-Id") UUID agentId
    ) {

        return ResponseEntity.ok(
                batchLifecycleService.pickupBatch(
                        batchId,
                        agentId
                )
        );
    }

    /*
     * =========================================================
     * DELIVER ONE STOP
     * =========================================================
     */

    @PostMapping("/{batchId}/orders/{orderId}/deliver")
    public ResponseEntity<BatchResponse> deliverStop(
            @PathVariable UUID batchId,
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID agentId
    ) {

        return ResponseEntity.ok(
                batchLifecycleService.deliverStop(
                        batchId,
                        orderId,
                        agentId
                )
        );
    }

    /*
     * =========================================================
     * CANCEL
     * =========================================================
     */

    @PostMapping("/{batchId}/cancel")
    public ResponseEntity<BatchResponse> cancelBatch(
            @PathVariable UUID batchId
    ) {

        return ResponseEntity.ok(
                batchLifecycleService.cancelBatch(
                        batchId
                )
        );
    }
}