# ── Stage 1: Build ──────────────────────────────────────────────────────
# Run on the runner's native platform for speed; the JAR is platform-agnostic
FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependency layer independently of source changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the application
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Custom JRE via jlink ───────────────────────────────────────
# Must run on the TARGET platform so the JRE binary matches the runtime arch
FROM amazoncorretto:17-alpine AS jre-build

# binutils provides objcopy, which jlink requires on Alpine (musl libc)
RUN apk add --no-cache binutils

# Modules required for Spring Boot + Spring Data JPA + PostgreSQL + Actuator.
# To regenerate this list for your exact fat-JAR run:
#   jdeps --multi-release 17 --ignore-missing-deps -q -recursive \
#          --print-module-deps target/app.jar
RUN jlink \
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.xml,jdk.crypto.ec,jdk.naming.dns,jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /opt/jre

# ── Stage 3: Run ────────────────────────────────────────────────────────
# Bare Alpine — no JDK, no compiler, smallest possible attack surface
FROM alpine:3.21

ENV JAVA_HOME=/opt/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Copy only the custom JRE from the jlink stage
COPY --from=jre-build /opt/jre $JAVA_HOME

# Alpine uses addgroup/adduser instead of groupadd/useradd
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1
