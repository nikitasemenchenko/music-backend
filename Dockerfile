
# Build stage
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew && \
    ./gradlew --no-daemon clean buildFatJar


# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system appgroup && \
    useradd --system --uid 10001 \
      --gid appgroup appuser && \
    mkdir -p /app/storage && \
    chown -R appuser:appgroup /app

COPY --from=build \
    --chown=appuser:appgroup \
    /app/build/libs/*-all.jar /app/app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
