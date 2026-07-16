#!/bin/bash
# Point d'entrée unique du conteneur all-in-one : prépare PostgreSQL et Kafka
# au premier démarrage puis délègue le pilotage des process à supervisord.
set -euo pipefail

/docker-init/init-db.sh

KAFKA_CONFIG=/opt/kafka/config/server.properties
cat > "$KAFKA_CONFIG" <<EOF
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@localhost:9093
listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
advertised.listeners=PLAINTEXT://${KAFKA_ADVERTISED_HOST:-localhost}:9092
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
log.dirs=${KAFKA_DATA_DIR}
offsets.topic.replication.factor=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
group.initial.rebalance.delay.ms=0
auto.create.topics.enable=true
EOF

if [ ! -f "${KAFKA_DATA_DIR}/meta.properties" ]; then
    echo "[entrypoint] Formatage du stockage Kafka (KRaft)"
    kafka-storage.sh format -t "$(kafka-storage.sh random-uuid)" -c "$KAFKA_CONFIG" --ignore-formatted
fi

cat > /entrypoint-kafka.sh <<'EOF'
#!/bin/bash
exec /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties
EOF
chmod +x /entrypoint-kafka.sh

cat > /entrypoint-app.sh <<'EOF'
#!/bin/bash
set -euo pipefail
echo "[entrypoint-app] Attente de PostgreSQL, Redis et Kafka..."
until pg_isready -h localhost -p 5432 -U "${BC_DB_OWNER_USER:-bc_owner}" >/dev/null 2>&1; do sleep 1; done
until redis-cli -h localhost ping >/dev/null 2>&1; do sleep 1; done
until nc -z localhost 9092 >/dev/null 2>&1; do sleep 1; done
echo "[entrypoint-app] Dépendances prêtes, démarrage de l'application."
exec java ${JAVA_OPTS:-} -jar /app/app.jar
EOF
chmod +x /entrypoint-app.sh

exec /usr/bin/supervisord -c /etc/supervisor/conf.d/supervisord.conf
