# Spring Boot + PulsePoint v2 Integration Walkthrough

## Overview

We have built, styled, and validated a monolithic Java Spring Boot application integrating **PulsePoint v2** reactive frontend layer with **Tailwind CSS** (zero-build, without npm), and no separate Node.js / Vite build step or frontend server.

```
Browser (PulsePoint v2 runtime + Tailwind CSS)
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
- **Entities & Schema:** [`Task.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/entity/Task.java) with `TaskStatus` and `TaskPriority` enums, timestamps, and JPA lifecycle hooks.
- **Service Layer:** Interface [`TaskService`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/service/TaskService.java) and implementation [`TaskServiceImpl`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/service/impl/TaskServiceImpl.java) using constructor injection.
- **DTOs:** [`CreateTaskRequest`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/dto/CreateTaskRequest.java), [`UpdateTaskRequest`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/dto/UpdateTaskRequest.java), [`TaskResponse`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/dto/TaskResponse.java).
- **Security:** [`SecurityConfig`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/config/SecurityConfig.java) with session auth, in-memory user (`demo`/`demo123`), and JSON-aware exception handling for `X-PP-RPC` requests.

### 2. PulsePoint v2 Wire Protocol Integration
- **Function Registry:** [`PulsePointRpcRegistry`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/PulsePointRpcRegistry.java) allows registering named RPC handlers safely without dynamic code evaluation.
- **Wire Protocol Filter:** [`PulsePointRpcFilter`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/PulsePointRpcFilter.java):
  - Intercepts `POST` requests where `X-PP-RPC: true`
  - Extracts the requested operation from `X-PP-Function`
  - Parses JSON arguments or multipart form uploads
  - Detects streaming returns (`PulsePointStream`) and switches output to `Content-Type: text/event-stream;charset=UTF-8`
  - Returns `application/json` with the exact payload format PulsePoint expects (including RFC-compliant `{ "error": "...", "errors": { ... } }` error envelopes).
- **CSRF Bridge:** [`PulsePointCsrfFilter`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/PulsePointCsrfFilter.java) + `CookieCsrfTokenRepository.setCookieName("pp_csrf")` exposes the token for the client JS to send as `X-CSRF-Token`.
- **RPC Registrar:** [`TaskRpcRegistrar`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/handler/TaskRpcRegistrar.java) exposes `listTasks`, `getTask`, `createTask`, `updateTask`, `deleteTask`, `streamTaskAudit`, and `uploadTaskAttachment`.

### 3. Server-Sent Events (SSE) Streaming
- [`PulsePointStreamEmitter.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/stream/PulsePointStreamEmitter.java) flushes SSE chunks immediately over the HTTP response stream in format `data: <json>\n\n`.
- Registered `streamTaskAudit`: emits live audit steps (`25%`, `50%`, `75%`, `100%`) for long-running task operations.
- Client consumed seamlessly via PulsePoint's `pp.rpc("streamTaskAudit", { taskId }, { onStream, onStreamComplete, onStreamError })`.
- Reactive progress bar (`{auditPercent}%`) and status indicator (`{auditStep}`) update with zero page flicker.

### 4. Named WebSockets & Collaborative Live Sync
- [`WebSocketConfig.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/config/WebSocketConfig.java) maps `/__pulsepoint/ws`.
- [`PulsePointWebSocketHandler.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/websocket/PulsePointWebSocketHandler.java):
  - Inspects query param `name` (e.g. `/__pulsepoint/ws?name=tasks`).
  - Handles PulsePoint's 25-second control frame heartbeat: when client sends `{"__pp": "ping"}`, backend immediately responds `{"__pp": "pong"}` to prevent 45-second connection drops.
- [`TaskBroadcaster.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/websocket/TaskBroadcaster.java):
  - Connected sessions receive real-time JSON frames: `TASK_CREATED`, `TASK_UPDATED`, `TASK_DELETED`.
- Multiple browser tabs synchronize instantaneously without polling or manual refreshes.
- Live connection indicator: `● WebSocket Live Sync: {liveSyncStatus}` with pulsing green indicator.

### 5. Multipart File Uploads & XSS Safety
- `PulsePointRpcFilter` supports `multipart/form-data` decoding via `StandardServletMultipartResolver`.
- Registered `uploadTaskAttachment` accepting file attachments with live upload tracking via PulsePoint's `onUploadProgress({ loaded, total, percent })`.
- XSS verification: verified that client HTML template interpolation safely escapes `<script>` tags as text nodes.

### 6. Tailwind CSS & Responsive UI/UX Modernization (Without npm)
- **Zero-Build Tailwind CSS**: Configured Tailwind via CDN script (`https://cdn.tailwindcss.com`) paired with an in-page configuration script. Requires **0 npm / Node.js dependencies**, perfectly fitting the Spring Boot monolith.
- **Inter Typography & Zinc Design System**: Applied Google Fonts `Inter` with neutral zinc surfaces (`bg-zinc-950`, `bg-zinc-900/90`, `border-zinc-800`), glowing indigo rings, and high-contrast status pills.
- **Full Mobile/Tablet/Desktop Responsiveness**: Header, quick metrics cards, creation form grid (`grid-cols-1 sm:grid-cols-2`), filter buttons, and task cards stack cleanly without horizontal scrollbars on mobile (375x667).
- **Dynamic Quick Metrics**: Real-time counts for Total, Todo, In Progress, and Done tasks dynamically bound to PulsePoint state.
- **Strict Compliance with `docs/llms.md`**: Preserved `<template pp-component="task_manager">` boundary, quoted all attribute bindings (`class="..."`, `hidden="{...}"`), and maintained keyed `<template pp-for>`.

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
- Seeded with 40 records via [`schema.sql`](../schema.sql)
- Loaded reactively via `listTasks` RPC
- Inserted tasks reactively via `createTask` RPC
- Validated error states and JSON error responses.

### End-to-End Interactive Verification Suite (8/8 PASSED)

| # | Feature / Test Case | Actions & Triggers | Observations & Result | Status |
|---|---------------------|--------------------|------------------------|:------:|
| **1** | **Authentication** | Navigated to `/login`, authenticated with `demo` / `demo123`. | Auto-redirected to `/tasks`. Modern dark card with demo account pills. | **PASS** |
| **2** | **Initial State & Filtering** | Inspected top badges and clicked `TODO`, `IN PROGRESS`, `DONE`, `ALL`. | Initial tasks loaded via RPC. `WebSocket Live Sync: Connected` active. Filter tabs toggle instantly. | **PASS** |
| **3** | **Validation & Error Handling (RPC)** | Submitted empty title, then 210-character title. | Server Bean Validation rejected invalid inputs with top red banner and inline warnings. Valid task cleared errors. | **PASS** |
| **4** | **Status Mutation & Deletion (RPC)** | Changed task to `IN PROGRESS` then `DONE`. Clicked `Delete`. | Status badge mutated dynamically with zero page flicker. Task removed from DOM upon deletion. | **PASS** |
| **5** | **Server-Sent Events (SSE) Streaming** | Clicked `⚡ Audit (SSE)` on Task #1. | Real-time card appeared, streaming progress (25% → 50% → 75% → 100%) and step messages via `text/event-stream`. | **PASS** |
| **6** | **Multipart File Upload with Progress** | Clicked `📎 Attach`, selected `test-attachment.txt`. | Upload progress bar tracked upload chunks via `onUploadProgress`, finishing at 100% with green success notice. | **PASS** |
| **7** | **Multi-Tab WebSocket Live Sync** | Opened Tab 2. Created `"WebSocket Sync Task"` in Tab 1. Checked Tab 2. | Tab 2 dynamically received `TASK_CREATED` over `ws://` and rendered the new task without any manual reload. | **PASS** |
| **8** | **Tailwind CSS & Mobile Responsiveness** | Resized browser to mobile (375x667) and tablet (768x1024). | Fluid layout cleanly reflowed navigation, quick stats, form controls, and task cards with zero horizontal overflow. | **PASS** |

#### Visual Evidence & Snapshots

- **Modernized Login Page:** [`screenshots/login_page_1790321882096.png`](screenshots/login_page_1790321882096.png)
- **Responsive Tasks Dashboard (Desktop):** [`screenshots/tasks_page_dashboard_1790321972386.png`](screenshots/tasks_page_dashboard_1790321972386.png)
- **Task Created via RPC:** [`screenshots/task_created_success_1790322134999.png`](screenshots/task_created_success_1790322134999.png)
- **Status Mutation to IN_PROGRESS:** [`screenshots/status_updated_in_progress_1790322166624.png`](screenshots/status_updated_in_progress_1790322166624.png)
- **Live SSE Audit Streaming:** [`screenshots/sse_audit_progress_1790322214884.png`](screenshots/sse_audit_progress_1790322214884.png)
- **Mobile Viewport (375x667 Header & Form):** [`screenshots/mobile_view_top_1790322286184.png`](screenshots/mobile_view_top_1790322286184.png)
- **Mobile Viewport (375x667 Task Cards):** [`screenshots/mobile_view_portrait_1790322262913.png`](screenshots/mobile_view_portrait_1790322262913.png)
- **Tablet Viewport (768x1024):** [`screenshots/tablet_view_1790322311058.png`](screenshots/tablet_view_1790322311058.png)
- **Multi-Tab WebSocket Live Sync:** [`screenshots/websocket_live_sync_1790163508917.png`](screenshots/websocket_live_sync_1790163508917.png)

#### Interactive Session Recordings
- **Tailwind CSS & Responsive Verification Video:** [`screenshots/tailwind_responsive_test_1790321816150.webp`](screenshots/tailwind_responsive_test_1790321816150.webp)
- **Full Monolithic E2E Verification Video:** [`screenshots/full_interactive_verification_1790159186402.webp`](screenshots/full_interactive_verification_1790159186402.webp)

---

## Reusable Java Integration Findings for PulsePoint

| Topic | Finding / Best Practice |
|---|---|
| **Zero-Build Tailwind CSS** | Tailwind can be integrated via CDN + custom JavaScript config script directly inside Thymeleaf templates with 0 npm dependencies, preserving the zero-build nature of PulsePoint. |
| **CSRF Handling** | Configure Spring Security's `CookieCsrfTokenRepository.withHttpOnlyFalse()` with `setCookieName("pp_csrf")` and `setHeaderName("X-CSRF-Token")`. |
| **Error Format** | When an RPC fails, Spring Security & exception handlers must return `{ "error": "...", "errors": {} }` with `Content-Type: application/json` rather than standard Spring Boot HTML error pages. |
| **SSE Streaming** | Return `Content-Type: text/event-stream;charset=UTF-8`, set `Cache-Control: no-cache`, format chunks as `data: <json>\n\n`, and call `response.getWriter().flush()` after every emission. PulsePoint's `handleStream()` automatically strips the prefix and parses the JSON. |
| **WebSocket Heartbeats** | PulsePoint client sends `{"__pp": "ping"}` every 25 seconds. The Java backend MUST respond with `{"__pp": "pong"}` to keep the connection alive. Failure to respond causes the client to drop and start exponential reconnect backoff after 45 seconds. |
| **Multipart RPC** | Check `request.getContentType().startsWith("multipart/")`, wrap in `MultipartHttpServletRequest`, and extract `MultipartFile` instances alongside text fields. |
| **Thymeleaf Compatibility** | Thymeleaf's `${...}` syntax does not collide with PulsePoint's `{...}` bindings. However, template tags with `pp-for` should avoid dynamic `th:*` attributes that evaluate at server render time. |
| **Response Flushing** | Servlet filters writing JSON directly to `response.getWriter()` must call `.flush()` before completing to ensure client receives the complete JSON stream. |
