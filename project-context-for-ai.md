# Contexte Complet du Projet

**Projet:** user-payment-service  
**Date de génération:** 19/03/2026 23:56:26  
**Chemin:** D:\Projets\Scolaire\Reseau\Litige\user-payment-service

---

## Table des matières
1. [Structure du projet](#structure-du-projet)
2. [Contenu des fichiers](#contenu-des-fichiers)
3. [Statistiques](#statistiques)

---

## Structure du projet

```
├── .github
│   └── workflows
│       └── ci-cd.yml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── yowyob
│   │   │           └── template
│   │   │               ├── application
│   │   │               │   └── service
│   │   │               │       └── AgentService.java
│   │   │               ├── domain
│   │   │               │   ├── exception
│   │   │               │   │   └── StockFullException.java
│   │   │               │   ├── model
│   │   │               │   │   └── Agent.java
│   │   │               │   └── ports
│   │   │               │       ├── in
│   │   │               │       │   └── AgentUseCase.java
│   │   │               │       └── out
│   │   │               │           ├── AgentRepositoryPort.java
│   │   │               │           └── RechargePublisherPort.java
│   │   │               ├── infrastructure
│   │   │               │   ├── adapters
│   │   │               │   │   ├── inbound
│   │   │               │   │   │   └── rest
│   │   │               │   │   │       ├── dto
│   │   │               │   │   │       │   ├── AuthResponse.java
│   │   │               │   │   │       │   ├── LoginRequest.java
│   │   │               │   │   │       │   ├── RechargeRequest.java
│   │   │               │   │   │       │   └── RegisterRequest.java
│   │   │               │   │   │       ├── AgentController.java
│   │   │               │   │   │       └── GlobalExceptionHandler.java
│   │   │               │   │   └── outbound
│   │   │               │   │       ├── messaging
│   │   │               │   │       │   └── KafkaRechargeAdapter.java
│   │   │               │   │       └── persistence
│   │   │               │   │           ├── entity
│   │   │               │   │           │   └── AgentEntity.java
│   │   │               │   │           ├── repository
│   │   │               │   │           │   └── AgentR2dbcRepository.java
│   │   │               │   │           └── AgentR2dbcAdapter.java
│   │   │               │   ├── config
│   │   │               │   │   ├── ApplicationConfig.java
│   │   │               │   │   ├── DatabaseInitConfig.java
│   │   │               │   │   ├── KafkaConfig.java
│   │   │               │   │   ├── RedisConfig.java
│   │   │               │   │   ├── SecurityConfig.java
│   │   │               │   │   └── WebClientConfig.java
│   │   │               │   ├── mappers
│   │   │               │   │   └── AgentMapper.java
│   │   │               │   └── security
│   │   │               │       ├── AuthenticationManager.java
│   │   │               │       ├── JwtService.java
│   │   │               │       └── SecurityContextRepository.java
│   │   │               └── UserPaymentService.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── prod.application.yml
│   │       └── schema.sql
│   └── test
│       └── java
│           └── com
│               └── yowyob
│                   └── reactive_hexagonal
│                       └── ReactiveHexagonalApplicationTests.java
├── .gitattributes
├── .gitignore
├── compose.yaml
├── Dockerfile
├── generate.js
├── mvnw
├── mvnw.cmd
└── pom.xml
```

---

## Contenu des fichiers

### 📄 .github\workflows\ci-cd.yml

```yaml
name: Spring Boot CI/CD

on:
  push:
    branches:
      - "**"
      #- main

env:
  REGISTRY_IMAGE: ghcr.io/${{ github.repository_owner }}/user-payment-service
  APP_NAME: user-payment-service
  HEALTH_DELAY: ${{ secrets.DEPLOY_DELAY }}
  CONTAINER_NAME: user-payment-service

jobs:
  tests:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: 21

      - name: Run Unit Tests + SonarQube Analysis
        continue-on-error: true
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          mvn clean verify sonar:sonar \
            -Dsonar.projectKey=${{ env.APP_NAME }} \
            -Dsonar.projectName=${{ env.APP_NAME }} \
            -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }}

  build:
    needs: tests
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Log in to GitHub Container Registry
        run: |
          echo "${{ secrets.PERSONAL_ACCESS_TOKEN }}" | docker login ghcr.io -u ${{ secrets.REGISTRY_NAMESPACE }} --password-stdin

      - name: Use prod.application.yml
        run: |
          echo "Using prod.application.yml"
          rm src/main/resources/application.yml
          mv src/main/resources/prod.application.yml src/main/resources/application.yml
          cat src/main/resources/application.yml

      - name: Build Docker image
        run: |
          echo "Building Docker image..."
          docker build -t ${{ env.REGISTRY_IMAGE }}:latest .

      - name: Push Docker image
        run: |
          echo "Pushing Docker image..."
          docker push ${{ env.REGISTRY_IMAGE }}:latest


  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
      - name: Checkout repository
        uses: actions/checkout@v3

      - name: Add SSH key
        uses: webfactory/ssh-agent@v0.7.0
        with:
          ssh-private-key: ${{ secrets.SSH_PRIVATE_KEY }}

      - name: Deploy on server
        run: |
          ssh -o StrictHostKeyChecking=no ${{ secrets.REMOTE_USER }}@${{ secrets.REMOTE_HOST }} << 'EOF'

            echo "Pulling latest image ${{ env.REGISTRY_IMAGE }}"

            docker login ghcr.io -u ${{ secrets.REGISTRY_NAMESPACE }} -p ${{ secrets.PERSONAL_ACCESS_TOKEN }}

            cd /root/projet_synthese/infra

            echo "Stopping container"
            docker compose down ${{ env.CONTAINER_NAME }}

            echo "Removing old image"
            docker rmi -f ${{ env.REGISTRY_IMAGE }}:latest || true

            echo "Pulling new image"
            docker pull ${{ env.REGISTRY_IMAGE }}:latest

            echo " Starting container"
            docker compose up -d ${{ env.CONTAINER_NAME }}

            echo " Waiting ${{ env.HEALTH_DELAY }} seconds for health check"
            sleep ${{ env.HEALTH_DELAY }}

            echo " Checking container health..."
            STATUS=$(docker inspect --format='{{json .State.Health.Status}}' ${{ env.CONTAINER_NAME }})

            echo "Health status: $STATUS"

            if [ "$STATUS" != "\"healthy\"" ]; then
              echo "ERROR: Container is not healthy"
              exit 1
            fi

            echo "Deployment successful & container healthy!"
          EOF
```

*Lignes: 120*

---

### 📄 compose.yaml

```yaml

```

*Lignes: 1*

---

### 📄 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.6</version>
        <relativePath/>
    </parent>
    
    <groupId>com.yowyob</groupId>
    <artifactId>user-payment-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>reactive-hexagonal</name>
    <description>api pour la gestion des agents</description>
    
    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
    </properties>
    
    <dependencies>
        <!-- REACTIVE CORE -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
	   <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- DATA (R2DBC & REDIS) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-r2dbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>r2dbc-postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>

        <!-- MESSAGING (KAFKA) -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>io.projectreactor.kafka</groupId>
            <artifactId>reactor-kafka</artifactId>
            <version>1.3.22</version>
        </dependency>

        <!-- CLOUD & RESILIENCE -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
        </dependency>

        <!-- TOOLS (Lombok & Mapstruct) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        
        <!-- Docker Compose Auto-setup -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-docker-compose</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- SECURITY -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- JWT (JJWT) -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.30</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

```

*Lignes: 191*

---

### 📄 src\main\java\com\yowyob\template\application\service\AgentService.java

```java
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
```

*Lignes: 51*

---

### 📄 src\main\java\com\yowyob\template\domain\exception\StockFullException.java

```java
package com.yowyob.template.domain.exception;

public class StockFullException extends RuntimeException {
    public StockFullException(String message) {
        super(message);
    }
}
```

*Lignes: 7*

---

### 📄 src\main\java\com\yowyob\template\domain\model\Agent.java

```java
package com.yowyob.template.domain.model;

import java.util.UUID;

public record Agent(UUID id, String name, String email, String password, String status) {
    // Helper pour masquer le mot de passe avant de renvoyer au front
    public Agent withoutPassword() {
        return new Agent(id, name, email, null, status);
    }
}

```

*Lignes: 11*

---

### 📄 src\main\java\com\yowyob\template\domain\ports\in\AgentUseCase.java

```java
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

```

*Lignes: 17*

---

### 📄 src\main\java\com\yowyob\template\domain\ports\out\AgentRepositoryPort.java

```java
package com.yowyob.template.domain.ports.out;

import com.yowyob.template.domain.model.Agent;
import reactor.core.publisher.Mono;

public interface AgentRepositoryPort {
    Mono<Agent> save(Agent agent);
    Mono<Agent> findByEmail(String email);
}

```

*Lignes: 10*

---

### 📄 src\main\java\com\yowyob\template\domain\ports\out\RechargePublisherPort.java

```java
package com.yowyob.template.domain.ports.out;

import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

public interface RechargePublisherPort {
    Mono<Void> publishRechargeEvent(UUID targetWalletId, BigDecimal amount);
}

```

*Lignes: 10*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\inbound\rest\AgentController.java

```java
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
```

*Lignes: 42*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\inbound\rest\dto\AuthResponse.java

```java
package com.yowyob.template.infrastructure.adapters.inbound.rest.dto;

public record AuthResponse(String token) {}

```

*Lignes: 4*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\inbound\rest\dto\LoginRequest.java

```java
package com.yowyob.template.infrastructure.adapters.inbound.rest.dto;

public record LoginRequest(String email, String password) {}


```

*Lignes: 5*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\inbound\rest\dto\RechargeRequest.java

```java
package com.yowyob.template.infrastructure.adapters.inbound.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RechargeRequest(UUID targetWalletId, BigDecimal amount) {}


```

*Lignes: 8*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\inbound\rest\dto\RegisterRequest.java

```java
package com.yowyob.template.infrastructure.adapters.inbound.rest.dto;

public record RegisterRequest(String name, String email, String password) {}

```

*Lignes: 4*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\inbound\rest\GlobalExceptionHandler.java

```java
package com.yowyob.template.infrastructure.adapters.inbound.rest;

import com.yowyob.template.domain.exception.StockFullException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockFullException.class)
    public ProblemDetail handleStockException(StockFullException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Stock Overflow");
        problem.setType(URI.create("errors/stock-full"));
        return problem;
    }
}
```

*Lignes: 21*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\outbound\messaging\KafkaRechargeAdapter.java

```java
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
```

*Lignes: 36*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\outbound\persistence\AgentR2dbcAdapter.java

```java
package com.yowyob.template.infrastructure.adapters.outbound.persistence;

import com.yowyob.template.domain.model.Agent;
import com.yowyob.template.domain.ports.out.AgentRepositoryPort;
import com.yowyob.template.infrastructure.adapters.outbound.persistence.repository.AgentR2dbcRepository;
import com.yowyob.template.infrastructure.mappers.AgentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AgentR2dbcAdapter implements AgentRepositoryPort {

    private final AgentR2dbcRepository repository;
    private final AgentMapper mapper;

    @Override
    public Mono<Agent> save(Agent agent) {
        return repository.save(mapper.toEntity(agent))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Agent> findByEmail(String email) {
        return repository.findByEmail(email)
                .map(mapper::toDomain);
    }
}

```

*Lignes: 30*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\outbound\persistence\entity\AgentEntity.java

```java
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

```

*Lignes: 22*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\adapters\outbound\persistence\repository\AgentR2dbcRepository.java

```java
package com.yowyob.template.infrastructure.adapters.outbound.persistence.repository;

import com.yowyob.template.infrastructure.adapters.outbound.persistence.entity.AgentEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface AgentR2dbcRepository extends R2dbcRepository<AgentEntity, UUID> {
    Mono<AgentEntity> findByEmail(String email);
}

```

*Lignes: 11*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\config\ApplicationConfig.java

```java
package com.yowyob.template.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

*Lignes: 15*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\config\DatabaseInitConfig.java

```java
package com.yowyob.template.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.core.DatabaseClient;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Configuration
public class DatabaseInitConfig {

    private final DatabaseClient databaseClient;

    public DatabaseInitConfig(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @PostConstruct
    public void init() throws Exception {
        ClassPathResource resource = new ClassPathResource("schema.sql");
        String sql = Files.lines(Paths.get(resource.getURI()))
                          .collect(Collectors.joining("\n"));

        String[] statements = sql.split(";");

        Flux.fromArray(statements)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .flatMap(s -> databaseClient.sql(s).then())
            .subscribe(
                null,
                err -> System.err.println("Erreur lors de l'init DB : " + err),
                () -> System.out.println("Base initialisée avec succès")
            );
    }
}

```

*Lignes: 41*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\config\KafkaConfig.java

```java
package com.yowyob.template.infrastructure.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonSerializer.class);

        SenderOptions<String, Object> senderOptions = SenderOptions.create(props);

        return new ReactiveKafkaProducerTemplate<>(senderOptions);
    }
}

```

*Lignes: 32*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\config\RedisConfig.java

```java
package com.yowyob.template.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(mapper, Object.class);

        RedisSerializationContext<String, Object> context =
                RedisSerializationContext.<String, Object>newSerializationContext(new StringRedisSerializer())
                        .value(jsonSerializer)
                        .hashValue(jsonSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}

```

*Lignes: 35*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\config\SecurityConfig.java

```java
package com.yowyob.template.infrastructure.config;

import com.yowyob.template.infrastructure.security.AuthenticationManager;
import com.yowyob.template.infrastructure.security.SecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                // Gestionnaire d'authentification personnalisé
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)

                .authorizeExchange(exchanges -> exchanges
                        // Endpoints publics (Auth)
                        .pathMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .pathMatchers(
                                "/actuator/**").permitAll()
                        // Endpoints protégés (Tout le reste)
                        .anyExchange().authenticated()
                )
                // Gestion des erreurs (401 au lieu de redirection login)
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint((swe, e) ->
                                Mono.fromRunnable(() -> swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED))
                        )
                        .accessDeniedHandler((swe, e) ->
                                Mono.fromRunnable(() -> swe.getResponse().setStatusCode(HttpStatus.FORBIDDEN))
                        )
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Your allowed origins
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3999",
                "http://168.119.122.86:3999"
        ));

        // Your allowed methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allowed headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow credentials
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
```

*Lignes: 85*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\config\WebClientConfig.java

```java
package com.yowyob.template.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class WebClientConfig {

//    @Bean
//    public StockApiClient stockApiClient(WebClient.Builder builder,
//                                         @Value("${application.external.stock-service-url}") String url) {
//
//        WebClient webClient = builder.baseUrl(url).build();
//        WebClientAdapter adapter = WebClientAdapter.create(webClient);
//        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
//
//        return factory.createClient(StockApiClient.class);
//    }
}
```

*Lignes: 23*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\mappers\AgentMapper.java

```java
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

```

*Lignes: 14*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\security\AuthenticationManager.java

```java
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

```

*Lignes: 44*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\security\JwtService.java

```java
package com.yowyob.template.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String generateToken(String username) {
        return buildToken(new HashMap<>(), username);
    }

    private String buildToken(Map<String, Object> extraClaims, String username) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

```

*Lignes: 68*

---

### 📄 src\main\java\com\yowyob\template\infrastructure\security\SecurityContextRepository.java

```java
package com.yowyob.template.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    private final JwtService jwtService;

    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .flatMap(authHeader -> {
                    String token = authHeader.substring(7);

                    if (jwtService.isTokenValid(token)) {
                        String username = jwtService.extractUsername(token);

                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                username,
                                token,
                                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))
                        );
                        return Mono.just(new SecurityContextImpl(auth));
                    }
                    return Mono.empty();
                });
    }
}

```

*Lignes: 49*

---

### 📄 src\main\java\com\yowyob\template\UserPaymentService.java

```java
package com.yowyob.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserPaymentService {

	public static void main(String[] args) {
		SpringApplication.run(UserPaymentService.class, args);
	}

}

```

*Lignes: 14*

---

### 📄 src\main\resources\application.yml

```yaml
server:
  port: 8091

spring:
  application:
    name: user-payment-service
    
  docker:
    compose:
      enabled: false

 # POSTGRESQL (R2DBC)
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:payment_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
     pool:
      enabled: true
      initial-size: 1
      max-size: 5
      max-idle-time: 30m
      max-life-time: 10m
      acquire-retry: 3
      max-acquire-time: 30s
      validation-query: SELECT 1
  sql:
    init:
      mode: always

  # REDIS CLUSTER 
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:7000}
      password: ${REDIS_PASSWORD:password}
      cluster:
        enabled: false 

  # KAFKA 
  kafka:
    bootstrap-servers: ${KAFKA_HOST:localhost}:${KAFKA_PORT:9092}
    consumer:
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

# CUSTOM CONFIG 
application:
  external:
    stock-service-url: http://${EXTERNAL_HOST:localhost}:8081
  security:
    jwt:
      # Générez une vraie clé en prod (ex: openssl rand -base64 64)
      secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
      expiration: 3600000 # 1 heure en ms

  kafka:
    topics:
      transaction-recharge: transaction-recharge-topic

management:
  endpoints:
    web:
      exposure:
        include: ["health","info","prometheus"]
  endpoint:
    health:
      show-details: "always"
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true


# RESILIENCE4J 
resilience4j:
  circuitbreaker:
    instances:
      stock-service:
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
        slidingWindowSize: 5

```

*Lignes: 90*

---

### 📄 src\main\resources\prod.application.yml

```yaml
server:
  port: 8091

spring:
  application:
    name: user-payment-service
    
  docker:
    compose:
      enabled: false

  # POSTGRESQL (R2DBC)
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:payment_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
     pool:
      enabled: true
      initial-size: 1
      max-size: 5
      max-idle-time: 30m
      max-life-time: 10m
      acquire-retry: 3
      max-acquire-time: 30s
      validation-query: SELECT 1
  sql:
    init:
      mode: always

  # REDIS CLUSTER 
  data:
    redis:
      password: ${REDIS_PASSWORD:password}
      cluster:
        nodes:
          - ${REDIS_HOST:localhost}:7001
          - ${REDIS_HOST:localhost}:7002
          - ${REDIS_HOST:localhost}:7003
          - ${REDIS_HOST:localhost}:7004
          - ${REDIS_HOST:localhost}:7005
          - ${REDIS_HOST:localhost}:7006

  # KAFKA 
  kafka:
    bootstrap-servers: ${KAFKA_HOST:localhost}:${KAFKA_PORT:9092}
    consumer:
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

# CUSTOM CONFIG 
application:
  external:
    stock-service-url: http://${EXTERNAL_HOST:localhost}:8081
  security:
    jwt:
      # Générez une vraie clé en prod (ex: openssl rand -base64 64)
      secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
      expiration: 3600000 # 1 heure en ms

  kafka:
    topics:
      transaction-recharge: transaction-recharge-topic

management:
  endpoints:
    web:
      exposure:
        include: ["health","info","prometheus"]
  endpoint:
    health:
      show-details: "always"
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

# RESILIENCE4J 
resilience4j:
  circuitbreaker:
    instances:
      stock-service:
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
        slidingWindowSize: 5

```

*Lignes: 93*

---

### 📄 src\main\resources\schema.sql

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DROP TABLE IF EXISTS agents;
DROP TABLE IF EXISTS products;

CREATE TABLE agents (
                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                        name VARCHAR(255) NOT NULL,
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password VARCHAR(255) NOT NULL,
                        status VARCHAR(50) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

*Lignes: 13*

---

### 📄 src\test\java\com\yowyob\reactive_hexagonal\ReactiveHexagonalApplicationTests.java

```java
package com.yowyob.reactive_hexagonal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReactiveHexagonalApplicationTests {

	@Test
	void contextLoads() {
	}

}

```

*Lignes: 14*

---

## Statistiques

- **Total de fichiers analysés:** 34
- **Total de lignes de code:** 1 231
- **Moyenne de lignes par fichier:** 36

### Répartition par type de fichier

- **.java:** 28 fichiers
- **.yml:** 3 fichiers
- **.yaml:** 1 fichier
- **.xml:** 1 fichier
- **.sql:** 1 fichier

---

*Contexte généré automatiquement pour analyse par IA*
