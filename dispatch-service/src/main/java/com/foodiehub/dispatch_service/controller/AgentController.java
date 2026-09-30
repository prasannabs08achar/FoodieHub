package com.foodiehub.dispatch_service.controller;

import com.foodiehub.dispatch_service.dto.AgentHeartbeatRequest;
import com.foodiehub.dispatch_service.dto.AgentProvisionRequest;
import com.foodiehub.dispatch_service.dto.AgentStatusResponse;
import com.foodiehub.dispatch_service.model.AgentLocationHistory;
import com.foodiehub.dispatch_service.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;


    /*
     * =========================================================
     * GO ONLINE
     * =========================================================
     */
    @PostMapping("/online")
    public ResponseEntity<AgentStatusResponse> goOnline(
            @RequestHeader("X-User-Id") UUID agentUserId
    ) {

        return ResponseEntity.ok(
                agentService.goOnline(
                        agentUserId
                )
        );
    }


    /*
     * =========================================================
     * GO OFFLINE
     * =========================================================
     */
    @PostMapping("/offline")
    public ResponseEntity<AgentStatusResponse> goOffline(
            @RequestHeader("X-User-Id") UUID agentUserId
    ) {

        return ResponseEntity.ok(
                agentService.goOffline(
                        agentUserId
                )
        );
    }


    /*
     * =========================================================
     * HEARTBEAT
     * =========================================================
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<AgentStatusResponse> heartbeat(
            @RequestHeader("X-User-Id") UUID agentUserId,
            @Valid @RequestBody AgentHeartbeatRequest request
    ) {

        return ResponseEntity.ok(
                agentService.heartbeat(
                        agentUserId,
                        request
                )
        );
    }


    /*
     * =========================================================
     * GET CURRENT STATUS
     * =========================================================
     */
    @GetMapping("/status")
    public ResponseEntity<AgentStatusResponse> getStatus(
            @RequestHeader("X-User-Id") UUID agentUserId
    ) {

        return ResponseEntity.ok(
                agentService.getStatus(
                        agentUserId
                )
        );
    }
    @GetMapping("/location-history")
    public ResponseEntity<List<AgentLocationHistory>> getLocationHistory(
            @RequestHeader("X-User-Id") UUID agentUserId
    ) {
        return ResponseEntity.ok(
                agentService.getLocationHistory(agentUserId)
        );
    }
    @PostMapping
    public ResponseEntity<AgentStatusResponse> provisionAgent(
            @Valid @RequestBody AgentProvisionRequest request
    ) {
        return ResponseEntity.ok(
                agentService.provisionAgent(request.userId())
        );
    }
}