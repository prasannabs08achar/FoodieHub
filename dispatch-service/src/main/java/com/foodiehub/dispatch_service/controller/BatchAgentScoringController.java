package com.foodiehub.dispatch_service.controller;

import com.foodiehub.dispatch_service.dto.BatchAgentScoreResponse;
import com.foodiehub.dispatch_service.dto.BatchPickupLocation;
import com.foodiehub.dispatch_service.service.BatchAgentScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/dispatch/batches/scoring")
@RequiredArgsConstructor
public class BatchAgentScoringController {

    private final BatchAgentScoringService batchAgentScoringService;

    /**
     * Returns all eligible agents ordered by score descending.
     */
    @GetMapping("/agents")
    public ResponseEntity<List<BatchAgentScoreResponse>> scoreAgents(
            @RequestParam BigDecimal pickupLatitude,
            @RequestParam BigDecimal pickupLongitude
    ) {

        return ResponseEntity.ok(
                batchAgentScoringService.scoreAgents(
                        new BatchPickupLocation(
                                pickupLatitude,
                                pickupLongitude
                        )
                )
        );
    }

    /**
     * Returns the highest-scored eligible agent.
     */
    @GetMapping("/best")
    public ResponseEntity<BatchAgentScoreResponse> scoreBestAgent(
            @RequestParam BigDecimal pickupLatitude,
            @RequestParam BigDecimal pickupLongitude
    ) {

        BatchAgentScoreResponse result =
                batchAgentScoringService.scoreBestAgent(
                        new BatchPickupLocation(
                                pickupLatitude,
                                pickupLongitude
                        )
                );

        if (result == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(result);
    }
}