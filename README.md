# SmartOps Planner

SmartOps Planner is a Spring Boot backend for operational planning. It helps teams manage skills, employees, tasks, assignments and planning runs, then automatically selects the best employee for each pending task using a scoring algorithm.

The project is designed as a clean REST API with JWT security, PostgreSQL persistence, Flyway migrations, automated tests and OpenAPI documentation.

## Overview

Operational teams often need to answer a simple but important question: who should work on each task?

SmartOps Planner models the core planning data and provides an assignment engine that considers:

- required skills
- employee seniority
- weekly capacity
- current workload
- task priority
- deadline pressure

The API can create domain data, run planning, inspect assignments and expose dashboard summaries.

## Features

- Employee management with skills, seniority and weekly capacity.
- Skill catalog management.
- Task management with priority, deadline, required skills and status.
- Planning runs that persist assignment decisions.
- Scoring explanations for why an employee was or was not selected.
- Dashboard endpoints for workload, task status and planning summary.
- JWT authentication with role-based authorization.
- Flyway-managed PostgreSQL schema.
- OpenAPI / Swagger UI documentation.
- Unit, controller and integration tests.
- GitHub Actions workflows for tests and Docker image build.

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Maven
- Testcontainers
- JUnit 5
- Mockito
- MockMvc
- springdoc-openapi / Swagger UI
- GitHub Actions

## Architecture

The codebase follows package-by-feature organization:

```text
com.smartops.planner
+-- auth          # registration and login
+-- user          # users and roles
+-- security      # JWT, filters and security config
+-- skill         # skills API and persistence
+-- employee      # employees API and persistence
+-- task          # tasks API and persistence
+-- planning      # scoring, assignments and planning runs
+-- dashboard     # reporting endpoints
+-- common        # shared exceptions and API error response
+-- config        # OpenAPI configuration
```

Each functional package follows the usual Spring shape where it makes sense:

- `Controller`
- `Service`
- `Repository`
- `Entity`
- `DTO`

Business logic lives in services, not controllers.

## Domain Model

Main entities:

- `User`: application account used for authentication.
- `Role`: `ADMIN`, `MANAGER`, `EMPLOYEE`.
- `Skill`: capability such as Java, Spring Boot or Docker.
- `Employee`: person with email, seniority, skills and weekly workload.
- `Task`: work item with priority, estimated hours, deadline, status and required skills.
- `PlanningRun`: execution record of the planning process.
- `Assignment`: result for one task in a planning run.

Important relationships:

- Employee many-to-many Skill.
- Task many-to-many required Skill.
- Task optional assigned Employee.
- PlanningRun one-to-many Assignment.
- Assignment references Task and optionally Employee.

Task statuses:

```text
PENDING
ASSIGNED
IN_PROGRESS
DONE
CANCELLED
```

Task priorities:

```text
LOW
MEDIUM
HIGH
URGENT
```

## Scoring Algorithm

`ScoringService` evaluates each candidate employee for each pending task. The best eligible candidate is selected.

Core scoring rules:

| Rule | Effect |
|---|---:|
| Matching required skill | `+20` per skill |
| Missing required skill | `-25` and not eligible |
| Extra skill | `+5`, capped at `+10` |
| Enough seniority | `+15` |
| Seniority gap | `-15` |
| Urgent task with non-senior employee | `-30` |
| Workload below 60% | `+15` |
| Workload between 60% and 80% | `+8` |
| Workload above 80% | `-10` |
| Enough weekly capacity | `+20` |
| Capacity exceeded | `-40` and not eligible |
| Close deadline with relevant workload | `-15` |

Hard blockers:

- missing required skills
- exceeding weekly capacity

When no employee is eligible, the task remains `PENDING` and an unassigned assignment is persisted with an explanation.

More detail is available in:

```text
src/main/java/com/smartops/planner/planning/scoring-algorithm.md
```

## API Endpoints

Public endpoints:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Home message |
| `GET` | `/api/health` | Health check |
| `POST` | `/api/auth/register` | Register user |
| `POST` | `/api/auth/login` | Login and receive JWT |
| `GET` | `/v3/api-docs` | OpenAPI JSON |
| `GET` | `/swagger-ui.html` | Swagger UI |

Protected endpoints:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/skills` | List skills |
| `GET` | `/api/skills/{id}` | Find skill |
| `POST` | `/api/skills` | Create skill |
| `DELETE` | `/api/skills/{id}` | Delete skill |
| `GET` | `/api/employees` | List employees |
| `GET` | `/api/employees/{id}` | Find employee |
| `POST` | `/api/employees` | Create employee |
| `PUT` | `/api/employees/{id}` | Update employee |
| `DELETE` | `/api/employees/{id}` | Delete employee |
| `GET` | `/api/tasks` | List tasks |
| `GET` | `/api/tasks/{id}` | Find task |
| `POST` | `/api/tasks` | Create task |
| `PUT` | `/api/tasks/{id}` | Update task |
| `PATCH` | `/api/tasks/{id}/status` | Update task status |
| `DELETE` | `/api/tasks/{id}` | Delete task |
| `POST` | `/api/planning/run` | Run planning |
| `GET` | `/api/planning/runs` | List planning runs |
| `GET` | `/api/planning/runs/{id}` | Find planning run |
| `GET` | `/api/planning/assignments` | List assignments |
| `GET` | `/api/planning/assignments/{id}` | Find assignment |
| `GET` | `/api/dashboard/workload` | Employee workload summary |
| `GET` | `/api/dashboard/task-status` | Task status summary |
| `GET` | `/api/dashboard/planning-summary` | Planning summary |

## Security

Authentication uses JWT.

Register or login to receive a token, then call protected endpoints with:

```http
Authorization: Bearer TOKEN
```

Roles:

- `ADMIN`
- `MANAGER`
- `EMPLOYEE`

Access rules:

| Path | Access |
|---|---|
| `/api/auth/**` | Public |
| `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` | Public |
| `/api/skills/**` | `ADMIN`, `MANAGER` |
| `/api/employees/**` | `ADMIN`, `MANAGER` |
| `/api/tasks/**` | `ADMIN`, `MANAGER` |
| `/api/planning/**` | `ADMIN`, `MANAGER` |
| `/api/dashboard/**` | `ADMIN`, `MANAGER` |
| `/api/my-tasks/**` | `EMPLOYEE` |

Passwords are stored with BCrypt.

## Running Locally

Requirements:

- Java 21
- Maven
- Docker Desktop

Start PostgreSQL:

```bash
docker compose up -d
```

Run the application:

```bash
mvn spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Default local database settings:

```text
database: smartops
username: smartops
password: smartops
port: 5432
```

## Running with Docker

The project includes `docker-compose.yml` for local PostgreSQL:

```bash
docker compose up -d
```

Build the application:

```bash
mvn clean package
```

Build an OCI image with Spring Boot buildpacks:

```bash
mvn spring-boot:build-image -Dspring-boot.build-image.imageName=smartops-planner:local
```

Run the image against the Compose database:

```bash
docker run --rm -p 8080:8080 ^
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/smartops ^
  -e SPRING_DATASOURCE_USERNAME=smartops ^
  -e SPRING_DATASOURCE_PASSWORD=smartops ^
  smartops-planner:local
```

On Linux, replace `host.docker.internal` if needed with the appropriate Docker host address or run both services on the same Docker network.

## Testing

Run all tests:

```bash
mvn clean test
```

Test coverage includes:

- service unit tests
- controller tests with MockMvc and mocked services
- security authorization tests
- JWT unit tests
- PostgreSQL integration tests with Testcontainers
- OpenAPI documentation accessibility test

Integration tests use Testcontainers and do not depend on the local `docker-compose.yml`.

Requirements for integration tests:

- Docker Desktop running

## CI/CD

GitHub Actions workflows live in:

```text
.github/workflows
```

Current workflows:

- `tests.yml`: runs Maven tests on `push` and `pull_request`.
- `docker-build.yml`: builds the application and Docker image on pushes to `main` and manual runs.

Docker Hub publishing is prepared for future secrets:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

## JSON Examples

Register:

```json
{
  "username": "manager",
  "password": "password123",
  "role": "MANAGER"
}
```

Login:

```json
{
  "username": "manager",
  "password": "password123"
}
```

Create skill:

```json
{
  "name": "Java"
}
```

Create employee:

```json
{
  "name": "Ada",
  "email": "ada@smartops.test",
  "maxWeeklyHours": 40,
  "currentWeeklyHours": 10,
  "seniorityLevel": "SENIOR",
  "skillIds": [1]
}
```

Create task:

```json
{
  "title": "Build planning API",
  "description": "Implement assignment planning flow",
  "priority": "HIGH",
  "estimatedHours": 6,
  "deadline": "2026-06-15",
  "requiredSkillIds": [1]
}
```

Update task status:

```json
{
  "status": "IN_PROGRESS"
}
```

Run planning:

```bash
curl -X POST http://localhost:8080/api/planning/run \
  -H "Authorization: Bearer TOKEN"
```

## Swagger

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

Use the `Authorize` button and paste:

```text
Bearer TOKEN
```

No frontend screenshots are included because this repository is a backend API project.

## Future Improvements

- Employee availability calendar.
- Skill proficiency levels.
- Configurable scoring weights.
- Employee self-service endpoints for `/api/my-tasks`.
- Forced assignments and manual override workflows.
- Pagination and filtering for list endpoints.
- Dockerfile or production Compose setup.
- Deployment workflow with environment-specific configuration.
