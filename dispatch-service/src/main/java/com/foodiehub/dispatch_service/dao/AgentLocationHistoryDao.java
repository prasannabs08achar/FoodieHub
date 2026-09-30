package com.foodiehub.dispatch_service.dao;

import com.foodiehub.dispatch_service.model.AgentLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentLocationHistoryDao
        extends JpaRepository<AgentLocationHistory, UUID> {

    List<AgentLocationHistory> findByAgentIdOrderByRecordedAtDesc(
            UUID agentId
    );
}