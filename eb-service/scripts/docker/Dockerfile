# eb-service Docker image
#
# Build (repository root):
#   docker build -f eb-service/scripts/docker/Dockerfile -t nsight-eb:local .
#
# Run:
#   docker run --rm -p 8089:8089 \
#     -e SPRING_PROFILES_ACTIVE=local \
#     -e NSIGHT_EB_EVENT_PUBLISH_EP_ONLINE_URL=http://host.docker.internal:8090/ep/online \
#     -v nsight-eb-data:/app/data \
#     nsight-eb:local
#
# Health:
#   curl http://localhost:8089/actuator/health

# ---------- build stage ----------
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

# Gradle wrapper + build scripts first (layer cache)
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# Modules required to produce eb.war
COPY tcf-util ./tcf-util
COPY tcf-core ./tcf-core
COPY tcf-web ./tcf-web
COPY eb-service ./eb-service

# Windows checkout often has CRLF on gradlew; that makes Linux report "./gradlew: not found"
RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew --no-daemon \
        :tcf-util:build \
        :tcf-core:build \
        :tcf-web:build \
        :eb-service:bootWar \
        -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 ebuser \
    && mkdir -p /app/data/nsight-txlog \
    && chown -R ebuser:ebuser /app

COPY --from=build --chown=ebuser:ebuser /workspace/eb-service/build/libs/eb.war /app/eb.war

USER ebuser

ENV SPRING_PROFILES_ACTIVE=local \
    SERVER_PORT=8089 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8" \
    NSIGHT_TXLOG_PATH=/app/data/nsight-txlog

EXPOSE 8089

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dnsight.txlog.path=${NSIGHT_TXLOG_PATH} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -Dserver.port=${SERVER_PORT} -jar /app/eb.war"]
