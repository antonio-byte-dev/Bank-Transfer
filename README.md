# Transference

A money transfer backend built around a **Camunda 8** workflow, with automated fraud
detection, conditional human compliance review, and a Vue frontend for initiating
transfers and (as an admin) approving or declining flagged ones.

This project exists as a portfolio piece demonstrating workflow orchestration,
event-driven job processing, and DevOps practices (containerization, seeded demo
data, automated tests) around a realistic financial use case.

## Architecture

```
┌─────────────┐      REST       ┌──────────────────┐      gRPC/REST      ┌───────────────┐
│  Vue         │ ───────────────▶│  Spring Boot      │◀────────────────────▶│  Camunda 8     │
│  frontend    │◀─────────────── │  transfer-backend │                      │  (c8run)       │
└─────────────┘                  └──────────┬────────┘                      └───────────────┘
                                             │
                                             ▼
                                   ┌───────────────────┐
                                   │  Postgres          │
                                   │  (accounts,         │
                                   │   transfer_history)  │
                                   └───────────────────┘
```

The Spring Boot app is both a **REST API** (serving the frontend) and a set of
**Camunda job workers** that drive a BPMN process for every transfer. See
[`docs/WORKFLOW.md`](docs/WORKFLOW.md) for the full process breakdown and
[`docs/API.md`](docs/API.md) for endpoint details.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot, Java 17 |
| Workflow engine | Camunda 8 (via `c8run` for local development) |
| Database | PostgreSQL |
| Frontend | Vue 3 (Composition API), Vite |
| Testing | JUnit 5, Mockito, AssertJ |
| Containerization | Docker, Docker Compose |

## Prerequisites

- Docker and Docker Compose
- A running Camunda 8 instance reachable from the backend container. Local
  development uses [`c8run`](https://github.com/camunda/camunda/tree/main/c8run),
  which bundles Zeebe, Elasticsearch, and Operate as local processes.

## Running the project
 
1. Start Camunda locally via `c8run` (outside Docker — see its own docs for setup).
2. **Deploy `workflow.bpmn` to Camunda.** The backend's job workers
   (`balance-check`, `fraud-check`, `compliance-review`, `execute-transfer`)
   only receive work once a process definition referencing those job types
   has actually been deployed — without this step, submitted transfers will
   silently hang with no error, since there's no deployed process for them
   to run against. Open `workflow.bpmn` in **Camunda Modeler** and use its
   **Deploy** button (top-right), pointing it at your running `c8run`
   instance (default `http://localhost:8080`).
   Re-deploy any time `workflow.bpmn` changes — Zeebe process definitions
   are versioned and immutable, so editing the file has no effect on
   already-running or future instances until it's redeployed. See
   [`docs/WORKFLOW.md`](docs/WORKFLOW.md) for the process itself.
3. From the project root, bring up the rest of the stack:
```bash
   docker compose up --build
```
 
   This starts:
   - `postgres` — the application database
   - `backend` — the Spring Boot app (REST API + Camunda job workers), on
     `http://localhost:8081`
   - `frontend` — the Vue app, served via Nginx, on `http://localhost:5173`
4. Open `http://localhost:5173` in a browser. Demo accounts and transfer
   history are reset automatically on every backend startup (see
   [Demo data](#demo-data) below).

### Demo data

The backend seeds a fixed set of demo accounts and clears prior transfer
history on every startup, so each run starts from a known, repeatable state.
This is controlled by the `demo` Spring profile
(`SPRING_PROFILES_ACTIVE=demo` in `docker-compose.yml`) and implemented in
`DemoDataSeeder`. Remove that profile for anything other than local demo use.

### Resetting everything

```bash
docker compose down -v
docker compose up --build
```

`-v` removes the named Postgres/Zeebe volumes entirely — use this if you want
a guaranteed clean slate (e.g. after a schema change).

## Running tests

```bash
mvn test
```


## Project structure

```
src/main/java/com/antoniobytedev/transference/
├── config/       # Startup configuration (e.g. DemoDataSeeder)
├── controller/    # REST controllers
├── entity/        # JPA entities (Account)
├── model/         # Non-entity domain models (TransferHistory)
├── repository/    # Spring Data repositories
├── service/       # Business logic (AccountService, FraudDetectionService, ...)
└── worker/        # Camunda job workers

frontend/transference-frontend/
└── src/views/TransferForm.vue   # Transfer form + admin review panel
```

## Further reading

- [`docs/WORKFLOW.md`](docs/WORKFLOW.md) — the BPMN process, gateway logic, and fraud scoring rules
- [`docs/API.md`](docs/API.md) — REST endpoint reference
