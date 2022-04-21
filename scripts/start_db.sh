#!/bin/bash

CONTAINER_NAME=balancer-postgres
DB_USER=balanceradmin
DB_PASSWORD=PA55W0rD
DB_NAME=balancer_db
DB_PORT=6000

docker run \
  -d \
  --restart unless-stopped \
  --name "$CONTAINER_NAME" \
  -p "$DB_PORT":5432 \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -e POSTGRES_USER="$DB_USER" \
  -e POSTGRES_DB="$DB_NAME" \
postgres:latest
