package com.foodiehub.dispatch_service.config;

import com.foodiehub.dispatch_service.dao.BatchingConfigDao;
import com.foodiehub.dispatch_service.model.BatchingConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BatchingConfigInitializer implements CommandLineRunner {

    private final BatchingConfigDao batchingConfigDao;

    @Override
    public void run(String... args) {

        if (batchingConfigDao.count() == 0) {

            batchingConfigDao.save(
                    BatchingConfig.builder()
                            .maxBatchSize(4)
                            .batchRadiusKm(
                                    BigDecimal.valueOf(1.5)
                            )
                            .maxDetourPercentage(
                                    BigDecimal.valueOf(30)
                            )
                            .build()
            );
        }
    }
}