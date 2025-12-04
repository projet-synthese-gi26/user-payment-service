package com.yowyob.template.domain.ports.out;

import com.yowyob.template.domain.model.Agent;
import reactor.core.publisher.Mono;

public interface AgentRepositoryPort {
    Mono<Agent> save(Agent agent);
    Mono<Agent> findByEmail(String email);
}
