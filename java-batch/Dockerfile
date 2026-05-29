# PAS Premium Batch - Docker Image
# Spring Boot 3.x / Java 17
# Replaces COBOL PREMBAT program and PREMIUM-BATCH.jcl

# ---------------------------------------------------------------------------
# Stage 1 – Build the JAR with Maven
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -B -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 – Runtime image
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

LABEL maintainer="mainframe-ops@acme.com"
LABEL description="Policy Administration System - Premium Batch Calculator"
LABEL version="1.0.0"

RUN mkdir -p /app/logs

COPY --from=build /workspace/target/pas-premium-batch-1.0.0.jar /app/pas-premium-batch.jar

WORKDIR /app

ENV DB2_USERNAME=PASFACAD
ENV DB2_PASSWORD=changeme
ENV JAVA_OPTS="-Xmx1g -Xms512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar pas-premium-batch.jar"]
