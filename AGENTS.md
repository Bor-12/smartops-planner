# AGENTS.md — SmartOps Planner

## Objetivo del proyecto

Este proyecto es una API backend en Spring Boot para gestión de planificación operativa: employees, skills, tasks, availability, assignments y planificación.

Codex debe trabajar de forma incremental, fase por fase, evitando cambios innecesarios.

## Reglas generales

- No reestructurar todo el proyecto salvo que sea imprescindible.
- Mantener la arquitectura por paquetes funcionales: employee, skill, task, availability, assignment, planning.
- Usar Controller + Service + Repository + Entity/DTO cuando proceda.
- No meter lógica de negocio en los controllers.
- Validar entradas con DTOs y anotaciones Jakarta Validation.
- Devolver errores mediante el GlobalExceptionHandler ya existente.
- Mantener respuestas REST coherentes y simples.
- No añadir seguridad, JWT, Docker, frontend ni cambios de infraestructura si no se pide expresamente.
- No modificar el pom.xml salvo necesidad real.

## Convenciones de API

- Endpoints REST en plural:
    - /api/employees
    - /api/skills
    - /api/tasks
    - /api/availability
    - /api/assignments
    - /api/planning

- Usar métodos HTTP correctamente:
    - GET para consultar.
    - POST para crear.
    - PUT/PATCH para actualizar.
    - DELETE para eliminar.

## Entidades y reglas de negocio

### Tasks

- Una Task debe tener:
    - title
    - description opcional
    - priority
    - estimatedHours
    - deadline
    - status inicial PENDING
    - requiredSkills

- TaskStatus debe incluir:
    - PENDING
    - IN_PROGRESS
    - DONE
    - CANCELLED

- La prioridad debe ser clara, por ejemplo:
    - LOW
    - MEDIUM
    - HIGH
    - CRITICAL

## Tests

- Separar tests por capa cuando tenga sentido:
    - Controller tests para endpoints REST.
    - Service tests para lógica de negocio.
    - Repository tests solo si aportan valor.

- No hacer tests gigantes ni frágiles.
- Usar MockMvc solo para probar controllers REST.
- Mockear dependencias del service cuando se testee controller.
- Mockear repositories cuando se testee service.
- No levantar toda la aplicación si no es necesario.

## Forma de trabajar

Antes de cambiar código:

1. Revisar la estructura actual.
2. Identificar clases ya existentes.
3. Reutilizar patrones del proyecto.
4. Hacer cambios pequeños y coherentes.
5. Explicar brevemente qué se ha cambiado.

Después de cambiar código:

1. Indicar archivos modificados.
2. Indicar comandos para probar.
3. Avisar si hay algo pendiente o dudoso.