FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B verify
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/data && chown -R 10001:10001 /app
COPY --from=build /app/target/customer-agent-0.1.0.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
