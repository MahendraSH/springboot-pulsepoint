# Spring Boot + PulsePoint v2 Integration Walkthrough

## Overview

We have built and validated a monolithic Java Spring Boot application integrating **PulsePoint v2** reactive frontend layer with no separate Node.js / Vite build step or frontend server.

```
Browser (PulsePoint v2 runtime)
   │
   │  RPC (POST + X-PP-RPC: true, X-PP-Function, X-CSRF-Token)
   ▼
Spring Boot Application Monolith
   │
   ├─► PulsePointCsrfFilter (sets pp_csrf cookie)
   ├─► Spring Security (validates X-CSRF-Token & session auth)
   ├─► PulsePointRpcFilter (intercepts and dispatches RPC functions)
   │     └─► TaskRpcRegistrar
   │           └─► TaskService (Business Layer)
   │                 └─► TaskRepository (Spring Data JPA)
   │                       └─► PostgreSQL
   └─► Thymeleaf (serves initial HTML host template)
```

---

## What Was Implemented

### 1. Spring Boot Monolithic Backend
- **Entities & Schema:** [`Task.java`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/entity/Task.java) with `TaskStatus` and `TaskPriority` enums, timestamps, and JPA lifecycle hooks.
- **Service Layer:** Interface [`TaskService`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/service/TaskService.java) and implementation [`TaskServiceImpl`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/service/impl/TaskServiceImpl.java) using constructor injection.
- **DTOs:** [`CreateTaskRequest`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/dto/CreateTaskRequest.java), [`UpdateTaskRequest`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/dto/UpdateTaskRequest.java), [`TaskResponse`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/dto/TaskResponse.java).
- **Security:** [`SecurityConfig`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/config/SecurityConfig.java) with session auth, in-memory user (`demo`/`demo123`), and JSON-aware exception handling for `X-PP-RPC` requests.

### 2. PulsePoint v2 Wire Protocol Integration
- **Function Registry:** [`PulsePointRpcRegistry`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/PulsePointRpcRegistry.java) allows registering named RPC handlers safely without dynamic code evaluation.
- **Wire Protocol Filter:** [`PulsePointRpcFilter`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/PulsePointRpcFilter.java):
  - Intercepts `POST` requests where `X-PP-RPC: true`
  - Extracts the requested operation from `X-PP-Function`
  - Parses JSON arguments and invokes the registered handler
  - Returns `application/json` with the exact payload format PulsePoint expects (including RFC-compliant `{ "error": "...", "errors": { ... } }` error envelopes).
- **CSRF Bridge:** [`PulsePointCsrfFilter`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/PulsePointCsrfFilter.java) + `CookieCsrfTokenRepository.setCookieName("pp_csrf")` exposes the token for the client JS to send as `X-CSRF-Token`.
- **RPC Registrar:** [`TaskRpcRegistrar`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/handler/TaskRpcRegistrar.java) exposes `listTasks`, `getTask`, `createTask`, `updateTask`, `deleteTask`.

### 3. Reactive UI
- [`tasks.html`](basic.sprinng.pulsepoint/src/main/resources/templates/tasks.html):
  - Self-hosted runtime loaded from [`/js/pp-reactive-v2.min.js`](basic.sprinng.pulsepoint/src/main/resources/static/js/pp-reactive-v2.min.js)
  - `<template pp-component="task_manager">` boundary
  - `pp.state([])`, `pp.effect(...)`
  - Reactive list iteration with `<template pp-for="task in filteredTasks">`
  - Single-page feel without page reloads.

---

## Verification Results

### Automated Tests
Ran 17 automated tests via Maven Surefire:
- `PulsePointRpcFilterTest`: 7 tests passing (dispatches, payload parsing, validation errors, 404 handling, parameter extraction)
- `TaskServiceTest`: 6 tests passing (CRUD, mappings, exceptions)
- `TaskControllerTest`: 3 tests passing (view routing, redirects)
- `ApplicationTests`: 1 test passing (context load)

### Live PostgreSQL End-to-End
- Seeded with 40 records via [`schema.sql`](schema.sql)
- Loaded reactively via `listTasks` RPC
- Inserted tasks reactively via `createTask` RPC
- Validated error states and JSON error responses.

### End-to-End Browser Automation Test
- **Test Runner:** Antigravity IDE Browser Test Suite (interactive Chromium engine).
- **Navigation & Authentication:** `/login` with `demo` / `demo123` -> redirected to `/tasks` under authenticated session.
- **Task Creation via RPC:** Submitted new task `#46` (`Automated Browser E2E Task`), count incremented 45 -> 46 without full page reload.
- **Status Mutation via RPC:** Changed `#46` from `TODO` to `IN_PROGRESS` via `pp.rpc("updateTask", ...)` -> badge updated reactively with zero page flicker.
- **Client-Side Filtering:** Tested tabs (`TODO`, `In Progress`, `Done`, `All`) -> rendered dynamically via `pp.state` filter.
- **Task Deletion via RPC:** Executed `deleteTask` -> element removed from DOM reactively, task count returned to 45.
- **Full Session Recording:** Captured in `task_full_e2e_test_1790150651004.webp`.

### Form Validation & Error Handling Browser Test
- **Empty Title Validation:** Cleared title input and clicked "Add Task via RPC" ➔ Top alert displayed `"Validation error: Title is required"`, inline error beneath field displayed `"Title is required"` (**PASS**).
- **Overly Long Title (>200 chars):** Submitted 210-character title ➔ Top alert and inline error displayed `"Title must not exceed 200 characters"` (**PASS**).
- **Recovery with Valid Task:** Submitted `"Validation Test Task"` (ID #49) ➔ Red error banners cleared immediately, green success notification displayed, task dynamically prepended (**PASS**).
- **Validation Session Recording:** Captured in `task_validation_tests_1790152840219.webp`.

---

## Reusable Java Integration Findings for PulsePoint

| Topic | Finding / Best Practice |
|---|---|
| **CSRF Handling** | Configure Spring Security's `CookieCsrfTokenRepository.withHttpOnlyFalse()` with `setCookieName("pp_csrf")` and `setHeaderName("X-CSRF-Token")`. |
| **Error Format** | When an RPC fails, Spring Security & exception handlers must return `{ "error": "...", "errors": {} }` with `Content-Type: application/json` rather than standard Spring Boot HTML error pages. |
| **Thymeleaf Compatibility** | Thymeleaf's `${...}` syntax does not collide with PulsePoint's `{...}` bindings. However, template tags with `pp-for` should avoid dynamic `th:*` attributes that evaluate at server render time. |
| **Response Flushing** | Servlet filters writing JSON directly to `response.getWriter()` must call `.flush()` before completing to ensure client receives the complete JSON stream. |
