package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.dao.AgentScoringConfigDao;
import com.foodiehub.dispatch_service.dao.OrderAssignmentDao;
import com.foodiehub.dispatch_service.dto.BatchAgentScoreResponse;
import com.foodiehub.dispatch_service.dto.BatchPickupLocation;
import com.foodiehub.dispatch_service.model.Agent;
import com.foodiehub.dispatch_service.model.AgentScoringConfig;
import com.foodiehub.dispatch_service.model.AssignmentStatus;
import com.foodiehub.dispatch_service.model.OrderAssignment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchAgentScoringService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private static final BigDecimal ONE =
            BigDecimal.ONE;

    /*
     * The existing individual scoring implementation
     * uses 30 minutes as the idle-time normalization cap.
     *
     * We keep the same value here so individual and
     * batch scoring remain consistent.
     */
    private static final long IDLE_TIME_CAP_MINUTES = 30;

    private final AgentDao agentDao;

    private final OrderAssignmentDao orderAssignmentDao;

    private final AgentScoringConfigDao agentScoringConfigDao;

    /**
     * Scores all eligible agents for a batch pickup location.
     *
     * Results are sorted from highest score to lowest score.
     */
    @Transactional(readOnly = true)
    public List<BatchAgentScoreResponse> scoreAgents(
            BatchPickupLocation pickupLocation
    ) {

        validatePickupLocation(
                pickupLocation
        );

        AgentScoringConfig config =
                getScoringConfig();

        validateWeights(
                config
        );

        Instant now =
                Instant.now();

        return agentDao
                .findByOnlineTrue()
                .stream()
                .filter(agent ->
                        isEligibleAgent(
                                agent,
                                now
                        )
                )
                .map(agent ->
                        scoreAgent(
                                agent,
                                pickupLocation,
                                config,
                                now
                        )
                )
                .sorted(
                        Comparator
                                .comparing(
                                        BatchAgentScoreResponse::totalScore
                                )
                                .reversed()
                                .thenComparing(
                                        BatchAgentScoreResponse::agentId
                                )
                )
                .toList();
    }

    /**
     * Returns the highest-scored eligible agent.
     */
    @Transactional(readOnly = true)
    public BatchAgentScoreResponse scoreBestAgent(
            BatchPickupLocation pickupLocation
    ) {

        return scoreAgents(
                pickupLocation
        )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private BatchAgentScoreResponse scoreAgent(
            Agent agent,
            BatchPickupLocation pickupLocation,
            AgentScoringConfig config,
            Instant now
    ) {

        BigDecimal distanceKm =
                calculateDistance(
                        agent.getCurrentLatitude(),
                        agent.getCurrentLongitude(),
                        pickupLocation.latitude(),
                        pickupLocation.longitude()
                );

        BigDecimal distanceScore =
                calculateDistanceScore(
                        distanceKm
                );

        int activeDeliveryCount =
                countActiveDeliveries(
                        agent.getId()
                );

        BigDecimal loadScore =
                calculateLoadScore(
                        activeDeliveryCount
                );

        BigDecimal acceptanceRate =
                calculateAcceptanceRate(
                        agent.getId()
                );

        BigDecimal acceptanceScore =
                acceptanceRate;

        long idleMinutes =
                calculateIdleMinutes(
                        agent.getId(),
                        now
                );

        BigDecimal idleTimeScore =
                calculateIdleTimeScore(
                        idleMinutes
                );

        BigDecimal totalScore =
                distanceScore
                        .multiply(
                                config.getDistanceWeight()
                        )
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
                        .setScale(
                                6,
                                RoundingMode.HALF_UP
                        );

        return new BatchAgentScoreResponse(
                agent.getId(),
                agent.getUserId(),
                distanceScore,
                loadScore,
                acceptanceScore,
                idleTimeScore,
                totalScore,
                activeDeliveryCount,
                acceptanceRate,
                idleMinutes
        );
    }

    private boolean isEligibleAgent(
            Agent agent,
            Instant now
    ) {

        if (agent.getId() == null) {
            return false;
        }

        if (!Boolean.TRUE.equals(
                agent.getActive()
        )) {
            return false;
        }

        if (!Boolean.TRUE.equals(
                agent.getOnline()
        )) {
            return false;
        }

        /*
         * Agent must have a usable current heartbeat location.
         */
        if (agent.getCurrentLatitude() == null
                || agent.getCurrentLongitude() == null) {

            return false;
        }

        /*
         * Failed/stale agents remain excluded during cooldown.
         */
        if (agent.getAssignmentCooldownUntil() != null
                && now.isBefore(
                agent.getAssignmentCooldownUntil()
        )) {

            return false;
        }

        return true;
    }

    private BigDecimal calculateDistanceScore(
            BigDecimal distanceKm
    ) {

        /*
         * 0 km => 1.0
         *
         * Increasing distance produces a progressively
         * smaller score.
         */
        return ONE
                .divide(
                        ONE.add(distanceKm),
                        6,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateLoadScore(
            int activeDeliveryCount
    ) {

        return ONE
                .divide(
                        ONE.add(
                                BigDecimal.valueOf(
                                        activeDeliveryCount
                                )
                        ),
                        6,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateAcceptanceRate(
            UUID agentId
    ) {

        List<OrderAssignment> assignments =
                orderAssignmentDao.findByAgentId(
                        agentId
                );

        if (assignments.isEmpty()) {
            /*
             * Neutral score for an agent with no history.
             */
            return BigDecimal.valueOf(0.5);
        }

        long accepted =
                assignments.stream()
                        .filter(assignment ->
                                assignment.getStatus()
                                        == AssignmentStatus.ACCEPTED
                        )
                        .count();

        long declined =
                assignments.stream()
                        .filter(assignment ->
                                assignment.getStatus()
                                        == AssignmentStatus.DECLINED
                        )
                        .count();

        long expired =
                assignments.stream()
                        .filter(assignment ->
                                assignment.getStatus()
                                        == AssignmentStatus.EXPIRED
                        )
                        .count();

        long total =
                accepted
                        + declined
                        + expired;

        if (total == 0) {
            return BigDecimal.valueOf(0.5);
        }

        return BigDecimal
                .valueOf(accepted)
                .divide(
                        BigDecimal.valueOf(total),
                        6,
                        RoundingMode.HALF_UP
                );
    }

    private long calculateIdleMinutes(
            UUID agentId,
            Instant now
    ) {

        OrderAssignment latestAccepted =
                orderAssignmentDao
                        .findTopByAgentIdAndStatusOrderByOfferedAtDesc(
                                agentId,
                                AssignmentStatus.ACCEPTED
                        )
                        .orElse(null);

        /*
         * No previous accepted delivery:
         * treat the agent as fully idle.
         */
        if (latestAccepted == null) {
            return IDLE_TIME_CAP_MINUTES;
        }

        LocalDateTime referenceTime =
                latestAccepted.getCompletedAt() != null
                        ? latestAccepted.getCompletedAt()
                        : latestAccepted.getRespondedAt();

        if (referenceTime == null) {
            return 0;
        }

        Instant referenceInstant =
                referenceTime
                        .atZone(
                                java.time.ZoneId.systemDefault()
                        )
                        .toInstant();

        long minutes =
                Duration
                        .between(
                                referenceInstant,
                                now
                        )
                        .toMinutes();

        return Math.max(
                0,
                Math.min(
                        minutes,
                        IDLE_TIME_CAP_MINUTES
                )
        );
    }

    private BigDecimal calculateIdleTimeScore(
            long idleMinutes
    ) {

        return BigDecimal
                .valueOf(idleMinutes)
                .divide(
                        BigDecimal.valueOf(
                                IDLE_TIME_CAP_MINUTES
                        ),
                        6,
                        RoundingMode.HALF_UP
                );
    }

    private int countActiveDeliveries(
            UUID agentId
    ) {

        return (int)
                orderAssignmentDao
                        .countByAgentIdAndStatusAndCompletedAtIsNull(
                                agentId,
                                AssignmentStatus.ACCEPTED
                        );
    }

    private BigDecimal calculateDistance(
            BigDecimal latitude1,
            BigDecimal longitude1,
            BigDecimal latitude2,
            BigDecimal longitude2
    ) {

        double lat1 =
                Math.toRadians(
                        latitude1.doubleValue()
                );

        double lon1 =
                Math.toRadians(
                        longitude1.doubleValue()
                );

        double lat2 =
                Math.toRadians(
                        latitude2.doubleValue()
                );

        double lon2 =
                Math.toRadians(
                        longitude2.doubleValue()
                );

        double deltaLat =
                lat2 - lat1;

        double deltaLon =
                lon2 - lon1;

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
                .valueOf(
                        EARTH_RADIUS_KM * c
                )
                .setScale(
                        6,
                        RoundingMode.HALF_UP
                );
    }

    private AgentScoringConfig getScoringConfig() {

        return agentScoringConfigDao
                .findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Agent scoring configuration not found"
                        )
                );
    }

    private void validateWeights(
            AgentScoringConfig config
    ) {

        if (config.getDistanceWeight() == null
                || config.getLoadWeight() == null
                || config.getAcceptanceWeight() == null
                || config.getIdleTimeWeight() == null) {

            throw new IllegalStateException(
                    "Agent scoring weights cannot be null"
            );
        }

        if (config.getDistanceWeight()
                .compareTo(BigDecimal.ZERO) < 0
                || config.getLoadWeight()
                .compareTo(BigDecimal.ZERO) < 0
                || config.getAcceptanceWeight()
                .compareTo(BigDecimal.ZERO) < 0
                || config.getIdleTimeWeight()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalStateException(
                    "Agent scoring weights cannot be negative"
            );
        }

        BigDecimal total =
                config.getDistanceWeight()
                        .add(config.getLoadWeight())
                        .add(config.getAcceptanceWeight())
                        .add(config.getIdleTimeWeight());

        if (total.compareTo(
                BigDecimal.ONE
        ) != 0) {

            throw new IllegalStateException(
                    "Agent scoring weights must sum to 1.0"
            );
        }
    }

    private void validatePickupLocation(
            BatchPickupLocation pickupLocation
    ) {

        if (pickupLocation == null
                || pickupLocation.latitude() == null
                || pickupLocation.longitude() == null) {

            throw new IllegalArgumentException(
                    "Batch pickup coordinates are required"
            );
        }

        double latitude =
                pickupLocation.latitude()
                        .doubleValue();

        double longitude =
                pickupLocation.longitude()
                        .doubleValue();

        if (latitude < -90
                || latitude > 90) {

            throw new IllegalArgumentException(
                    "Invalid pickup latitude"
            );
        }

        if (longitude < -180
                || longitude > 180) {

            throw new IllegalArgumentException(
                    "Invalid pickup longitude"
            );
        }
    }
}