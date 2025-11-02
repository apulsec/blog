# Build stage: compile the Eureka server
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage: lightweight JRE image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/blog-eureka-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
