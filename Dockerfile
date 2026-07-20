# syntax=docker/dockerfile:1

# =========================================================================
# Stage 1: сборка jar через Maven
# =========================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Сначала только pom.xml — чтобы слой с зависимостями кэшировался
# Docker'ом отдельно и не пересобирался при каждом изменении кода.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Теперь код — этот слой будет пересобираться при каждом изменении src/
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# =========================================================================
# Stage 2: минимальный рантайм-образ
# =========================================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

# curl нужен только для HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Непривилегированный пользователь — не запускаем приложение от root
RUN groupadd --system spring && useradd --system --gid spring spring

WORKDIR /app
COPY --from=build /build/target/finance-tracker-*.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

# JVM и так уважает лимиты контейнера (cgroup) начиная с Java 10+,
# явные -Xmx можно прокинуть через JAVA_OPTS при docker run/compose.
ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
