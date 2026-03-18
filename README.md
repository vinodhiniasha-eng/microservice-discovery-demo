# Microservice Discovery

Spring Boot microservice demo using Eureka service registry and client-side load balancing.

## Architecture

```
                   +----------------------+
                   |   Eureka Registry    |
                   |   localhost:8761     |
                   +----------+-----------+
                              ^
                              |
                registers     |     registers
                              |
              +---------------+---------------+
              |                               |
+---------------------------+      +---------------------------+
| Greeting Service Instance 1|      | Greeting Service Instance 2|
| localhost:8081             |      | localhost:8082            |
| service: GREETING-SERVICE  |      | service: GREETING-SERVICE |
+-------------+--------------+      +-------------+-------------+
              ^                                   ^
              |                                   |
              +------------------+----------------+
                                 |
                          discovered via Eureka
                                 |
                      +----------+-----------+
                      |     Client Service   |
                      |    localhost:8080    |
                      +----------------------+
```

## Services

| Service | Port | Description |
|---|---|---|
| registry-server | 8761 | Eureka service registry |
| greeting-service | 8081 / 8082 | REST service, run two instances |
| client-service | 8080 | Calls greeting-service via discovery |

## How to Run

### 1. Start the registry

```bash
cd registry-server
mvn spring-boot:run
```

Open http://localhost:8761 to see the Eureka dashboard.

### 2. Start greeting-service instance 1 (port 8081)

```bash
cd greeting-service
mvn spring-boot:run
```

### 3. Start greeting-service instance 2 (port 8082)

```bash
cd greeting-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

Both instances should appear as `GREETING-SERVICE` in the Eureka dashboard.

### 4. Start the client

```bash
cd client-service
mvn spring-boot:run
```

### 5. Test load balancing

```bash
curl http://localhost:8080/call
```

Refresh several times. The response should alternate between:
```
Hello from greeting-service instance on port 8081
Hello from greeting-service instance on port 8082
```

## How It Works

- `registry-server` runs Eureka — the central service registry.
- `greeting-service` registers itself under the name `greeting-service` on startup.
- Running it twice (on 8081 and 8082) creates two registered instances under the same name.
- `client-service` uses a `@LoadBalanced` `RestTemplate` and calls `http://greeting-service/hello`.
- Spring Cloud LoadBalancer resolves `greeting-service` to a real host/port via Eureka and round-robins between instances.
