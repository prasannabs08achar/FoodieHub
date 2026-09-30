package com.foodiehub.dispatch_service.controller;


import com.foodiehub.dispatch_service.dto.AgentScoreResponse;
import com.foodiehub.dispatch_service.dto.AgentScoringConfigRequest;
import com.foodiehub.dispatch_service.dto.AgentScoringConfigResponse;
import com.foodiehub.dispatch_service.service.AgentScoringConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.foodiehub.dispatch_service.service.AgentScoringService;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/dispatch/scoring-config")
@RequiredArgsConstructor
public class AgentScoringConfigController {

    private final AgentScoringConfigService agentScoringConfigService;

    private final AgentScoringService agentScoringService;
    @GetMapping
    public ResponseEntity<AgentScoringConfigResponse> getConfig() {

        return ResponseEntity.ok(
                agentScoringConfigService.getConfig()
        );
    }

    @PutMapping
    public ResponseEntity<AgentScoringConfigResponse> updateConfig(
            @Valid @RequestBody AgentScoringConfigRequest request
    ) {

        return ResponseEntity.ok(
                agentScoringConfigService.updateConfig(request)
        );
    }
    @GetMapping("/agents")
    public ResponseEntity<List<AgentScoreResponse>> scoreAgents(
            @RequestParam BigDecimal pickupLatitude,
            @RequestParam BigDecimal pickupLongitude
    ) {
        return ResponseEntity.ok(
                agentScoringService.scoreEligibleAgents(
                        pickupLatitude,
                        pickupLongitude
                )
        );
    }
}