# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /build/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring

EXPOSE 8083

ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "app.jar"]