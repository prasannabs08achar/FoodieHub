package com.foodiehub.dispatch_service.dao;


import com.foodiehub.dispatch_service.model.BatchingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BatchingConfigDao extends JpaRepository<BatchingConfig, UUID> {

    Optional<BatchingConfig> findFirstByOrderByUpdatedAtDesc();
}