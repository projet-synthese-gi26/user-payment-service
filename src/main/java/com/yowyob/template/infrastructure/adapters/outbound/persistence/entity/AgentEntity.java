package com.yowyob.template.infrastructure.adapters.outbound.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;

@Table("agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentEntity {
    @Id
    private UUID id;
    private String name;
    private String email;
    private String password;
    private String status;
}
