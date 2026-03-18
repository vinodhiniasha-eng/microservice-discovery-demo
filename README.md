# Microservice Discovery

Spring Boot microservice demo using Eureka service registry and client-side load balancing, with optional Linkerd service mesh on Kubernetes.

## Architecture

### Part 1 — Eureka Service Discovery

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
                      |    localhost:8083    |
                      +----------------------+
```

### Part 2 — Linkerd Service Mesh on Kubernetes (Optional)

```
client-service pod
  app container + linkerd-proxy
          |
          v
   greeting-service (K8s Service)
          |
   +------+------+
   |             |
pod 1         pod 2
app+proxy     app+proxy
```

---

## Services

| Service | Port | Description |
|---|---|---|
| registry-server | 8761 | Eureka service registry |
| greeting-service | 8081 / 8082 | REST service, run two instances |
| client-service | 8083 | Calls greeting-service via discovery |

---

## Part 1 — Run with Eureka

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
curl http://localhost:8083/call
```

Refresh several times. The response alternates between:
```
Hello from greeting-service instance on port 8081
Hello from greeting-service instance on port 8082
```

### How It Works

- `registry-server` runs Eureka — the central service registry.
- `greeting-service` registers itself under the name `greeting-service` on startup.
- Running it twice creates two registered instances under the same name.
- `client-service` uses a `@LoadBalanced` `RestTemplate` and calls `http://greeting-service/hello`.
- Spring Cloud LoadBalancer resolves the service name via Eureka and round-robins between instances.

---

## Part 2 — Linkerd Service Mesh on Kubernetes (Optional)

### Prerequisites

```bash
brew install minikube kubectl
```

Install Linkerd CLI:
```bash
curl --proto '=https' --tlsv1.2 -sSfL https://run.linkerd.io/install | sh
export PATH=$PATH:$HOME/.linkerd2/bin
echo 'export PATH=$PATH:$HOME/.linkerd2/bin' >> ~/.zshrc
```

### 1. Start local Kubernetes cluster

```bash
minikube start
eval $(minikube docker-env)
```

### 2. Build and push Docker images

```bash
cd greeting-service
docker build -t asha307/greeting-service:latest .
docker push asha307/greeting-service:latest

cd ../client-service
docker build -t asha307/client-service:latest .
docker push asha307/client-service:latest
```

### 3. Deploy to Kubernetes

```bash
kubectl apply -f k8s/greeting-deployment.yaml
kubectl apply -f k8s/client-deployment.yaml
kubectl get pods
```

### 4. Install Linkerd control plane

```bash
kubectl apply --server-side -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.4.0/standard-install.yaml
linkerd install --crds | kubectl apply -f -
linkerd install --set proxyInit.runAsRoot=true | kubectl apply -f -
linkerd check
```

### 5. Inject Linkerd sidecar proxies

```bash
kubectl get deploy greeting-service -o yaml | linkerd inject - | kubectl apply -f -
kubectl get deploy client-service -o yaml | linkerd inject - | kubectl apply -f -
kubectl get pods
```

Each pod should show `2/2` containers — the app container and the `linkerd-proxy` sidecar.

### 6. Install Linkerd Viz (observability)

```bash
linkerd viz install | kubectl apply -f -
linkerd viz check
```

### 7. Test and observe

Port-forward the client service:
```bash
kubectl port-forward svc/client-service 8083:80
```

Open http://localhost:8083/call and refresh several times. Traffic is load balanced between the two greeting-service pods through the mesh.

View mesh stats:
```bash
linkerd viz stat deploy
```

Open the Linkerd dashboard:
```bash
linkerd viz dashboard
```

### How It Works

- Kubernetes Service handles DNS-based discovery — no Eureka needed.
- `linkerd inject` adds a `linkerd-proxy` sidecar container to each pod.
- All pod-to-pod traffic flows through the sidecar proxies transparently — no code changes required.
- The Linkerd control plane provides observability (metrics, live traffic), reliability (retries, timeouts), and security (mTLS) at the platform layer.

---

## Project Structure

```
microservice-discovery/
├── registry-server/          # Eureka server
├── greeting-service/         # REST service (2 instances)
│   └── Dockerfile
├── client-service/           # Calls greeting-service
│   └── Dockerfile
├── k8s/
│   ├── greeting-deployment.yaml
│   └── client-deployment.yaml
└── README.md
```
