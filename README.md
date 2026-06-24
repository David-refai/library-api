# 📚 Library API

![Java](https://img.shields.io/badge/Java-21-E34F26?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)
![H2 DB](https://img.shields.io/badge/H2-In_Memory-00A9E2?style=for-the-badge)

A robust, versioned RESTful API built with Spring Boot, managing standard library operations. This project serves as a comprehensive demonstration of **Clean Architecture**, **Global Exception Handling**, **Rich Integration Testing**, and advanced **Concurrency Control**.

## ✨ Features

* **Domain Driven Layout**: `Controller` → `Service` → `Repository` layer separation.
* **API Versioning**: Hosted side-by-side `v1` and `v2` endpoints.
* **Separation of Concerns**: Entities are never exposed. Total reliance on `Request DTOs` and `Response DTOs` for payload management.
* **Spring Data JPA Relationships**:
  * `@OneToMany` (Author ↔ Book)
  * `@OneToOne` (Book ↔ Loan)
* **Global Exception Handling**: All API exceptions are funnelled through a centralized `@RestControllerAdvice` yielding a standardized `ApiError` JSON schema avoiding leaked stack traces (`400`, `404`, `409`).
* **Concurrency Optimistic Locking**: Implementation of `@Version` locking to protect database concurrency across parallel loans, ensuring scalable race-condition safety.
* **Test Validation & Coverage**: +30 independent, database-clearing integration tests checking happy path, bad requests, 404s, constraint failures, and multi-thread collision racing with **90% JaCoCo coverage**.

---

## 🚀 Running the API Locally

### Prerequisites
Make sure you have installed on your local machine:
- **Java 21** or later
- **Maven 3.9+** (or use the included wrapper)

### Start the Spring Boot Server
You can simply run the API using Maven:
```bash
cd library-api
mvn spring-boot:run
```
The server binds to `http://localhost:7070/`.

---

## 📖 API Documentation (OpenAPI / Swagger)

The API is fully documented using **SpringDoc OpenAPI**. You easily explore, test and interact with the endpoints dynamically using the Swagger UI view.

Once the application is running, navigate directly to:
👉 **[http://localhost:7070/swagger-ui.html](http://localhost:7070/swagger-ui.html)**

Or fetch the raw `JSON` spec via:
👉 **[http://localhost:7070/api-docs](http://localhost:7070/api-docs)**

---

## 🔐 Security — JWT Authentication & Roles

The API requires authentication on every `/api/**` endpoint. There are two built-in accounts:

| Username | Role  | Can do |
|----------|-------|--------|
| `admin`  | ADMIN | Read + create/update/delete (POST/PUT/PATCH/DELETE) |
| `user`   | USER  | Read only (GET) |

Credentials and the JWT signing secret are read from environment variables (see table below) — they are **not** hardcoded. Local defaults exist only so the app can start without extra setup.

**1. Log in to obtain a token:**
```bash
curl -X POST http://localhost:7070/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin-changeit"}'
```
Response:
```json
{ "token": "<jwt>", "expiresInMs": 3600000 }
```

**2. Call a protected endpoint with the token:**
```bash
curl http://localhost:7070/api/v1/books \
     -H "Authorization: Bearer <jwt>"
```

| Environment variable | Purpose | Local default |
|---|---|---|
| `JWT_SECRET` | Signing key for issued tokens | dev-only fallback (override in any real deployment) |
| `JWT_EXPIRATION_MS` | Token lifetime | `3600000` (1 hour) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Admin account | `admin` / `admin-changeit` |
| `USER_USERNAME` / `USER_PASSWORD` | Read-only account | `user` / `user-changeit` |

See [SECURITY_REPORT.md](SECURITY_REPORT.md) for the full OWASP Top 10 analysis behind these controls.

---

## 🐳 Run with Docker

The app ships with a multi-stage `Dockerfile` — no local Java installation needed.

```bash
docker build -t library-api .

docker run -p 7070:7070 \
  -e JWT_SECRET=replace-with-a-long-random-secret \
  -e ADMIN_PASSWORD=replace-with-a-strong-password \
  -e USER_PASSWORD=replace-with-a-strong-password \
  library-api
```

---

## ⚙️ CI/CD Pipeline

`.github/workflows/ci-cd-pipeline.yml` runs on every push/PR to `main`:

1. **build-and-test** — runs `mvn test`.
2. **dependency-check** — runs OWASP Dependency-Check; fails the pipeline on any High/Critical (CVSS ≥ 7) vulnerability.
3. **docker-build-push** *(push to `main` only)* — builds the image and pushes `:latest` and `:<sha>` to Docker Hub.
4. **deploy** *(push to `main` only)* — fully automated deployment step.

To enable steps 3–4, add these **Repository Secrets** (Settings → Secrets and variables → Actions):

| Secret | Required | Purpose |
|---|---|---|
| `DOCKERHUB_USERNAME` | Yes | Docker Hub login |
| `DOCKERHUB_TOKEN` | Yes | Docker Hub access token (not your password) |
| `NVD_API_KEY` | Optional | Speeds up the OWASP Dependency-Check NVD feed download |
| `DEPLOY_WEBHOOK_URL` | Optional | If set, the deploy job POSTs to it (e.g. a Render/Railway deploy hook) |

---

## 🧪 Testing & Code Coverage

The API is covered by exhaustive Integration Tests leveraging `TestRestTemplate` and a full `RANDOM_PORT` web environment to mimic real HTTP traffic alongside a volatile `H2` database which resets gracefully.

### Run All Tests
```bash
mvn clean test
```

### View Code Coverage (JaCoCo)
Coverage measurements are automatically captured through the `jacoco-maven-plugin`. After running the test suite, you can inspect the visual HTML report here:

```text
library-api/target/site/jacoco/index.html
```

---

## 🚦 Standalone Concurrency Exercises

This repository also contains heavily documented and visual standalone classes to experiment directly with multithreading concepts (`se.chasacademy.concurrency`).

To run them directly from the compiled target folder:

**Race Simulator (Race Condition Bug)**
```bash
mvn compile
java -cp target/classes se.chasacademy.concurrency.RaceSimulator
```

**Download Simulator (Thread Pools vs Unlimited parallel execution)**
```bash
java -cp target/classes se.chasacademy.concurrency.DownloadSimulator
```

**Concurrency Extreme (20+ Threads & Output Contention)**
```bash
java -cp target/classes se.chasacademy.concurrency.ConcurrencyExtra
```
