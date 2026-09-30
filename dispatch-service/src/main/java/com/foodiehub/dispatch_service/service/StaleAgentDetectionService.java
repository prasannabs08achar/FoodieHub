package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.model.Agent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaleAgentDetectionService {

    private final AgentDao agentDao;

    private static final long STALE_THRESHOLD_SECONDS = 120;

    @Scheduled(
            fixedDelayString = "${dispatch.stale-agent.check-interval-ms:30000}"
    )
    @Transactional
    public void detectStaleAgents() {

        Instant now = Instant.now();

        List<Agent> onlineAgents = agentDao.findByOnlineTrue();

        for (Agent agent : onlineAgents) {

            Instant lastHeartbeat = agent.getLastHeartbeatAt();

            /*
             * An online agent without any heartbeat is considered stale.
             */
            if (lastHeartbeat == null) {
                markOffline(agent);
                continue;
            }

            long secondsSinceHeartbeat =
                    Duration.between(lastHeartbeat, now).getSeconds();

            if (secondsSinceHeartbeat >= STALE_THRESHOLD_SECONDS) {
                markOffline(agent);
            }
        }
    }

    private void markOffline(Agent agent) {

        agent.setOnline(false);

        log.info(
                "Agent marked offline because heartbeat is stale. " +
                        "agentId={}, userId={}, lastHeartbeatAt={}",
                agent.getId(),
                agent.getUserId(),
                agent.getLastHeartbeatAt()
        );
    }
}