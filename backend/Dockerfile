# TradeUp API — container image.
#
# Render has no native Java runtime, so JVM services deploy from a Dockerfile.
# Two stages: the first builds the jar with the full JDK, the second ships only
# a JRE and the jar, which keeps the deployed image a few hundred MB rather than
# well over a gigabyte.

# ---------------------------------------------------------------------------
# Stage 1 — build
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

# Copy the wrapper and the POM on their own first. Docker caches this layer, so
# dependencies are only re-downloaded when the POM actually changes, not on
# every source edit.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src

# Tests run in CI and locally against in-memory H2; skipping them here keeps
# deploys fast and stops a build depending on anything outside the image.
RUN ./mvnw -B -DskipTests package \
    && mv target/tradeup-*.jar app.jar

# ---------------------------------------------------------------------------
# Stage 2 — run
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as a non-root user. If the process is ever compromised it should not also
# own the filesystem it is running on.
RUN addgroup -S tradeup && adduser -S -G tradeup tradeup

COPY --from=build --chown=tradeup:tradeup /build/app.jar app.jar

# Uploaded photos and generated exports land here. On a host with an ephemeral
# filesystem this directory does not survive a restart — see the README.
RUN mkdir -p /app/data && chown -R tradeup:tradeup /app/data

USER tradeup

# MaxRAMPercentage lets the JVM size its heap from the container limit rather
# than from the host, which is what keeps it inside a small instance.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Documentation only: the real port comes from PORT at runtime.
EXPOSE 8080

# Shell form so $JAVA_OPTS is expanded. Spring reads PORT itself.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
