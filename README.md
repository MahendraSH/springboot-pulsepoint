# Spring Boot 3.x + PulsePoint v2 Integration & Reference Demo

A monolithic **Java 21 / Spring Boot 3.x** reference application demonstrating full reactive frontend capabilities powered by **PulsePoint v2** and **Tailwind CSS** — without Node.js, npm, Vite, Webpack, or a separate frontend build server.

---

## 🎬 Live Interactive Demo

https://github.com/MahendraSH/springboot-pulsepoint/raw/main/demo.mp4

> **Full E2E Demo Video** ([`demo.mp4`](demo.mp4)): Watch the complete 8-step interactive verification in action — including reactive RPC CRUD, instant client filtering, live Jakarta Bean Validation feedback, Server-Sent Events (SSE) progress streaming, multipart file uploads, cross-tab WebSocket synchronization, and mobile/tablet/desktop responsive reflow.

---

## 🚀 Quick Start

### 1. Prerequisites
- **Java 21+**
- **PostgreSQL** running locally on port `5432` with user `postgres` / password `postgres` (or adjust `application.yaml`).

### 2. Database Setup
Create the database and load the initial seed records (40 realistic tasks):

```bash
# Using psql (default database name configured in application.yaml)
createdb pulsepoint_db
psql -d pulsepoint_db -f schema.sql
```

*(Note: Spring Data JPA will also automatically initialize/update tables via `spring.jpa.hibernate.ddl-auto=update`. You can override database URL, credentials, or port via `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` environment variables).*

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

5. **Server-Sent Events (SSE) Streaming**:
   - Long-running audit simulation emits progressive chunks (`25%` ➔ `50%` ➔ `75%` ➔ `100%`) over `text/event-stream`.
   - Client consumes chunks via PulsePoint's `onStream`, `onStreamComplete`, and `onStreamError` callbacks, updating progress bars and audit step text in real time.

6. **Named WebSockets & Cross-Tab Collaborative Sync**:
   - Full-duplex WebSocket channel (`ws://localhost:8080/__pulsepoint/ws?name=tasks`) synchronized via `pp.socket`.
   - Automatic 25-second control frame heartbeat handling (`{"__pp": "ping"}` ➔ `{"__pp": "pong"}`) prevents 45-second connection drops.
   - Mutations (`TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED`) broadcast to all open sessions, updating multiple browser tabs instantaneously without page reloads.

7. **Multipart File Uploads with Progress**:
   - File attachment uploads handled via `StandardServletMultipartResolver` through the RPC pipeline.
   - Real-time client upload tracking via `onUploadProgress` (`loaded`, `total`, `percent`).

8. **Zero-Build Tailwind CSS & Responsive UI/UX**:
   - Clean zinc dark-mode design system with Google Fonts `Inter`, glowing indigo accents, and high-contrast status pills.
   - Loaded via CDN with custom Tailwind configuration script directly inside Thymeleaf templates — **0 npm or Node.js dependencies**.
   - Fluid responsive reflow across Mobile (375x667), Tablet (768x1024), and Desktop viewports.

9. **Reusable Java Integration Package**:
   Located in `basic.sprinng.pulsepoint.pulsepoint`:
   - `PulsePointRpcFilter`: Intercepts and routes `X-PP-RPC: true` POST requests, SSE streams, and multipart uploads.
   - `PulsePointRpcRegistry`: Thread-safe registry mapping function names to Java service methods.
   - `PulsePointCsrfFilter`: Bridges Spring Security CSRF token to `pp_csrf` cookie.
   - `PulsePointStream` & `PulsePointStreamEmitter`: Server-Sent Events streaming infrastructure.
   - `PulsePointWebSocketHandler` & `TaskBroadcaster`: Named WebSocket handling, ping/pong heartbeats, and session broadcasting.
   - `PulsePointValidationException`: Carries structured field errors formatted as `{ "error": "...", "errors": { "field": [...] } }`.

---

## 🧪 Testing & Verification

### Automated Unit & Integration Tests (22 tests)

Run the full automated test suite:

```bash
cd basic.sprinng.pulsepoint
./mvnw clean test
```

- **`PulsePointRpcFilterTest`** (9 tests): RPC dispatching, JSON argument parsing, validation error envelopes, 404 function lookups, conflict handling, SSE chunk emission, and multipart form upload handling.
- **`PulsePointWebSocketHandlerTest`** (3 tests): WebSocket session lifecycle, ping/pong 25-second control frame heartbeat handling, and broadcast to open client sessions.
- **`TaskServiceTest`** (6 tests): Database CRUD logic, status transitions, exception handling.
- **`TaskControllerTest`** (3 tests): MVC route rendering and authentication redirects.
- **`ApplicationTests`** (1 test): Spring application context health.

### Interactive Browser End-to-End Verification (8/8 PASSED)

The application was completely verified in an interactive Chromium environment across 8 operational areas:
1. **Authentication**: `/login` (user `demo`/`demo123`) ➔ authenticated session redirect to `/tasks`.
2. **Initial State & Filtering**: Initial RPC load + instant client-side filtering via `pp.state` (`TODO`, `IN PROGRESS`, `DONE`, `ALL`).
3. **Form Validation (RPC)**: Bean validation on empty and oversized titles, field-level inline errors, recovery on valid input.
4. **Status Mutation & Deletion (RPC)**: Status toggles (`TODO` ➔ `IN_PROGRESS` ➔ `DONE`) and task deletion with immediate DOM reconciliation.
5. **Server-Sent Events (SSE) Streaming**: Progressive task audit progress bar (25% ➔ 50% ➔ 75% ➔ 100%) streamed via `text/event-stream`.
6. **Multipart File Uploads**: Real-time byte upload tracking via `onUploadProgress` to 100% and confirmation alert.
7. **Multi-Tab WebSocket Live Sync**: Cross-tab real-time task synchronization over `ws://localhost:8080/__pulsepoint/ws?name=tasks` without page reloads.
8. **Tailwind CSS & Mobile Responsiveness**: Fluid layout cleanly reflows navigation, quick metrics, form controls, and task cards across mobile (375x667), tablet (768x1024), and desktop without horizontal overflow.

*Visual evidence, screenshots, and full session video recordings are preserved in [`docs/screenshots/`](docs/screenshots/) and [`demo.mp4`](demo.mp4).*

---

## 📚 Documentation & Architectural Reports

Comprehensive technical documentation, evaluation reports, issue analyses, and design blueprints are organized in the [`docs/`](docs/) directory:

| Document | Description |
|---|---|
| [**`PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md`**](docs/PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md) | **Turnkey Official Guide**: Complete setup for Spring Boot developers (Security, CSRF, RPC, SSE, WebSockets, Multipart, Zero-Build Styling). |
| [**`WALKTHROUGH.md`**](docs/WALKTHROUGH.md) | **Technical Implementation Walkthrough**: Full breakdown of the backend monolith, RPC filter, tests, and interactive E2E verification across 8 test suites. |
| [**`PULSEPOINT_JAVA_REPORT.md`**](docs/PULSEPOINT_JAVA_REPORT.md) | **Comprehensive Evaluation Report**: Executive analysis answering primary feasibility questions, 8-step verification, and comparisons with HTMX / Vaadin. |
| [**`PULSEPOINT_JAVA_ISSUES.md`**](docs/PULSEPOINT_JAVA_ISSUES.md) | **Friction & Bug Catalog**: Detailed analysis of 6 discovered issues (JSON error handling, CSRF cookie naming, Jackson serialization) with workarounds and fixes. |
| [**`PULSEPOINT_AGENTIC_TEST.md`**](docs/PULSEPOINT_AGENTIC_TEST.md) | **AI-Agent Usability Report**: Evaluation matrix tracking how an autonomous AI coding agent navigated documentation, with a 14-dimension complexity scorecard. |
| [**`PLAN_SSE_WEBSOCKET.md`**](docs/PLAN_SSE_WEBSOCKET.md) | **SSE & WebSocket Plan**: Architectural blueprint and step-by-step implementation plan for SSE & WebSockets. |
| [**`PLAN.md`**](docs/PLAN.md) | **Foundational Architecture Plan**: Initial monolithic architecture and phased design specification. |
| [**`llms.md`**](docs/llms.md) | **PulsePoint Wire Contract**: PulsePoint v2 runtime specification, hooks, directives, and wire protocols. |

> 💡 *Browse all documentation, design plans, and screenshots in the [**Documentation Hub (`docs/README.md`)**](docs/README.md).*

---

## 📁 Repository Structure

```text
springboot-pulsepoint-basic-demo/
├── README.md                               # Project overview and quick start guide
├── demo.mp4                                # Complete interactive E2E verification demo video
├── schema.sql                              # PostgreSQL database seed script (40 tasks)
├── docs/                                   # Documentation, evaluation reports, and assets
│   ├── README.md                           # Documentation hub and table of contents
│   ├── PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md # Turnkey official Spring Boot guide
│   ├── WALKTHROUGH.md                      # Technical implementation walkthrough
│   ├── PULSEPOINT_JAVA_REPORT.md           # Main architectural evaluation report
│   ├── PULSEPOINT_JAVA_ISSUES.md           # Catalog of 6 issues, root causes & fixes
│   ├── PULSEPOINT_AGENTIC_TEST.md          # AI-agent usability report & complexity scorecard
│   ├── PLAN_SSE_WEBSOCKET.md               # SSE & WebSockets design plan
│   ├── PLAN.md                             # Foundational monolithic architecture plan
│   ├── llms.md                             # PulsePoint v2 specification & wire protocol
│   └── screenshots/                        # Interactive browser E2E test captures & recordings
└── basic.sprinng.pulsepoint/               # Spring Boot Application
    ├── pom.xml                             # Maven configuration (Java 21, Spring Boot 3.x / 4.x)
    └── src/
        ├── main/
        │   ├── java/basic/sprinng/pulsepoint/
        │   │   ├── Application.java        # Spring Boot entry point
        │   │   ├── config/                 # SecurityConfig, JacksonConfig, WebSocketConfig
        │   │   ├── controller/             # TaskController (Thymeleaf views)
        │   │   ├── dto/                    # CreateTaskRequest, UpdateTaskRequest, TaskResponse
        │   │   ├── entity/                 # Task, TaskStatus, TaskPriority
        │   │   ├── exception/              # GlobalExceptionHandler, ResourceNotFoundException, ConflictException
        │   │   ├── pulsepoint/             # Reusable PulsePoint Java integration layer
        │   │   │   ├── PulsePointRpcFilter.java
        │   │   │   ├── PulsePointRpcRegistry.java
        │   │   │   ├── PulsePointRpcFunction.java
        │   │   │   ├── PulsePointCsrfFilter.java
        │   │   │   ├── exception/          # PulsePointValidationException
        │   │   │   ├── stream/             # PulsePointStream, PulsePointStreamEmitter
        │   │   │   ├── websocket/          # PulsePointWebSocketHandler, TaskBroadcaster
        │   │   │   └── handler/            # TaskRpcRegistrar
        │   │   ├── repository/             # TaskRepository (Spring Data JPA)
        │   │   └── service/                # TaskService and TaskServiceImpl
        │   └── resources/
        │       ├── application.yaml        # Database & Spring configurations (PostgreSQL)
        │       ├── static/
        │       │   ├── css/style.css       # Clean dark-mode UI styling & overrides
        │       │   └── js/pp-reactive-v2.min.js # Self-hosted PulsePoint runtime
        │       └── templates/
        │           ├── login.html          # Authentication view (Tailwind CSS styled)
        │           └── tasks.html          # PulsePoint reactive task manager component (Tailwind CSS styled)
        └── test/                           # Automated JUnit 5 tests (22 tests passing)
            └── java/basic/sprinng/pulsepoint/
                ├── ApplicationTests.java
                ├── controller/TaskControllerTest.java
                ├── service/TaskServiceTest.java
                └── pulsepoint/
                    ├── PulsePointRpcFilterTest.java
                    └── websocket/PulsePointWebSocketHandlerTest.java
```

