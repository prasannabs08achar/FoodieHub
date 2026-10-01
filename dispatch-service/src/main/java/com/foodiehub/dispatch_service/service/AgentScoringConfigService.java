package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.dao.AgentScoringConfigDao;
import com.foodiehub.dispatch_service.dto.AgentScoringConfigRequest;
import com.foodiehub.dispatch_service.dto.AgentScoringConfigResponse;
import com.foodiehub.dispatch_service.model.AgentScoringConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AgentScoringConfigService {

    private static final BigDecimal REQUIRED_TOTAL = BigDecimal.ONE;

    private final AgentScoringConfigDao agentScoringConfigDao;

    @Transactional(readOnly = true)
    public AgentScoringConfigResponse getConfig() {

        AgentScoringConfig config =
                agentScoringConfigDao
                        .findFirstByOrderByUpdatedAtDesc()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Agent scoring configuration not found"
                                )
                        );

        return mapToResponse(config);
    }

    @Transactional
    public AgentScoringConfigResponse updateConfig(
            AgentScoringConfigRequest request
    ) {

        validateWeights(request);

        AgentScoringConfig config =
                agentScoringConfigDao
                        .findFirstByOrderByUpdatedAtDesc()
                        .orElseGet(AgentScoringConfig::new);

        config.setDistanceWeight(request.distanceWeight());
        config.setLoadWeight(request.loadWeight());
        config.setAcceptanceWeight(request.acceptanceWeight());
        config.setIdleTimeWeight(request.idleTimeWeight());

        config = agentScoringConfigDao.save(config);

        return mapToResponse(config);
    }

    private void validateWeights(
            AgentScoringConfigRequest request
    ) {

        if (request == null
                || request.distanceWeight() == null
                || request.loadWeight() == null
                || request.acceptanceWeight() == null
                || request.idleTimeWeight() == null) {

            throw new IllegalArgumentException(
                    "All agent scoring weights are required"
            );
        }

        if (request.distanceWeight()
                .compareTo(BigDecimal.ZERO) < 0
                || request.loadWeight()
                .compareTo(BigDecimal.ZERO) < 0
                || request.acceptanceWeight()
                .compareTo(BigDecimal.ZERO) < 0
                || request.idleTimeWeight()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Agent scoring weights cannot be negative"
            );
        }

        BigDecimal total =
                request.distanceWeight()
                        .add(request.loadWeight())
                        .add(request.acceptanceWeight())
                        .add(request.idleTimeWeight());

        if (total.compareTo(REQUIRED_TOTAL) != 0) {

            throw new IllegalArgumentException(
                    "Agent scoring weights must sum to 1.0"
            );
        }
    }
    private AgentScoringConfigResponse mapToResponse(
            AgentScoringConfig config
    ) {

        return new AgentScoringConfigResponse(
                config.getId(),
                config.getDistanceWeight(),
                config.getLoadWeight(),
                config.getAcceptanceWeight(),
                config.getIdleTimeWeight(),
                config.getUpdatedAt()
        );
    }
}