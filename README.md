# Build docker images
```
sbt assembly
docker build -t senz/payz-bank .
```

# Run with docker
```
docker run -it \
-e SWITCH_HOST=dev.localhost \
-e SWITCH_PORT=9090 \
-e CASSANDRA_HOST=10.2.2.23 \
-e CASSANDRA_PORT=9042 \
-v /Users/eranga/payz/trans/logs:/app/logs:rw \
-v /Users/eranga/payz/trans/.keys:/app/.keys:rw \
senz/payz-bank
```