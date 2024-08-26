# Docker login

```bash
docker login docker.io -u rsof -p <docker_key>
```

# Build images

### Worker

```bash
./gradlew worker_controller:dockerTagsPush
```

### Scaling controller

```bash
./gradlew scaling_controller:dockerTagsPush
```

### Balancer API

```bash
./gradlew balancer_api:dockerTagsPush
```

### Load Balancer

```bash
./gradlew load_balancer:dockerTagsPush
```