# SmartOps Planner

API backend en Spring Boot para planificacion operativa. Gestiona usuarios, empleados, skills, tareas y ejecuciones de planificacion. Incluye una interfaz web sencilla por roles.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Security + JWT
- PostgreSQL
- Flyway
- Maven
- Docker / Docker Compose
- JUnit, Mockito, MockMvc y Testcontainers
- Swagger/OpenAPI

## Funcionalidades

- Login con JWT.
- Roles `ADMIN`, `MANAGER` y `EMPLOYEE`.
- Gestion de usuarios, empleados, skills y tareas.
- Planificacion automatica de tareas.
- Asignaciones con explicacion.
- Vista de tareas propias para empleados.
- Dashboard operativo.
- Tests de servicios, controllers, seguridad e integracion.
- Dockerfile multi-stage.
- Docker Compose con PostgreSQL y app.
- GitHub Actions para CI y build Docker.

## Roles

| Rol | Puede hacer |
|---|---|
| `ADMIN` | Usuarios, empleados, skills, tareas, dashboard y planificacion |
| `MANAGER` | Empleados, skills, tareas, dashboard y planificacion |
| `EMPLOYEE` | Ver sus tareas asignadas y cambiar su estado |

## Usuarios Demo

Solo se cargan con el perfil `dev`.

| Usuario | Rol | Password |
|---|---|---|
| `admin` | `ADMIN` | `password123` |
| `manager` | `MANAGER` | `password123` |
| `employee` | `EMPLOYEE` | `password123` |
| `laura.sanchez` | `MANAGER` | `password123` |
| `miguel.torres` | `MANAGER` | `password123` |
| `ana.garcia` | `EMPLOYEE` | `password123` |
| `carlos.martin` | `EMPLOYEE` | `password123` |
| `marta.lopez` | `EMPLOYEE` | `password123` |

## Ejecutar En Local

### Opcion recomendada para desarrollar

Levanta solo PostgreSQL con Docker y ejecuta la app con Maven:

```powershell
docker compose up -d postgres
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Abrir:

```text
http://localhost:8080
```

### Probar todo con Docker Compose

```powershell
copy .env.example .env
docker compose --profile app up --build
```

Abrir:

```text
http://localhost:8080
```

Comprobar health:

```powershell
curl http://localhost:8080/api/health
```

Debe devolver:

```text
OK
```

Parar:

```powershell
docker compose --profile app down
```

Borrar tambien la base de datos:

```powershell
docker compose --profile app down -v
```

## Docker

Archivos principales:

```text
Dockerfile
docker-compose.yml
.dockerignore
.env.example
```

El `Dockerfile` construye la app en dos fases:

- fase build: Maven + Java 21
- fase runtime: JRE ligero para ejecutar el `.jar`

`docker-compose.yml` puede levantar:

- `postgres`
- `app` + `postgres` usando `--profile app`

Los servicios tienen healthcheck:

- PostgreSQL usa `pg_isready`
- la app usa `/api/health`

No subas `.env` al repositorio. Usa `.env.example` como plantilla.

## Seguridad

- `POST /api/auth/login` es publico.
- `POST /api/auth/register` requiere `ADMIN`.
- `/api/users/**` requiere `ADMIN`.
- `/api/employees/**`, `/api/skills/**`, `/api/tasks/**`, `/api/planning/**` y `/api/dashboard/**` requieren `ADMIN` o `MANAGER`.
- `/api/my-tasks/**` requiere `EMPLOYEE`.
- Las passwords se guardan con BCrypt.
- La API usa JWT con:

```http
Authorization: Bearer TOKEN
```

El secreto JWT se configura con:

```text
APP_SECURITY_JWT_SECRET
```

## Endpoints Principales

| Metodo | Endpoint | Descripcion |
|---|---|---|
| `POST` | `/api/auth/login` | Login |
| `GET` | `/api/users` | Listar usuarios |
| `POST` | `/api/users` | Crear usuario |
| `GET` | `/api/employees` | Listar empleados |
| `POST` | `/api/employees` | Crear empleado |
| `GET` | `/api/skills` | Listar skills |
| `POST` | `/api/skills` | Crear skill |
| `GET` | `/api/tasks` | Listar tareas |
| `POST` | `/api/tasks` | Crear tarea |
| `PATCH` | `/api/tasks/{id}/status` | Cambiar estado |
| `POST` | `/api/planning/run` | Ejecutar planificacion |
| `GET` | `/api/planning/assignments` | Ver asignaciones |
| `GET` | `/api/dashboard/planning-summary` | Resumen |
| `GET` | `/api/my-tasks` | Mis tareas |
| `PATCH` | `/api/my-tasks/{id}/status` | Cambiar estado de mi tarea |

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## Tests

```powershell
mvn clean test
```

Tests concretos:

```powershell
mvn "-Dtest=SecurityAuthorizationTest,SecurityIntegrationTest" test
mvn "-Dtest=PlanningServiceTest,PlanningIntegrationTest" test
```

Los tests de integracion usan Testcontainers, asi que necesitan Docker Desktop arrancado.

## GitHub Actions

Workflows:

| Workflow | Archivo | Hace |
|---|---|---|
| `CI` | `.github/workflows/tests.yml` | Tests, build del JAR y artifact |
| `Docker` | `.github/workflows/docker-build.yml` | Build de imagen Docker |

El workflow de Docker publica en Docker Hub si existen estos secrets:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

Si no existen, solo construye la imagen para validar el `Dockerfile`.

## Base De Datos

PostgreSQL se levanta desde `docker-compose.yml`.

Migraciones Flyway:

```text
src/main/resources/db/migration
```

Datos demo:

```text
src/main/java/com/smartops/planner/config/DemoDataLoader.java
```

Resetear base local:

```powershell
docker compose down -v
docker compose up -d postgres
```

## Estructura

```text
src/main/java/com/smartops/planner
+-- auth
+-- user
+-- security
+-- employee
+-- skill
+-- task
+-- planning
+-- dashboard
+-- common
+-- config
+-- web
```

Interfaz:

```text
src/main/resources/static
+-- index.html
+-- css/styles.css
+-- js/
```
