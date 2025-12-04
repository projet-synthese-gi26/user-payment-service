package com.yowyob.template.domain.ports.in;

import com.yowyob.template.domain.model.Agent;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface AgentUseCase {
    // Auth & Gestion
    Mono<Agent> register(Agent agent);
    Mono<String> login(String email, String password); // Retourne un Token (simulé)

    // Opération métier : Recharge
    Mono<Void> performRecharge(UUID agentId, UUID targetWalletId, BigDecimal amount);
}
