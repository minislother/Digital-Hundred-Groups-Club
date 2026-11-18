# Build the Spring Boot jar inside Docker so the server does not need Maven or JDK installed.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY club_springboot/pom.xml ./pom.xml
RUN mvn -q -DskipTests dependency:go-offline

COPY club_springboot/src ./src
RUN mvn -q -DskipTests package

# Run with a smaller JRE image and a non-root user for safer production defaults.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system baituan \
    && useradd --system --gid baituan --home-dir /app baituan \
    && mkdir -p /var/baituan/upload /var/log/baituan \
    && chown -R baituan:baituan /app /var/baituan /var/log/baituan

COPY --from=build /workspace/target/shop-0.0.1-SNAPSHOT.jar /app/app.jar

USER baituan
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8081 \
    JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
