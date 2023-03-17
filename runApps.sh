#!/usr/bin/env sh

echo "👉 Maven clean install"
./mvnw clean install -DskipTests

echo "👉 Building docker container"
#// TODO remove
 # docker stop aggregation-api
 # docker rm aggregation-api
 # docker rmi aggregation-api
docker build . --platform linux/amd64 --tag aggregation-api

docker-compose up
