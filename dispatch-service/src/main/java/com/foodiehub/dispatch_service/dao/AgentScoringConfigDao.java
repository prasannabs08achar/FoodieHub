package com.foodiehub.dispatch_service.dao;

import com.foodiehub.dispatch_service.model.AgentScoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentScoringConfigDao
        extends JpaRepository<AgentScoringConfig, UUID> {

    Optional<AgentScoringConfig> findFirstByOrderByUpdatedAtDesc();
}
