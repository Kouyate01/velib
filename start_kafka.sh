#!/bin/bash
# Script pour démarrer Zookeeper, Kafka et créer le topic velib-data

KAFKA_DIR="$HOME/kafka_2.13-3.9.1"

# Démarrer Zookeeper en arrière-plan
x-terminal-emulator -e bash -c "$KAFKA_DIR/bin/zookeeper-server-start.sh $KAFKA_DIR/config/zookeeper.properties" &
sleep 5

# Démarrer Kafka en arrière-plan
x-terminal-emulator -e bash -c "$KAFKA_DIR/bin/kafka-server-start.sh $KAFKA_DIR/config/server.properties" &
sleep 5

# Créer le topic velib-data
$KAFKA_DIR/bin/kafka-topics.sh --create --topic velib-data --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 || echo "Le topic existe déjà."

echo "Zookeeper, Kafka et le topic velib-data sont prêts."
