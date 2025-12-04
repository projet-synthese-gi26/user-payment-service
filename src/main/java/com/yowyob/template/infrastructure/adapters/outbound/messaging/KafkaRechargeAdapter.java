package com.yowyob.template.infrastructure.adapters.outbound.messaging;

import com.yowyob.template.domain.ports.out.RechargePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaRechargeAdapter implements RechargePublisherPort {

    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;

    @Value("${application.kafka.topics.transaction-recharge}")
    private String rechargeTopic;

    // DTO interne pour l'envoi JSON (doit matcher celui attendu par le payment-service)
    record TransactionEvent(UUID walletId, BigDecimal amount) {}

    @Override
    public Mono<Void> publishRechargeEvent(UUID targetWalletId, BigDecimal amount) {
        TransactionEvent event = new TransactionEvent(targetWalletId, amount);

        log.info("Envoi demande recharge Kafka -> Wallet: {} Montant: {}", targetWalletId, amount);

        return kafkaTemplate.send(rechargeTopic, targetWalletId.toString(), event)
                .then();
    }
}