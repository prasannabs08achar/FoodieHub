package com.foodiehub.dispatch_service.service;


import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.dao.AgentScoringConfigDao;
import com.foodiehub.dispatch_service.dao.OrderAssignmentDao;
import com.foodiehub.dispatch_service.dto.AgentScoreResponse;
import com.foodiehub.dispatch_service.model.Agent;
import com.foodiehub.dispatch_service.model.AssignmentStatus;
import com.foodiehub.dispatch_service.model.OrderAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentScoringService {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private static final double EARTH_RADIUS_KM = 6371.0;

    private static final long IDLE_TIME_CAP_MINUTES = 30;

    private final AgentDao agentDao;
    private final OrderAssignmentDao orderAssignmentDao;
    private final AgentScoringConfigDao scoringConfigDao;

    @Transactional(readOnly = true)
    public List<AgentScoreResponse> scoreEligibleAgents(
            BigDecimal pickupLatitude,
            BigDecimal pickupLongitude
    ) {

        var config =
                scoringConfigDao
                        .findFirstByOrderByUpdatedAtDesc()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Agent scoring configuration not found"
                                )
                        );

        List<Agent> agents = agentDao.findByOnlineTrue();

        List<AgentScoreResponse> results = new ArrayList<>();

        for (Agent agent : agents) {

            if (!Boolean.TRUE.equals(agent.getActive())) {
                continue;
            }

            if (agent.getCurrentLatitude() == null
                    || agent.getCurrentLongitude() == null) {
                continue;
            }

            BigDecimal distanceKm = calculateDistance(
                    pickupLatitude,
                    pickupLongitude,
                    agent.getCurrentLatitude(),
                    agent.getCurrentLongitude()
            );

            BigDecimal distanceScore =
                    calculateDistanceScore(distanceKm);

            BigDecimal loadScore =
                    calculateLoadScore(agent);

            BigDecimal acceptanceScore =
                    calculateAcceptanceScore(agent);

            BigDecimal idleTimeScore =
                    calculateIdleTimeScore(agent);

            BigDecimal finalScore =
                    distanceScore
                            .multiply(config.getDistanceWeight())
                            .add(
                                    loadScore.multiply(
                                            config.getLoadWeight()
                                    )
                            )
                            .add(
                                    acceptanceScore.multiply(
                                            config.getAcceptanceWeight()
                                    )
                            )
                            .add(
                                    idleTimeScore.multiply(
                                            config.getIdleTimeWeight()
                                    )
                            )
                            .setScale(4, RoundingMode.HALF_UP);

            results.add(
                    new AgentScoreResponse(
                            agent.getId(),
                            agent.getUserId(),
                            distanceKm,
                            distanceScore,
                            loadScore,
                            acceptanceScore,
                            idleTimeScore,
                            finalScore
                    )
            );
        }

        results.sort(
                Comparator.comparing(
                        AgentScoreResponse::finalScore
                ).reversed()
        );

        return results;
    }

    private BigDecimal calculateDistanceScore(
            BigDecimal distanceKm
    ) {

        /*
         * Closer agents receive a higher score.
         * 0 km -> 1.0
         * Increasing distance -> progressively lower score.
         */
        return ONE
                .divide(
                        ONE.add(distanceKm),
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateLoadScore(
            Agent agent
    ) {

        long activeAssignments =
                orderAssignmentDao
                        .findByAgentId(agent.getId())
                        .stream()
                        .filter(assignment ->
                                assignment.getStatus()
                                        == AssignmentStatus.ACCEPTED
                        )
                        .count();

        return ONE
                .divide(
                        ONE.add(
                                BigDecimal.valueOf(
                                        activeAssignments
                                )
                        ),
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateAcceptanceScore(
            Agent agent
    ) {

        List<OrderAssignment> assignments =
                orderAssignmentDao.findByAgentId(
                        agent.getId()
                );

        if (assignments.isEmpty()) {
            return ONE;
        }

        long respondedOffers =
                assignments.stream()
                        .filter(assignment ->
                                assignment.getStatus()
                                        == AssignmentStatus.ACCEPTED
                                        || assignment.getStatus()
                                        == AssignmentStatus.DECLINED
                        )
                        .count();

        if (respondedOffers == 0) {
            return ONE;
        }

        long acceptedOffers =
                assignments.stream()
                        .filter(assignment ->
                                assignment.getStatus()
                                        == AssignmentStatus.ACCEPTED
                        )
                        .count();

        return BigDecimal.valueOf(acceptedOffers)
                .divide(
                        BigDecimal.valueOf(respondedOffers),
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateIdleTimeScore(
            Agent agent
    ) {

        Instant lastHeartbeat =
                agent.getLastHeartbeatAt();

        if (lastHeartbeat == null) {
            return BigDecimal.ZERO;
        }

        long idleMinutes =
                Duration.between(
                        lastHeartbeat,
                        Instant.now()
                ).toMinutes();

        if (idleMinutes <= 0) {
            return BigDecimal.ZERO;
        }

        if (idleMinutes >= IDLE_TIME_CAP_MINUTES) {
            return ONE;
        }

        return BigDecimal.valueOf(idleMinutes)
                .divide(
                        BigDecimal.valueOf(
                                IDLE_TIME_CAP_MINUTES
                        ),
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateDistance(
            BigDecimal latitude1,
            BigDecimal longitude1,
            BigDecimal latitude2,
            BigDecimal longitude2
    ) {

        double lat1 = Math.toRadians(latitude1.doubleValue());
        double lon1 = Math.toRadians(longitude1.doubleValue());

        double lat2 = Math.toRadians(latitude2.doubleValue());
        double lon2 = Math.toRadians(longitude2.doubleValue());

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a =
                Math.sin(deltaLat / 2)
                        * Math.sin(deltaLat / 2)
                        + Math.cos(lat1)
                        * Math.cos(lat2)
                        * Math.sin(deltaLon / 2)
                        * Math.sin(deltaLon / 2);

        double c =
                2 * Math.atan2(
                        Math.sqrt(a),
                        Math.sqrt(1 - a)
                );

        return BigDecimal
                .valueOf(EARTH_RADIUS_KM * c)
                .setScale(4, RoundingMode.HALF_UP);
    }
}