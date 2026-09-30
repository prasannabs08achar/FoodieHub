package com.foodiehub.dispatch_service.dao;

import com.foodiehub.dispatch_service.model.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentDao extends JpaRepository<Agent, UUID> {

    Optional<Agent> findByUserId(UUID userId);
    List<Agent> findByOnlineTrue();
}