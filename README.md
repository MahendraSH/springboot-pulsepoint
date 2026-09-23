# Spring Boot 3.x + PulsePoint v2 Integration & Reference Demo

A monolithic **Java 21 / Spring Boot 3.x** reference application demonstrating full reactive frontend capabilities powered by **PulsePoint v2** — without Node.js, npm, Vite, Webpack, or a separate frontend build server.

---

## 🚀 Quick Start

### 1. Prerequisites
- **Java 21+**
- **PostgreSQL** running locally on port `5432` with user `postgres` / password `postgres` (or adjust `application.properties`).

### 2. Database Setup
Create the database and load the initial seed records (40 realistic tasks):

```bash
# Using psql
createdb taskdb
psql -d taskdb -f schema.sql
```

*(Note: Spring Data JPA will also automatically initialize/update tables via `spring.jpa.hibernate.ddl-auto=update`)*

### 3. Run the Application

```bash
cd basic.sprinng.pulsepoint
./mvnw spring-boot:run
```

Once started, open your browser at:
**[http://localhost:8080](http://localhost:8080)**

**Demo Login Credentials:**
- **Username:** `demo`
- **Password:** `demo123`

---

## 📋 What This Project Demonstrates

1. **Monolithic Architecture**:
   Spring Boot serves the initial HTML via Thymeleaf and handles all business logic and database persistence. PulsePoint runs directly in the browser via a static minified script (`/js/pp-reactive-v2.min.js`), giving instant single-page reactivity with zero Node.js build step.

2. **Full Reactive CRUD via PulsePoint RPC**:
   - **List:** Reads tasks on load via `pp.rpc("listTasks")`.
   - **Create:** Adds tasks reactively via `pp.rpc("createTask", payload)`.
   - **Update:** Transitions status (`TODO` ➔ `IN_PROGRESS` ➔ `DONE`) via `pp.rpc("updateTask", ...)`.
   - **Delete:** Deletes tasks via `pp.rpc("deleteTask", { id })` with immediate DOM reconciliation.
   - **Client-Side Filtering:** Filters tasks by status instantaneously using `pp.state` with zero server reloads.

3. **Jakarta Bean Validation & Field-Level Errors**:
   - Empty title is caught by `@NotBlank` and flagged both in an alert banner and inline beneath the field.
   - Overly long titles (>200 chars) are caught by `@Size(max = 200)`.
   - Valid inputs clear errors reactively and reset the form.

4. **Spring Security & CSRF Bridge**:
   - Preserves Spring Security session-based authentication.
   - Reusable `PulsePointCsrfFilter` provides the `pp_csrf` cookie expected by PulsePoint.
   - `CookieCsrfTokenRepository` validates the incoming `X-CSRF-Token` header on all RPC mutations.

5. **Reusable Java Integration Package**:
   Located in `basic.sprinng.pulsepoint.pulsepoint`:
   - `PulsePointRpcFilter`: Intercepts and routes `X-PP-RPC: true` POST requests.
   - `PulsePointRpcRegistry`: Thread-safe registry mapping function names to Java service methods.
   - `PulsePointValidationException`: Carries structured field errors formatted as `{ "error": "...", "errors": { "field": [...] } }`.

---

## 🧪 Testing & Verification

### Automated Unit & Integration Tests (17 tests)

Run the full automated test suite:

```bash
cd basic.sprinng.pulsepoint
./mvnw clean test
```

- **`PulsePointRpcFilterTest`** (7 tests): RPC dispatching, JSON argument parsing, validation error envelopes, 404 function lookups, conflict handling.
- **`TaskServiceTest`** (6 tests): Database CRUD logic, status transitions, exception handling.
- **`TaskControllerTest`** (3 tests): MVC route rendering and authentication redirects.
- **`ApplicationTests`** (1 test): Spring application context health.

### Browser End-to-End Verification

The application was verified with live browser automation in an interactive Chromium environment:
- **Full CRUD Lifecycle**: Verified creation, status updates, client filtering, and deletion without page reloads.
- **Validation Suite**: Verified `@NotBlank`, `@Size` limits, inline error bindings, and form recovery.

---

## 📚 Evaluation & Documentation Reports

Detailed architectural findings, issue catalog, and evaluation scorecards are included in the repository:

| Document | Description |
|---|---|
| [**`WALKTHROUGH.md`**](WALKTHROUGH.md) | Technical walkthrough of the architecture, components, and verification results. |
| [**`PULSEPOINT_JAVA_REPORT.md`**](PULSEPOINT_JAVA_REPORT.md) | Executive evaluation report answering key questions, comparing with HTMX/Vaadin, and assessing feasibility. |
| [**`PULSEPOINT_JAVA_ISSUES.md`**](PULSEPOINT_JAVA_ISSUES.md) | Catalog of 6 discovered friction points & bugs with reproduction steps, root causes, workarounds, and proposed fixes. |
| [**`PULSEPOINT_AGENTIC_TEST.md`**](PULSEPOINT_AGENTIC_TEST.md) | AI-agent usability evaluation matrix and a 14-dimension complexity assessment scorecard (scored 1–10). |
| [**`PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md`**](PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md) | Turnkey official documentation guide for Spring Boot developers integrating PulsePoint v2. |

---

## 📁 Repository Structure

```text
springboot-pulsepoint-basic-demo/
├── README.md                               # Project overview and quick start guide
├── WALKTHROUGH.md                          # Implementation walkthrough
├── PULSEPOINT_JAVA_REPORT.md               # Main architectural evaluation report
├── PULSEPOINT_JAVA_ISSUES.md               # Catalog of 6 issues & workarounds
├── PULSEPOINT_AGENTIC_TEST.md              # AI-agent usability report & complexity scorecard
├── PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md # Proposed official Spring Boot guide
├── schema.sql                              # PostgreSQL database seed script
└── basic.sprinng.pulsepoint/               # Spring Boot Application
    ├── pom.xml                             # Maven configuration (Java 21, Spring Boot 3.x)
    └── src/
        ├── main/
        │   ├── java/basic/sprinng/pulsepoint/
        │   │   ├── config/                 # SecurityConfig, JacksonConfig
        │   │   ├── controller/             # TaskController (Thymeleaf views)
        │   │   ├── dto/                    # CreateTaskRequest, UpdateTaskRequest, TaskResponse
        │   │   ├── entity/                 # Task, TaskStatus, TaskPriority
        │   │   ├── exception/              # GlobalExceptionHandler, ConflictException, etc.
        │   │   ├── pulsepoint/             # Reusable PulsePoint Java integration layer
        │   │   │   ├── PulsePointRpcFilter.java
        │   │   │   ├── PulsePointRpcRegistry.java
        │   │   │   ├── PulsePointCsrfFilter.java
        │   │   │   ├── PulsePointValidationException.java
        │   │   │   └── handler/TaskRpcRegistrar.java
        │   │   ├── repository/             # TaskRepository (Spring Data JPA)
        │   │   └── service/                # TaskService and TaskServiceImpl
        │   └── resources/
        │       ├── application.properties  # Database & Spring configurations
        │       ├── static/
        │       │   ├── css/style.css       # Clean dark-mode UI styling
        │       │   └── js/pp-reactive-v2.min.js # Self-hosted PulsePoint runtime
        │       └── templates/
        │           ├── login.html          # Authentication view
        │           └── tasks.html          # PulsePoint reactive task manager component
        └── test/                           # Automated JUnit 5 tests
```
