# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.8.6-openjdk-8 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=builder /app/target/book-server.jar .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "book-server.jar"]
