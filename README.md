# SmartOps Planner

SmartOps Planner es una aplicacion Spring Boot para planificacion operativa. Permite gestionar usuarios, empleados, skills, tareas y ejecuciones de planificacion, y asigna automaticamente tareas pendientes al empleado mas adecuado segun skills, capacidad semanal, seniority, prioridad y deadline.

El proyecto incluye API REST, JWT, autorizacion por roles, PostgreSQL, migraciones Flyway, tests automatizados, Swagger/OpenAPI y una interfaz web sencilla servida desde Spring Boot.

## Funcionalidades

- Login con JWT.
- Interfaz web por rol:
  - `ADMIN`: usuarios, empleados, tareas, dashboard y planificacion.
  - `MANAGER`: empleados, tareas, dashboard y planificacion.
  - `EMPLOYEE`: solo sus tareas asignadas.
- Gestion de usuarios.
- Gestion de empleados con seniority, skills y carga semanal.
- Gestion de skills.
- Gestion de tareas con prioridad, horas estimadas, deadline, estado y skills requeridas.
- Ejecucion de planificacion automatica.
- Consulta de asignaciones y explicaciones.
- Endpoint de tareas propias para empleados.
- Dashboard de resumen, carga y estado de tareas.
- Persistencia en PostgreSQL.
- Migraciones Flyway.
- Tests de servicios, controllers, seguridad e integracion.

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- PostgreSQL
- Flyway
- Maven
- JUnit 5
- Mockito
- MockMvc
- Testcontainers
- springdoc-openapi / Swagger UI

## Estructura

El codigo esta organizado por paquetes funcionales:

```text
com.smartops.planner
+-- auth          # login y registro protegido
+-- user          # usuarios y roles
+-- security      # JWT, filtros y reglas de seguridad
+-- employee      # empleados
+-- skill         # skills
+-- task          # tareas y tareas propias
+-- planning      # planificacion, scoring y asignaciones
+-- dashboard     # resumenes operativos
+-- common        # errores y respuestas compartidas
+-- config        # datos demo y OpenAPI
+-- web           # entrada web y healthcheck
```

La interfaz esta en:

```text
src/main/resources/static
+-- index.html
+-- css/styles.css
+-- js/app.js
+-- js/api.js
+-- js/session.js
+-- js/renderers.js
+-- js/format.js
```

## Modelo De Dominio

Entidades principales:

- `User`: cuenta de acceso.
- `Role`: `ADMIN`, `MANAGER`, `EMPLOYEE`.
- `Employee`: empleado con email, seniority, skills y carga semanal.
- `Skill`: habilidad.
- `Task`: tarea con prioridad, horas estimadas, deadline, estado y skills requeridas.
- `PlanningRun`: ejecucion del planificador.
- `Assignment`: resultado de asignar o no asignar una tarea.

Estados de tarea:

```text
PENDING
ASSIGNED
IN_PROGRESS
DONE
CANCELLED
```

Prioridades:

```text
LOW
MEDIUM
HIGH
URGENT
```

## Roles Y Permisos

| Rol | Puede hacer |
|---|---|
| `ADMIN` | Gestionar usuarios, empleados, skills, tareas, dashboard y planificacion |
| `MANAGER` | Gestionar empleados, skills, tareas, dashboard y planificacion |
| `EMPLOYEE` | Consultar sus tareas asignadas y cambiar su estado |

Reglas principales:

| Endpoint | Acceso |
|---|---|
| `GET /`, `GET /api/health` | Publico |
| `POST /api/auth/login` | Publico |
| `POST /api/auth/register` | `ADMIN` |
| `/api/users/**` | `ADMIN` |
| `/api/skills/**` | `ADMIN`, `MANAGER` |
| `/api/employees/**` | `ADMIN`, `MANAGER` |
| `/api/tasks/**` | `ADMIN`, `MANAGER` |
| `/api/planning/**` | `ADMIN`, `MANAGER` |
| `/api/dashboard/**` | `ADMIN`, `MANAGER` |
| `/api/my-tasks/**` | `EMPLOYEE` |

Las contrasenas se guardan con BCrypt.

## Planificacion

El planificador evalua cada tarea `PENDING` contra los empleados disponibles. Una asignacion solo se guarda como valida si el empleado es elegible.

Bloqueos duros:

- Faltan skills requeridas.
- La tarea supera la capacidad semanal disponible del empleado.

Reglas principales de scoring:

| Regla | Efecto |
|---|---:|
| Skill requerida cumplida | `+20` |
| Skill requerida faltante | `-25` y no elegible |
| Skill extra | `+5`, maximo `+10` |
| Seniority suficiente | `+15` |
| Seniority insuficiente | `-15` |
| Tarea urgente con empleado no senior | `-30` |
| Carga menor al 60% | `+15` |
| Carga entre 60% y 80% | `+8` |
| Carga superior al 80% | `-10` |
| Capacidad suficiente | `+20` |
| Capacidad excedida | `-40` y no elegible |
| Deadline cercano con carga relevante | `-15` |

Detalle tecnico:

```text
src/main/java/com/smartops/planner/planning/ScoringService.java
src/main/java/com/smartops/planner/planning/scoring-algorithm.md
```

## Ejecucion Local

Requisitos:

- Java 21
- Maven
- Docker Desktop

Arrancar PostgreSQL:

```powershell
docker compose up -d
```

Arrancar la app en modo normal:

```powershell
mvn spring-boot:run
```

Arrancar la app en modo dev:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Abrir:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Healthcheck:

```text
http://localhost:8080/api/health
```

## Datos Demo En Dev

El perfil `dev` carga datos demo desde:

```text
src/main/java/com/smartops/planner/config/DemoDataLoader.java
```

Usuarios demo:

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
| `david.romero` | `EMPLOYEE` | `password123` |
| `elena.navarro` | `EMPLOYEE` | `password123` |
| `pablo.ruiz` | `EMPLOYEE` | `password123` |

Para reiniciar la base de datos local y recargar datos demo:

```powershell
docker compose down -v
docker compose up -d
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

## Base De Datos Y Docker

El contenedor de PostgreSQL esta definido en:

```text
docker-compose.yml
```

El proyecto tambien incluye:

```text
Dockerfile
.dockerignore
.env.example
```

Configuracion de datasource:

```text
src/main/resources/application.yaml
src/main/resources/application-dev.yaml
```

Migraciones:

```text
src/main/resources/db/migration
```

Configuracion local por defecto:

```text
database: smartops
username: smartops
password: smartops
port: 5432
```

Ver volumenes:

```powershell
docker volume ls
docker compose config
```

Levantar solo PostgreSQL para desarrollo local:

```powershell
docker compose up -d postgres
```

Levantar aplicacion + PostgreSQL con Docker Compose:

```powershell
copy .env.example .env
docker compose --profile app up --build
```

La app queda disponible en:

```text
http://localhost:8080
```

Los servicios tienen healthcheck:

- PostgreSQL usa `pg_isready`.
- La app usa `GET /api/health`.

Variables principales:

| Variable | Uso |
|---|---|
| `POSTGRES_DB` | Nombre de la base de datos |
| `POSTGRES_USER` | Usuario de PostgreSQL |
| `POSTGRES_PASSWORD` | Password de PostgreSQL |
| `POSTGRES_PORT` | Puerto local de PostgreSQL |
| `APP_PORT` | Puerto local de la app |
| `SPRING_PROFILES_ACTIVE` | Perfil activo de Spring |
| `APP_SECURITY_JWT_SECRET` | Secreto JWT |
| `APP_SECURITY_JWT_EXPIRATION_SECONDS` | Duracion del JWT |

No subas `.env` al repositorio. Usa `.env.example` como plantilla.

## Uso Basico

Flujo recomendado:

1. Entrar como `manager` o `admin`.
2. Crear o revisar empleados y skills.
3. Crear tareas pendientes.
4. Ejecutar planificacion.
5. Revisar asignaciones.
6. Entrar como `employee`.
7. Ver `Mis tareas`.
8. Cambiar una tarea a `IN_PROGRESS` o `DONE`.

## API Principal

| Metodo | Endpoint | Descripcion |
|---|---|---|
| `POST` | `/api/auth/login` | Login |
| `POST` | `/api/auth/register` | Crear usuario desde contexto admin |
| `GET` | `/api/users` | Listar usuarios |
| `POST` | `/api/users` | Crear usuario |
| `GET` | `/api/employees` | Listar empleados |
| `POST` | `/api/employees` | Crear empleado |
| `GET` | `/api/skills` | Listar skills |
| `POST` | `/api/skills` | Crear skill |
| `GET` | `/api/tasks` | Listar tareas |
| `POST` | `/api/tasks` | Crear tarea |
| `PATCH` | `/api/tasks/{id}/status` | Cambiar estado de tarea |
| `POST` | `/api/planning/run` | Ejecutar planificacion |
| `GET` | `/api/planning/assignments` | Ver asignaciones de la ultima planificacion |
| `GET` | `/api/dashboard/planning-summary` | Resumen de planificacion |
| `GET` | `/api/dashboard/workload` | Carga de empleados |
| `GET` | `/api/dashboard/task-status` | Tareas por estado |
| `GET` | `/api/my-tasks` | Tareas del empleado autenticado |
| `PATCH` | `/api/my-tasks/{id}/status` | Cambiar estado de una tarea propia |

## Ejemplos JSON

Login:

```json
{
  "username": "manager",
  "password": "password123"
}
```

Crear usuario:

```json
{
  "username": "nuevo.usuario",
  "password": "password123",
  "role": "EMPLOYEE"
}
```

Crear empleado:

```json
{
  "name": "Ana Garcia",
  "email": "ana.garcia@smartops.demo",
  "maxWeeklyHours": 40,
  "currentWeeklyHours": 10,
  "seniorityLevel": "SENIOR",
  "skillIds": [1, 2]
}
```

Crear tarea:

```json
{
  "title": "Optimizar consultas de PostgreSQL",
  "description": "Revisar queries usadas por el dashboard",
  "priority": "HIGH",
  "estimatedHours": 6,
  "deadline": "2026-06-15",
  "requiredSkillIds": [4, 5]
}
```

Cambiar estado:

```json
{
  "status": "IN_PROGRESS"
}
```

Ejecutar planificacion:

```powershell
curl -X POST http://localhost:8080/api/planning/run `
  -H "Authorization: Bearer TOKEN"
```

## Tests

Ejecutar todos los tests:

```powershell
mvn clean test
```

Ejecutar tests concretos:

```powershell
mvn "-Dtest=SecurityAuthorizationTest,SecurityIntegrationTest" test
mvn "-Dtest=PlanningServiceTest,PlanningIntegrationTest" test
```

Cobertura incluida:

- Tests de servicios.
- Tests de controllers con MockMvc.
- Tests de autorizacion por rol.
- Tests de JWT.
- Tests de integracion con PostgreSQL mediante Testcontainers.
- Test de documentacion OpenAPI.

Los tests de integracion necesitan Docker Desktop arrancado.

## Capturas

Si el README se va a enseñar en GitHub, si meteria una captura de la interfaz. Ayuda bastante a entender que no es solo una API.

Recomendacion:

```text
docs/images/dashboard.png
```

Y despues anadir en esta seccion:

```md
![Dashboard de SmartOps Planner](docs/images/dashboard.png)
```

No he dejado una imagen enlazada todavia para evitar un enlace roto en el README.

## CI/CD

Los workflows estan en:

```text
.github/workflows
```

Workflows actuales:

| Workflow | Archivo | Que hace |
|---|---|---|
| `CI` | `.github/workflows/tests.yml` | Ejecuta tests, compila el JAR y sube el artefacto |
| `Docker` | `.github/workflows/docker-build.yml` | Construye la imagen Docker con el `Dockerfile` |

El workflow `CI` se ejecuta en:

- `push`
- `pull_request`

El workflow `Docker` se ejecuta en:

- push a `main`
- ejecucion manual con `workflow_dispatch`

Para publicar imagen en Docker Hub, configura estos secrets en GitHub:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

Si esos secrets existen, el workflow publica:

```text
DOCKERHUB_USERNAME/smartops-planner:<commit-sha>
DOCKERHUB_USERNAME/smartops-planner:latest
```

Si no existen, solo construye la imagen para validar que el Dockerfile funciona.

Badge recomendado cuando el repositorio este en GitHub:

```md
![CI](https://github.com/OWNER/REPOSITORY/actions/workflows/tests.yml/badge.svg)
```

## Mejoras Futuras

- Disponibilidad por calendario.
- Niveles de dominio por skill.
- Pesos de scoring configurables.
- Filtros y paginacion en listados.
- Reasignacion manual.
- Vista historica de planificaciones.
- Capturas reales de la interfaz en `docs/images`.
