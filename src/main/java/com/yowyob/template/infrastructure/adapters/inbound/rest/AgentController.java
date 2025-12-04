package com.yowyob.template.infrastructure.adapters.inbound.rest;

import com.yowyob.template.domain.model.Agent;
import com.yowyob.template.domain.ports.in.AgentUseCase;
import com.yowyob.template.infrastructure.adapters.inbound.rest.dto.AuthResponse;
import com.yowyob.template.infrastructure.adapters.inbound.rest.dto.LoginRequest;
import com.yowyob.template.infrastructure.adapters.inbound.rest.dto.RechargeRequest;
import com.yowyob.template.infrastructure.adapters.inbound.rest.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AgentController {

    private final AgentUseCase agentUseCase;

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Agent> register(@RequestBody RegisterRequest req) {
        return agentUseCase.register(new Agent(null, req.name(), req.email(), req.password(), null))
                .map(Agent::withoutPassword);
    }

    @PostMapping("/auth/login")
    public Mono<AuthResponse> login(@RequestBody LoginRequest req) {
        return agentUseCase.login(req.email(), req.password())
                .map(AuthResponse::new);
    }

    @PostMapping("/agents/{agentId}/recharge")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> recharge(@PathVariable UUID agentId, @RequestBody RechargeRequest req) {
        return agentUseCase.performRecharge(agentId, req.targetWalletId(), req.amount());
    }
}