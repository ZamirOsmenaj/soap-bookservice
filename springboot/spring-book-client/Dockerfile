# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/spring-book-client-1.0-SNAPSHOT.jar app.jar

# Run the demo then keep the container alive (same pattern as plain-Java client)
# so you can docker exec in and inspect logs after the run completes.
ENTRYPOINT ["sh", "-c", "java -Dserver.host=spring-book-server -jar app.jar && tail -f /dev/null"]
