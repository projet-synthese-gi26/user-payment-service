package com.yowyob.template.domain.model;

import java.util.UUID;

public record Agent(UUID id, String name, String email, String password, String status) {
    // Helper pour masquer le mot de passe avant de renvoyer au front
    public Agent withoutPassword() {
        return new Agent(id, name, email, null, status);
    }
}
