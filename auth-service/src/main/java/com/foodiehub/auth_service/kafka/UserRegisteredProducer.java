package com.foodiehub.auth_service.kafka;

import com.foodiehub.auth_service.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
public class UserRegisteredProducer {
    private final KafkaTemplate<String,UserRegisteredEvent>kafkaTemplate;


    public void publish(UserRegisteredEvent event) {

        kafkaTemplate.send(
                KafkaConfig.USER_REGISTERED_TOPIC,
                event.userId().toString(),
                event
        );
    }
}
