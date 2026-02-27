# ---------------------------------------------------------------------------
# Stage 1: Builder
# ---------------------------------------------------------------------------
FROM azul/zulu-openjdk:23 AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY vader/build.gradle vader/

RUN ./gradlew :vader:dependencies --no-daemon

COPY vader/src vader/src
RUN ./gradlew :vader:bootJar --no-daemon

WORKDIR /workspace/vader/build/extracted
RUN java -Djarmode=layertools -jar ../libs/*.jar extract

# ---------------------------------------------------------------------------
# Stage 2: Runtime
# ---------------------------------------------------------------------------
FROM azul/zulu-openjdk:23-jre
WORKDIR /app

# We do this as root before switching to the unprivileged spring user.
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=builder /workspace/vader/build/extracted/dependencies/ ./
COPY --from=builder /workspace/vader/build/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/vader/build/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/vader/build/extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]