FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY baituan_gateway/pom.xml ./pom.xml
RUN mvn -q dependency:go-offline
COPY baituan_gateway/src ./src
RUN mvn -q package
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/target/baituan-gateway-1.0.0.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
