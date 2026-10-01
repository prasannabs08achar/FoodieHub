package com.foodiehub.order_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "order.preparation")
@Getter
@Setter
public class PreparationProperties {

    private long defaultPreparationMinutes = 20;
}