package com.foodiehub.dispatch_service.controller;

import com.foodiehub.dispatch_service.dto.AssignmentResponse;
import com.foodiehub.dispatch_service.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/orders/{orderId}")
    public ResponseEntity<AssignmentResponse> assignStaticAgent(
            @PathVariable UUID orderId
    ) {

        return ResponseEntity.ok(
                assignmentService.assignStaticAgent(orderId)
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