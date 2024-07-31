#!/bin/bash

echo "Initializing database"

PGPASSWORD="$DB_PASSWORD" psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -a -w --no-password -f /init-database.sql
PGPASSWORD="$DB_PASSWORD" psql -U "$DB_USER" -d "$DB_NAME" -h "$DB_HOST" -p "$DB_PORT" -a -w --no-password \
                                     -c "INSERT INTO users(username, password)
                                         VALUES ('admin', '$BALANCER_ADMIN_PW')
                                         ON CONFLICT(username)
                                         DO UPDATE SET
                                            password = '$BALANCER_ADMIN_PW';"
