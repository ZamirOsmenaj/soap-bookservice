# ── Build stage ──────────────────────────────────────────────────────────────
FROM maven:3.8.6-openjdk-8 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
# src includes the local WSDL and XSD files that wsimport reads at build time
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=builder /app/target/book-client.jar .
ENTRYPOINT ["sh", "-c", "java -Dserver.host=book-server -jar book-client.jar && tail -f /dev/null"]
