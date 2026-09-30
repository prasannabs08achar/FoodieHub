package com.foodiehub.dispatch_service.service;

import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.dao.AgentLocationHistoryDao;
import com.foodiehub.dispatch_service.dto.AgentHeartbeatRequest;
import com.foodiehub.dispatch_service.dto.AgentStatusResponse;
import com.foodiehub.dispatch_service.exception.AgentOfflineException;
import com.foodiehub.dispatch_service.model.Agent;
import com.foodiehub.dispatch_service.model.AgentLocationHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentDao agentDao;

    private final AgentLocationHistoryDao agentLocationHistoryDao;

    /*
     * =========================================================
     * GO ONLINE
     * =========================================================
     */
    @Transactional
    public AgentStatusResponse goOnline(
            UUID agentUserId
    ) {

        Agent agent =
                getAgentByUserId(
                        agentUserId
                );

        /*
         * Inactive agents cannot become available.
         */
        if (!Boolean.TRUE.equals(
                agent.getActive()
        )) {

            throw new IllegalArgumentException(
                    "Inactive agent cannot go online"
            );
        }

        agent.setOnline(true);

        agent =
                agentDao.save(
                        agent
                );

        return mapToResponse(
                agent
        );
    }


    /*
     * =========================================================
     * GO OFFLINE
     * =========================================================
     */
    @Transactional
    public AgentStatusResponse goOffline(
            UUID agentUserId
    ) {

        Agent agent =
                getAgentByUserId(
                        agentUserId
                );

        agent.setOnline(false);

        agent =
                agentDao.save(
                        agent
                );

        return mapToResponse(
                agent
        );
    }


    /*
     * =========================================================
     * HEARTBEAT
     * =========================================================
     *
     * Agent must be ONLINE before sending a heartbeat.
     */
    @Transactional
    public AgentStatusResponse heartbeat(
            UUID agentUserId,
            AgentHeartbeatRequest request
    ) {
        Agent agent = getAgentByUserId(agentUserId);

        if (!Boolean.TRUE.equals(agent.getOnline())) {
            throw new AgentOfflineException(
                    "Heartbeat is allowed only when the agent is online"
            );
        }

        Instant heartbeatTime = Instant.now();

        agent.setCurrentLatitude(request.latitude());
        agent.setCurrentLongitude(request.longitude());
        agent.setLastHeartbeatAt(heartbeatTime);

        agentDao.save(agent);

        AgentLocationHistory history = AgentLocationHistory.builder()
                .agentId(agent.getId())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .recordedAt(heartbeatTime)
                .build();

        agentLocationHistoryDao.save(history);

        return mapToResponse(agent);
    }

    @Transactional(readOnly = true)
    public List<AgentLocationHistory> getLocationHistory(
            UUID agentUserId
    ) {
        Agent agent = getAgentByUserId(agentUserId);

        return agentLocationHistoryDao
                .findByAgentIdOrderByRecordedAtDesc(agent.getId());
    }

    /*
     * =========================================================
     * GET STATUS
     * =========================================================
     */
    @Transactional(readOnly = true)
    public AgentStatusResponse getStatus(
            UUID agentUserId
    ) {

        Agent agent =
                getAgentByUserId(
                        agentUserId
                );

        return mapToResponse(
                agent
        );
    }


    /*
     * =========================================================
     * FIND AGENT BY USER ID
     * =========================================================
     */
    private Agent getAgentByUserId(
            UUID agentUserId
    ) {

        return agentDao
                .findByUserId(
                        agentUserId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Agent not found: "
                                        + agentUserId
                        )
                );
    }



    /*
     * =========================================================
     * MAP RESPONSE
     * =========================================================
     */
    private AgentStatusResponse mapToResponse(
            Agent agent
    ) {

        return new AgentStatusResponse(
                agent.getId(),
                agent.getUserId(),
                agent.getOnline(),
                agent.getActive(),
                agent.getCurrentLatitude(),
                agent.getCurrentLongitude(),
                agent.getLastHeartbeatAt()
        );
    }
}