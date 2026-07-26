# tcf-ui Docker image
#
# Build (repository root):
#   docker build -f tcf-ui/scripts/docker/Dockerfile -t nsight-ui:local .
#
# Run:
#   docker run --rm -p 8099:8099 \
#     -e SPRING_PROFILES_ACTIVE=local \
#     -e NSIGHT_TCF_UI_BOOTRUN_HOST=http://host.docker.internal \
#     nsight-ui:local
#
# Health:
#   curl http://localhost:8099/actuator/health
# UI:
#   http://localhost:8099/

# ---------- build stage ----------
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates gnupg \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# Gradle wrapper + build scripts first (layer cache)
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# Modules required to produce tcf-ui.war (help export needs node + tcf-help)
COPY tcf-util ./tcf-util
COPY tcf-core ./tcf-core
COPY tcf-help ./tcf-help
COPY tcf-ui ./tcf-ui

# Windows checkout often has CRLF on gradlew; that makes Linux report "./gradlew: not found"
RUN sed -i 's/\r$//' ./gradlew \
    && chmod +x ./gradlew \
    && ./gradlew --no-daemon \
        :tcf-util:build \
        :tcf-core:build \
        :tcf-help:exportHelp \
        :tcf-ui:bootWar \
        -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 uiuser \
    && mkdir -p /app/data \
    && chown -R uiuser:uiuser /app

COPY --from=build --chown=uiuser:uiuser /workspace/tcf-ui/build/libs/tcf-ui.war /app/tcf-ui.war

USER uiuser

ENV SPRING_PROFILES_ACTIVE=local \
    SERVER_PORT=8099 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8" \
    NSIGHT_TCF_UI_BOOTRUN_HOST=http://host.docker.internal \
    NSIGHT_TCF_UI_TOMCAT_GATEWAY_URL=http://host.docker.internal:8080

EXPOSE 8099

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -Dserver.port=${SERVER_PORT} -jar /app/tcf-ui.war"]
