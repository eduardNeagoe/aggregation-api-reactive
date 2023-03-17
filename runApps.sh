#!/usr/bin/env sh

echo "👉 Maven clean install (no tests)"
./mvnw clean install -DskipTests

echo "👉 Building docker container"

docker build . --platform linux/amd64 --tag aggregation-api

docker-compose up
