# Spring Boot + PulsePoint v2 Integration Walkthrough

## Overview

We have built and validated a monolithic Java Spring Boot application integrating **PulsePoint v2** reactive frontend layer with no separate Node.js / Vite build step or frontend server.

```
Browser (PulsePoint v2 runtime)
   │
   ├─► RPC (POST + X-PP-RPC: true, X-PP-Function, X-CSRF-Token)
   ├─► SSE Streaming (POST + X-PP-RPC: true + Accept: text/event-stream)
   └─► WebSocket (ws://host/__pulsepoint/ws?name=tasks, 25s ping/pong)
   ▼
Spring Boot Application Monolith
   │
   ├─► PulsePointCsrfFilter (sets pp_csrf cookie)
   ├─► Spring Security (validates X-CSRF-Token & session auth)
   ├─► PulsePointRpcFilter (intercepts and dispatches RPC functions & SSE streams)
   │     ├─► TaskRpcRegistrar
   │     │     ├─► TaskService (Business Layer)
   │     │     │     └─► TaskRepository (Spring Data JPA) ──► PostgreSQL
   │     │     ├─► PulsePointStream (Server-Sent Events)
   │     │     └─► TaskBroadcaster (Dispatches to WebSockets)
   ├─► WebSocketConfig & PulsePointWebSocketHandler (ws sessions & heartbeat)
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
  - Parses JSON arguments or multipart form uploads
  - Detects streaming returns (`PulsePointStream`) and switches output to `Content-Type: text/event-stream;charset=UTF-8`
  - Returns `application/json` with the exact payload format PulsePoint expects (including RFC-compliant `{ "error": "...", "errors": { ... } }` error envelopes).
- **CSRF Bridge:** [`PulsePointCsrfFilter`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/PulsePointCsrfFilter.java) + `CookieCsrfTokenRepository.setCookieName("pp_csrf")` exposes the token for the client JS to send as `X-CSRF-Token`.
- **RPC Registrar:** [`TaskRpcRegistrar`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/handler/TaskRpcRegistrar.java) exposes `listTasks`, `getTask`, `createTask`, `updateTask`, `deleteTask`, `streamTaskAudit`, and `uploadTaskAttachment`.

### 3. Server-Sent Events (SSE) Streaming (Phase 3)
- [`PulsePointStreamEmitter.java`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/stream/PulsePointStreamEmitter.java) flushes SSE chunks immediately over the HTTP response stream in format `data: <json>\n\n`.
- Registered `streamTaskAudit`: emits live audit steps (`25%`, `50%`, `75%`, `100%`) for long-running task operations.
- Client consumed seamlessly via PulsePoint's `pp.rpc("streamTaskAudit", { taskId }, { onStream, onStreamComplete, onStreamError })`.
- Reactive progress bar (`{auditPercent}%`) and status indicator (`{auditStep}`) update with zero page flicker.

### 4. Named WebSockets & Collaborative Live Sync (Phase 4)
- [`WebSocketConfig.java`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/config/WebSocketConfig.java) maps `/__pulsepoint/ws`.
- [`PulsePointWebSocketHandler.java`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/websocket/PulsePointWebSocketHandler.java):
  - Inspects query param `name` (e.g. `/__pulsepoint/ws?name=tasks`).
  - Handles PulsePoint's 25-second control frame heartbeat: when client sends `{"__pp": "ping"}`, backend immediately responds `{"__pp": "pong"}` to prevent 45-second connection drops.
- [`TaskBroadcaster.java`](basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/websocket/TaskBroadcaster.java):
  - Connected sessions receive real-time JSON frames: `TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED`.
- Multiple browser tabs synchronize instantaneously without polling or manual refreshes.
- Live connection indicator: `● Live Sync: {liveSyncStatus}`.

### 5. Multipart File Uploads & XSS Safety (Phase 5)
- `PulsePointRpcFilter` supports `multipart/form-data` decoding via `StandardServletMultipartResolver`.
- Registered `uploadTaskAttachment` accepting file attachments with live upload tracking via PulsePoint's `onUploadProgress({ loaded, total, percent })`.
- XSS verification: verified that client HTML template interpolation safely escapes `<script>` tags as text nodes.

---

## Verification Results

### Automated Tests (22 tests passing)
Ran 22 automated tests via Maven Surefire:
- `PulsePointRpcFilterTest`: **9 tests passing** (dispatches, payload parsing, validation errors, 404 handling, parameter extraction, SSE streaming chunk delivery, multipart file upload processing)
- `PulsePointWebSocketHandlerTest`: **3 tests passing** (session registration, ping/pong heartbeat control frame reply, broadcast to open sessions)
- `TaskServiceTest`: **6 tests passing** (CRUD, mappings, exceptions)
- `TaskControllerTest`: **3 tests passing** (view routing, redirects)
- `ApplicationTests`: **1 test passing** (context load)

### Live PostgreSQL End-to-End
- Seeded with 40 records via [`schema.sql`](schema.sql)
- Loaded reactively via `listTasks` RPC
- Inserted tasks reactively via `createTask` RPC
- Validated error states and JSON error responses.

### End-to-End Interactive Verification Suite (7/7 PASSED)

The full interactive browser test suite was executed in an interactive Chromium engine on `http://localhost:8080`, exercising the entire monolithic architecture:

| # | Feature / Test Case | Actions & Triggers | Observations & Result | Status |
|---|---------------------|--------------------|------------------------|:------:|
| **1** | **Authentication** | Navigated to `/login`, authenticated with `demo` / `demo123`. | Auto-redirected to `/tasks`. Header displayed `User: DEMO`. | **PASS** |
| **2** | **Initial State & Filtering** | Inspected top badges and clicked `TODO`, `IN PROGRESS`, `DONE`, `ALL`. | Initial tasks loaded via RPC. `● Live Sync: Connected` active. Client-side filtering operated instantly without network round-trips. | **PASS** |
| **3** | **Validation & Error Handling (RPC)** | Submitted empty title, then 210-character title, then `"AGY IDE E2E Test Task"`. | Server Bean Validation rejected invalid inputs with top red banner and inline warnings. Valid task cleared errors and prepended to DOM. | **PASS** |
| **4** | **Status Mutation & Deletion (RPC)** | Toggled `"AGY IDE E2E Test Task"` to `IN PROGRESS` then `DONE`. Clicked `Delete`. | Status badge mutated dynamically with zero page flicker. Task removed from DOM and total counter decremented upon deletion. | **PASS** |
| **5** | **Server-Sent Events (SSE) Streaming** | Clicked `⚡ Audit (SSE)` on Task #1. | Real-time card appeared, streaming progress (25% → 50% → 75% → 100%) and step messages via `text/event-stream`. | **PASS** |
| **6** | **Multipart File Upload with Progress** | Clicked `📎 Attach`, selected `test-attachment.txt`. | Upload progress bar tracked upload chunks via `onUploadProgress`, finishing at 100% with green success notice. | **PASS** |
| **7** | **Multi-Tab WebSocket Live Sync** | Opened Tab 2. Created `"WebSocket Sync Task"` in Tab 1. Checked Tab 2. | Tab 2 dynamically received `TASK_CREATED` over `ws://` and rendered the new task without any manual reload. | **PASS** |

#### Visual Evidence & Snapshots

- **Step 3 (Form Validation & Dynamic Creation):**
  - Empty title validation: [`docs/screenshots/empty_title_validation_1790160129534.png`](docs/screenshots/empty_title_validation_1790160129534.png)
  - Valid task created: [`docs/screenshots/valid_task_creation_1790160337821.png`](docs/screenshots/valid_task_creation_1790160337821.png)

- **Step 4 (Status Mutation & Reactive Deletion):**
  - Status mutation and deletion: [`docs/screenshots/status_mutation_and_deletion_1790161888799.png`](docs/screenshots/status_mutation_and_deletion_1790161888799.png)

- **Step 5 (Real-Time SSE Audit Streaming):**
  - Sequential audit stream: [`docs/screenshots/sse_audit_stream_1790162251926.png`](docs/screenshots/sse_audit_stream_1790162251926.png)

- **Step 6 (Multipart Upload with Progress Tracking):**
  - Byte transmission and completion: [`docs/screenshots/file_upload_progress_1790162452519.png`](docs/screenshots/file_upload_progress_1790162452519.png)

- **Step 7 (Multi-Tab WebSocket Live Sync):**
  - Multi-tab reactive sync: [`docs/screenshots/websocket_live_sync_1790163508917.png`](docs/screenshots/websocket_live_sync_1790163508917.png)

#### Full Interactive Session Recording
- Complete browser recording: [**`full_interactive_verification_1790159186402.webp`**](docs/screenshots/full_interactive_verification_1790159186402.webp)

---

## Reusable Java Integration Findings for PulsePoint

| Topic | Finding / Best Practice |
|---|---|
| **CSRF Handling** | Configure Spring Security's `CookieCsrfTokenRepository.withHttpOnlyFalse()` with `setCookieName("pp_csrf")` and `setHeaderName("X-CSRF-Token")`. |
| **Error Format** | When an RPC fails, Spring Security & exception handlers must return `{ "error": "...", "errors": {} }` with `Content-Type: application/json` rather than standard Spring Boot HTML error pages. |
| **SSE Streaming** | Return `Content-Type: text/event-stream;charset=UTF-8`, set `Cache-Control: no-cache`, format chunks as `data: <json>\n\n`, and call `response.getWriter().flush()` after every emission. PulsePoint's `handleStream()` automatically strips the prefix and parses the JSON. |
| **WebSocket Heartbeats** | PulsePoint client sends `{"__pp": "ping"}` every 25 seconds. The Java backend MUST respond with `{"__pp": "pong"}` to keep the connection alive. Failure to respond causes the client to drop and start exponential reconnect backoff after 45 seconds. |
| **Multipart RPC** | Check `request.getContentType().startsWith("multipart/")`, wrap in `MultipartHttpServletRequest`, and extract `MultipartFile` instances alongside text fields. |
| **Thymeleaf Compatibility** | Thymeleaf's `${...}` syntax does not collide with PulsePoint's `{...}` bindings. However, template tags with `pp-for` should avoid dynamic `th:*` attributes that evaluate at server render time. |
| **Response Flushing** | Servlet filters writing JSON directly to `response.getWriter()` must call `.flush()` before completing to ensure client receives the complete JSON stream. |
