## Project Overview

This is a monolithic Java Spring Boot application integrating PulsePoint.

The end goal is not just a Spring Boot CRUD application, but to demonstrate a **reusable PulsePoint ↔ Java Spring Boot integration**, built and tested with an AI coding agent.

The project is built incrementally in phases.

### Final Target Architecture

```text
                    Browser
                       │
                  PulsePoint
                       │
        ┌──────────────┼──────────────┐
        │              │              │
       RPC            SSE         WebSocket
        │              │              │
        └──────────────┼──────────────┘
                       ↓
              Spring Boot Monolith
                       ↓
                 Spring Security
              ┌────────┴────────┐
              │                 │
        Authentication      Authorization
              │                 │
              └────────┬────────┘
                       ↓
                 Service Layer
                       ↓
                  Repository
                       ↓
                  PostgreSQL
```

### Application Scope & Components

1. **Spring Boot Monolith**: User management, Task management, PostgreSQL, JPA, Service interfaces + implementations, DTOs, Bean Validation, Centralized Exception Handling.
2. **Security**: Session authentication, Password hashing, `ADMIN` / `USER` roles, Server-side authorization, CSRF protection.
3. **PulsePoint Integration**: Reusable integration layer for:
   - **RPC** (Phase 2 - first and core phase)
   - **SSE / Streaming** (Phase 3)
   - **WebSocket** (Phase 4)
   - Authentication & Authorization integration with Spring Security
   - CSRF handling
   - Validation & Error handling
4. **AI-Agent Evaluation**: Evaluating how well the AI coding agent implements the architecture, documenting failures, human interventions, and agentic testing results.

### Project Phases Roadmap

```text
PHASE 1: Spring Boot + Task + User + Security
                   ↓
PHASE 2: PulsePoint RPC
                   ↓
PHASE 3: PulsePoint SSE / Streaming
                   ↓
PHASE 4: PulsePoint WebSocket
                   ↓
PHASE 5: Security + CSRF + Validation + Error/Edge cases
                   ↓
PHASE 6: Full testing + AI-agent review/fixes
                   ↓
PHASE 7: Final documentation/report
```

---

# Technology

Use:

- Java 21
- Spring Boot 3.x
- Maven
- Spring MVC
- Spring Data JPA
- Hibernate
- PostgreSQL
- Spring Security
- Thymeleaf
- Jakarta Bean Validation
- Jackson
- JUnit 5
- Mockito where required

Do not introduce unnecessary frameworks.

Do not use:
- React
- Vue
- Angular
- Next.js
- Vite
- Node.js frontend

---

# Architecture

Use a clean monolithic layered architecture.

```text
Controller / PulsePoint Handler
            ↓
      Service Interface
            ↓
      Service Implementation
            ↓
         Repository
            ↓
        PostgreSQL
````

Business logic must remain inside the service layer.

PulsePoint must use the existing service layer.

Do not duplicate business logic for PulsePoint.

---

# Package Structure

Use this structure:

```text
src/main/java/com/example/taskapp/

├── config/
│
├── controller/
│
├── dto/
│   ├── auth/
│   ├── task/
│   └── user/
│
├── entity/
│
├── repository/
│
├── service/
│   └── impl/
│
├── security/
│
├── exception/
│
├── util/
│
├── constant/
│
└── pulsepoint/
    ├── handler/
    ├── dto/
    ├── exception/
    ├── security/
    └── support/
```

Keep package responsibilities clear.

Do not create unnecessary packages.

Do not create generic dumping-ground packages.

---

# Service Architecture

Every business service must have an interface.

Example:

```text
service/
    TaskService.java

service/impl/
    TaskServiceImpl.java
```

Implementation:

```java
@Service
public class TaskServiceImpl implements TaskService
```

Rules:

* Interfaces belong in `service`
* Implementations belong in `service.impl`
* `@Service` belongs on implementations
* Controllers depend on service interfaces
* Repositories must not be injected directly into controllers
* Use constructor injection

Do not create interfaces for classes where an abstraction provides no value.

---

# Controller

Controllers must remain thin.

Controllers may:

* receive requests
* validate DTOs
* call services
* return responses

Controllers must not contain:

* business logic
* database logic
* complex security logic
* duplicated validation logic

---

# Repository

Repositories are responsible only for persistence.

Use Spring Data JPA.

Repositories must not contain business logic.

The normal flow must be:

```text
Controller
    ↓
Service
    ↓
Repository
```

Never:

```text
Controller
    ↓
Repository
```

---

# DTO

Do not expose JPA entities directly to clients.

Use DTOs for requests and responses.

Organize DTOs by domain:

```text
dto/
├── auth/
├── task/
└── user/
```

Use Bean Validation annotations where appropriate.

---

# Entities

The application contains at least:

```text
User
Task
```

Use an enum for roles:

```text
ADMIN
USER
```

Task should support appropriate:

* title
* description
* status
* priority
* createdBy
* assignedTo
* createdAt
* updatedAt

Use appropriate JPA relationships.

Passwords must never be stored as plaintext.

---

# Authentication

Use Spring Security session-based authentication.

Implement:

* login
* logout
* session authentication
* password hashing
* authentication failure handling
* authentication entry point
* access denied handling
* current authenticated user resolution

Do not use JWT unless explicitly required later.

---

# Authorization

Support:

```text
ROLE_ADMIN
ROLE_USER
```

Authorization must always be enforced server-side.

Do not rely on:

* hidden UI buttons
* JavaScript checks
* PulsePoint state
* client-side validation

A user must not be able to bypass authorization by manually sending a request.

---

# Task Authorization

Users must only perform operations they are authorized to perform.

ADMIN may perform administrative operations.

USER may perform normal permitted task operations.

Task ownership/assignment rules must be enforced in the backend.

Do not rely on the UI to enforce permissions.

---

# CSRF

CSRF protection must remain enabled.

Never disable CSRF simply to make development or PulsePoint integration easier.

The future PulsePoint RPC implementation must integrate with the existing CSRF protection.

CSRF failures must be handled cleanly.

---

# Security Package

Keep reusable security functionality under:

```text
security/
```

Examples:

```text
CustomUserDetailsService
SecurityUser
AuthenticationEntryPoint
AccessDeniedHandler
SecurityContextHelper
```

Avoid duplicating SecurityContext access logic throughout the application.

---

# Configuration

Spring configuration belongs under:

```text
config/
```

Examples:

```text
SecurityConfig
PasswordConfig
```

Do not place configuration logic inside controllers or services.

---

# Exception Handling

Use centralized exception handling.

Create:

```text
exception/GlobalExceptionHandler.java
```

Use meaningful exceptions such as:

```text
ResourceNotFoundException
UnauthorizedException
ForbiddenException
ConflictException
```

Return consistent error responses.

Never expose:

* stack traces
* database internals
* passwords
* sensitive security information

---

# Utility Package

Use:

```text
util/
```

for genuinely reusable stateless utilities.

Possible utilities:

```text
SecurityUtils
ValidationUtils
DateTimeUtils
```

Do not put business logic inside utilities.

Do not turn `util` into a miscellaneous dumping ground.

If functionality belongs to a service, keep it in the service.

If functionality belongs to security, keep it in `security`.

---

# Constants

Use:

```text
constant/
```

for genuinely application-wide constants.

Prefer enums for finite values such as:

```text
Role
TaskStatus
TaskPriority
```

Do not create unnecessary constant classes.

---

# Dependency Injection

Always use constructor injection.

Do not use field injection.

Example:

```java
private final TaskService taskService;

public TaskController(TaskService taskService) {
    this.taskService = taskService;
}
```

---

# Code Quality

Follow:

* SOLID principles
* separation of concerns
* single responsibility
* dependency inversion
* clean naming
* low duplication
* high cohesion
* low coupling
* testability

Keep classes focused.

Keep methods focused.

Avoid deeply nested logic.

Avoid unnecessary abstractions.

---

# Self-Explanatory Code

Code should explain itself through:

* meaningful class names
* meaningful method names
* meaningful variable names
* clear package structure
* appropriate types
* small focused methods

Avoid unnecessary comments.

Do not write comments explaining obvious code.

Bad:

```java
// Get task by ID
Task task = taskService.findById(id);
```

Good:

```java
Task task = taskService.findById(id);
```

Use comments/Javadoc only when explaining:

* non-obvious behavior
* security-sensitive decisions
* complex algorithms
* framework limitations
* important architectural decisions
* PulsePoint-specific behavior

---

# No Duplicate Logic

Before creating new logic:

1. Check whether it already exists.
2. Reuse the existing service.
3. Reuse existing utilities where appropriate.
4. Do not copy business rules.
5. Keep one source of truth.

For example:

```text
REST
  ↓
TaskService
```

and:

```text
PulsePoint RPC
  ↓
TaskService
```

Both must use the same business logic.

---

# PulsePoint Architecture

PulsePoint is an important part of this project.

The integration must be reusable and isolated.

Use:

```text
pulsepoint/
├── handler/
├── dto/
├── exception/
├── security/
└── support/
```

The exact classes should be based on the actual PulsePoint documentation.

Potential components may include:

```text
PulsePointRpcHandler
PulsePointRequest
PulsePointResponse
PulsePointExceptionHandler
PulsePointCsrfSupport
PulsePointStreamSupport
PulsePointWebSocketSupport
```

Do not create these blindly.

Use the PulsePoint documentation and [llms.md](llms.md) as the source of truth.

Do not invent undocumented APIs.

---

# PulsePoint RPC

RPC is the first PulsePoint integration to implement.

The RPC layer must:

* receive PulsePoint RPC requests
* identify the requested operation
* validate input
* use existing services
* return appropriate responses
* handle errors
* preserve authentication
* preserve authorization
* preserve CSRF protection

Example:

```text
PulsePoint RPC
      ↓
PulsePointRpcHandler
      ↓
TaskService
      ↓
TaskServiceImpl
      ↓
TaskRepository
      ↓
PostgreSQL
```

Do not put Task business logic inside `PulsePointRpcHandler`.

---

# PulsePoint Authentication

PulsePoint must work with the existing Spring Security authentication.

Do not create a separate authentication system for PulsePoint.

Use the existing authenticated session.

Authentication remains controlled by Spring Security.

---

# PulsePoint Authorization

PulsePoint requests must go through the same server-side authorization rules.

For example:

```text
USER
 ↓
deleteTask
 ↓
Spring Security / authorization
 ↓
Rejected if not permitted
```

Do not rely on PulsePoint/client-side checks.

---

# PulsePoint CSRF

Do not disable CSRF for PulsePoint.

Determine the correct PulsePoint-compatible CSRF mechanism from the documentation.

The integration must preserve Spring Security CSRF protection.

Document or test any PulsePoint-specific CSRF behavior.

---

# PulsePoint SSE

SSE/streaming is implemented in a later phase.

When implemented:

* reuse existing services
* preserve authentication
* preserve authorization
* handle lifecycle correctly
* handle errors
* clean up resources
* do not break RPC

---

# PulsePoint WebSocket

WebSocket is implemented in a later phase.

When implemented:

* preserve authentication
* preserve authorization
* handle connection lifecycle
* handle messages
* handle errors
* clean up resources
* reuse existing services
* do not duplicate business logic
* do not break RPC or SSE

---

# Testing

Test important behavior rather than only happy paths.

Include:

* service tests
* controller tests where appropriate
* security tests
* integration tests
* PulsePoint integration tests

Security tests must include:

* unauthenticated access
* successful login
* invalid login
* logout
* ADMIN access
* USER access
* unauthorized operation
* ownership restrictions
* missing CSRF
* invalid CSRF
* valid CSRF

PulsePoint tests should include:

* valid RPC
* invalid RPC
* validation errors
* authentication
* authorization
* CSRF
* service failures
* malformed requests
* error responses

---

# AI Agent Rules

Use AI coding agents to implement the project.

Before creating code:

1. Inspect the existing project.
2. Read `AGENTS.md`.
3. Read the relevant PulsePoint documentation.
4. Understand existing architecture.
5. Reuse existing code where appropriate.

Do not rewrite working code unnecessarily.

Do not create duplicate classes.

Do not introduce dependencies without a reason.

Do not invent APIs.

Do not silently work around undocumented PulsePoint behavior.

If something is unclear:

* inspect the provided documentation
* inspect existing code
* test the behavior
* then implement the smallest appropriate solution

---

# Phase Rules

The project is implemented in phases.

A phase prompt defines what must be implemented now.

Do not implement future phases unless explicitly requested.


Do not implement SSE or WebSocket while working on the RPC phase unless explicitly instructed.

---

# Completion Requirements

A phase is not complete simply because code was generated.

Before finishing a phase:

1. Compile the project.
2. Run tests.
3. Fix compilation errors.
4. Fix failing tests.
5. Check security.
6. Check authorization.
7. Check CSRF.
8. Check package structure.
9. Check for duplicated logic.
10. Check for unused code.
11. Review the implementation against this file.

The implementation must be clean and maintainable before declaring the phase complete.


