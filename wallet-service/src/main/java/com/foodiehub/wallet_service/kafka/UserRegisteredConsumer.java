package com.foodiehub.wallet_service.kafka;

import com.foodiehub.wallet_service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredConsumer {
    private static final String TOPIC = "user.registered";
    private final WalletService walletService;
    @KafkaListener(
            topics = TOPIC,
            groupId = "wallet-service"
    )
    public void consume(UserRegisteredEvent event) {

        log.info(
                "Received UserRegisteredEvent for userId={}",
                event.userId()
        );

        walletService.createWalletIfNotExists(
                event.userId()
        );

        log.info(
                "Wallet created/verified for userId={}",
                event.userId()
        );
    }
}
