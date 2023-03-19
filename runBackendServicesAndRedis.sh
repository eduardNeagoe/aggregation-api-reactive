#!/usr/bin/env sh

echo "👉 Running the backend services (Shipments, Track, Pricing)"

docker container run --publish 4000:4000 qwkz/backend-services:latest &

./runRedis.sh
