#!/bin/bash

echo "Initializing database"

PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -a -w --no-password -f /init-database.sql
PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -a -w --no-password \
                                     -c "INSERT INTO users(username, password)
                                         VALUES ('admin', '$ADMIN_PW')
                                         ON CONFLICT DO NOTHING;"
