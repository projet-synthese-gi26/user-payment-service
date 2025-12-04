package com.yowyob.template.application.service;

import com.yowyob.template.domain.model.Agent;
import com.yowyob.template.domain.ports.in.AgentUseCase;
import com.yowyob.template.domain.ports.out.AgentRepositoryPort;
import com.yowyob.template.domain.ports.out.RechargePublisherPort;
import com.yowyob.template.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentService implements AgentUseCase {

    private final AgentRepositoryPort repository;
    private final RechargePublisherPort rechargePublisher;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ReactiveAuthenticationManager authenticationManager; // Injection du manager Spring Security

    @Override
    public Mono<Agent> register(Agent agent) {
        return repository.findByEmail(agent.email())
                .flatMap(existing -> Mono.error(new RuntimeException("Email déjà utilisé")))
                .switchIfEmpty(Mono.defer(() -> {
                    String encodedPwd = passwordEncoder.encode(agent.password());
                    Agent newAgent = new Agent(null, agent.name(), agent.email(), encodedPwd, "ACTIVE");
                    return repository.save(newAgent);
                }))
                .cast(Agent.class);
    }

    @Override
    public Mono<String> login(String email, String password) {
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))
                .map(auth -> jwtService.generateToken(auth.getName())) // Génère le vrai JWT si succès
                .onErrorMap(e -> new RuntimeException("Identifiants incorrects")); // Gérer l'erreur proprement
    }

    @Override
    public Mono<Void> performRecharge(UUID agentId, UUID targetWalletId, BigDecimal amount) {
        return rechargePublisher.publishRechargeEvent(targetWalletId, amount);
    }
}