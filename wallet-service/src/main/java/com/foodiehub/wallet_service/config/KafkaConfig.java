package com.foodiehub.wallet_service.config;

import com.foodiehub.wallet_service.kafka.UserRegisteredEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, UserRegisteredEvent> consumerFactory() {

        JsonDeserializer<UserRegisteredEvent> deserializer =
                new JsonDeserializer<>(UserRegisteredEvent.class);

        deserializer.addTrustedPackages(
                "com.foodiehub.wallet_service.kafka"
        );

        // IMPORTANT:
        // Ignore the Java class information added by Auth Service.
        // Deserialize directly into Wallet's UserRegisteredEvent.
        deserializer.setUseTypeHeaders(false);

        Map<String, Object> config = new HashMap<>();

        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "wallet-service"
        );

        config.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, UserRegisteredEvent> consumerFactory
    ) {

        ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        return factory;
    }
}