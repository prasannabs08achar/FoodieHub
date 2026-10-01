package com.foodiehub.dispatch_service.controller;

import com.foodiehub.dispatch_service.dto.AssignmentDecisionRequest;
import com.foodiehub.dispatch_service.dto.AssignmentResponse;
import com.foodiehub.dispatch_service.service.AgentAssignmentService;
import com.foodiehub.dispatch_service.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AgentAssignmentService agentAssignmentService;
    private final AssignmentService assignmentService;

    /*
     * Manual trigger.
     *
     * Useful for Postman testing and debugging.
     */
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<AssignmentResponse> assignOrder(
            @PathVariable UUID orderId
    ) {

        return ResponseEntity.ok(
                agentAssignmentService.assignOrder(
                        orderId
                )
        );
    }

    @PostMapping("/orders/{orderId}/accept")
    public ResponseEntity<AssignmentResponse> acceptOffer(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID agentUserId
    ) {

        return ResponseEntity.ok(
                agentAssignmentService.acceptOffer(
                        orderId,
                        agentUserId
                )
        );
    }

    @PostMapping("/orders/{orderId}/decline")
    public ResponseEntity<AssignmentResponse> declineOffer(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID agentUserId,
            @RequestBody(required = false)
            AssignmentDecisionRequest request
    ) {

        String reason =
                request == null
                        ? "Offer declined"
                        : request.reason();

        return ResponseEntity.ok(
                agentAssignmentService.declineOffer(
                        orderId,
                        agentUserId,
                        reason
                )
        );
    }

    @PostMapping("/orders/{orderId}/pickup")
    public ResponseEntity<Void> pickupOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID agentUserId
    ) {

        assignmentService.pickupOrder(
                orderId,
                agentUserId
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{orderId}/deliver")
    public ResponseEntity<Void> deliverOrder(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID agentUserId
    ) {

        assignmentService.deliverOrder(
                orderId,
                agentUserId
        );

        return ResponseEntity.ok().build();
    }
}