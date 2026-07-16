# syntax=docker/dockerfile:1

# ============================================================================
#  Business Core — image "all-in-one" : app + PostgreSQL + Redis + Kafka (KRaft)
#  dans un seul conteneur, supervisés par supervisord.
# ============================================================================

# ---- Stage 1 : build du jar Spring Boot -----------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && \
    mv target/*.jar /build/app.jar

# ---- Stage 2 : runtime tout-en-un ------------------------------------------
FROM eclipse-temurin:21-jre-jammy

ARG KAFKA_VERSION=3.9.1
ARG KAFKA_SCALA_VERSION=2.13

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8

# PostgreSQL 16, Redis, supervisord + outils de démarrage
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl gnupg lsb-release && \
    install -d /usr/share/postgresql-common/pgdg && \
    curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc && \
    echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" \
      > /etc/apt/sources.list.d/pgdg.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
      postgresql-16 \
      redis-server \
      supervisor \
      gosu \
      netcat-openbsd && \
    rm -rf /var/lib/apt/lists/*

# Kafka (mode KRaft, sans Zookeeper)
RUN curl -fsSL "https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_${KAFKA_SCALA_VERSION}-${KAFKA_VERSION}.tgz" \
      -o /tmp/kafka.tgz && \
    mkdir -p /opt/kafka && \
    tar -xzf /tmp/kafka.tgz -C /opt/kafka --strip-components=1 && \
    rm /tmp/kafka.tgz

ENV PATH="/opt/kafka/bin:${PATH}" \
    PGDATA=/var/lib/postgresql/data \
    KAFKA_DATA_DIR=/var/lib/kafka/data \
    REDIS_DATA_DIR=/var/lib/redis

# Répertoires de données (persistés via volumes docker-compose)
RUN mkdir -p "$PGDATA" "$KAFKA_DATA_DIR" "$REDIS_DATA_DIR" /var/log/supervisor /app && \
    chown -R postgres:postgres "$PGDATA" && \
    chown -R redis:redis "$REDIS_DATA_DIR"

COPY --from=build /build/app.jar /app/app.jar
COPY docker/entrypoint.sh /entrypoint.sh
COPY docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY docker/init-db.sh /docker-init/init-db.sh
COPY infra/postgres/01-roles.sql /docker-init/01-roles.sql
RUN chmod +x /entrypoint.sh /docker-init/init-db.sh

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=12 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["/entrypoint.sh"]
