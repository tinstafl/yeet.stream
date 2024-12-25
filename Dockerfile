FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY sdk-config.yaml ./sdk-config.yaml
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/sdk-config.yaml /sdk-config.yaml
COPY --from=build /app/target/yeet.stream-0.1.0.jar /yeet.stream-0.1.0.jar
COPY --from=build /app/target/agent/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
COPY --from=build /app/target/agent/opentelemetry-javaagent-extension.jar /opentelemetry-javaagent-extension.jar
ENTRYPOINT ["java", "-javaagent:/opentelemetry-javaagent.jar", "-Dotel.javaagent.extensions=/opentelemetry-javaagent-extension.jar", "-jar", "/yeet.stream-0.1.0.jar"]
