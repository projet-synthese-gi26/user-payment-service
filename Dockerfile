# ========================
# Stage 1: Build the JAR
# ========================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 1. Copy pom.xml and download dependencies (cached if pom.xml doesn't change)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copy source code and build
COPY src ./src
# We skip tests because they require running Kafka/DB which aren't available during build time
RUN mvn clean package -DskipTests

# ========================
# Stage 2: Create the Runtime Image
# ========================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1. Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 2. Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# 3. Expose the port defined in your application.yml
EXPOSE 8091

# 4. Run the application
# We use exec form to ensure signals (like SIGTERM) are passed correctly
ENTRYPOINT ["java", "-jar", "app.jar"]