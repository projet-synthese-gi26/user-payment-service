package com.yowyob.template.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.template.infrastructure.adapters.outbound.persistence.entity.AgentEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface AgentR2dbcRepository extends R2dbcRepository<AgentEntity, UUID> {
    Mono<AgentEntity> findByEmail(String email);
}
