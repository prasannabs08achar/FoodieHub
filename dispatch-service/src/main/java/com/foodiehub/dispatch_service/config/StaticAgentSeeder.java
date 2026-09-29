package com.foodiehub.dispatch_service.config;

import com.foodiehub.dispatch_service.dao.AgentDao;
import com.foodiehub.dispatch_service.model.Agent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StaticAgentSeeder implements CommandLineRunner {

    private final AgentDao agentDao;

    @Value("${dispatch.static-agent.user-id}")
    private UUID staticAgentUserId;

    @Override
    public void run(String... args) {

        if (agentDao.findByUserId(staticAgentUserId).isPresent()) {
            return;
        }

        Agent agent = Agent.builder()
                .userId(staticAgentUserId)
                .online(true)
                .active(true)
                .build();

        agentDao.save(agent);

        System.out.println(
                "Static agent created successfully. User ID: "
                        + staticAgentUserId
        );
    }
}