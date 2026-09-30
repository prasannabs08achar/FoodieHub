package com.foodiehub.dispatch_service.config;

import com.foodiehub.dispatch_service.dao.AgentScoringConfigDao;
import com.foodiehub.dispatch_service.model.AgentScoringConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AgentScoringConfigSeeder implements CommandLineRunner {

    private final AgentScoringConfigDao agentScoringConfigDao;

    @Override
    public void run(String... args) {

        if (agentScoringConfigDao.count() > 0) {
            return;
        }

        AgentScoringConfig config =
                AgentScoringConfig.builder()
                        .distanceWeight(new BigDecimal("0.5"))
                        .loadWeight(new BigDecimal("0.2"))
                        .acceptanceWeight(new BigDecimal("0.2"))
                        .idleTimeWeight(new BigDecimal("0.1"))
                        .build();

        agentScoringConfigDao.save(config);
    }
}