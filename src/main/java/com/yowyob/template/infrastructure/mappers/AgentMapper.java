package com.yowyob.template.infrastructure.mappers;

import com.yowyob.template.domain.model.Agent;
import com.yowyob.template.infrastructure.adapters.inbound.rest.dto.RegisterRequest;
import com.yowyob.template.infrastructure.adapters.outbound.persistence.entity.AgentEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AgentMapper {
    Agent toDomain(RegisterRequest request);
    AgentEntity toEntity(Agent domain);
    Agent toDomain(AgentEntity entity);
}
