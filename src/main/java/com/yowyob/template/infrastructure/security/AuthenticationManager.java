package com.yowyob.template.infrastructure.security;

import com.yowyob.template.domain.ports.out.AgentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final AgentRepositoryPort agentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        return agentRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Utilisateur non trouvé")))
                .flatMap(agent -> {
                    if (passwordEncoder.matches(password, agent.password())) {
                        // Authentification réussie
                        return Mono.just(new UsernamePasswordAuthenticationToken(
                                agent.email(),
                                agent.password(),
                                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))
                        ));
                    } else {
                        return Mono.error(new BadCredentialsException("Mot de passe incorrect"));
                    }
                });
    }
}
