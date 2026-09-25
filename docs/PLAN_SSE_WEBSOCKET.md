# Implementation Plan: PulsePoint v2 SSE Streaming, WebSockets & Advanced Features

This plan outlines the design, architecture, and step-by-step implementation for extending our **Spring Boot 3.x monolith** with **PulsePoint v2 Server-Sent Events (SSE)**, **Named WebSockets (`pp.socket`)**, and remaining advanced integration requirements from `todo.md`.

---

## 🎯 Target Capabilities & Scope

1. **Phase 3: PulsePoint SSE / Streaming (`onStream`)**
   - Stream long-running task progress, bulk imports, or background job status in real time.
   - Support `onStream`, `onStreamComplete`, and `onStreamError` client callbacks.
   - Spring Boot `SseEmitter` integration directly inside our monolithic backend.

2. **Phase 4: PulsePoint Named WebSockets (`pp.socket`)**
   - Full-duplex real-time channel connected to `/__pulsepoint/ws?name=<channelName>`.
   - PulsePoint control frame heartbeat management (`{"__pp": "ping"}` ➔ `{"__pp": "pong"}` every 25s).
   - Real-time collaborative task broadcasting: when a user adds/updates/deletes a task in one tab/browser, all connected clients immediately reflect the update reactively.

3. **Multipart File / Attachment RPC**
   - Support uploading file attachments to tasks with progress monitoring (`onUploadProgress`).
   - Clean Spring `MultipartFile` handling with CSRF protection.

4. **Resilience, Error Recovery & Edge Cases**
   - Interrupted SSE reconnection and UI recovery.
   - WebSocket auto-reconnect backoff and connection drop handling.
   - XSS sanitization checks on streamed data.

5. **Automated Testing & Live Browser Verification**
   - JUnit 5 / MockMvc tests for SSE controllers and WebSocket text message handlers.
   - Live multi-tab browser test verifying real-time sync.

6. **Documentation & Reporting**
   - Update `PULSEPOINT_JAVA_REPORT.md`, `PULSEPOINT_JAVA_ISSUES.md`, and `PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md`.

---

## 🏗️ Architecture & Component Design

### 1. SSE Streaming Flow

```text
Browser (pp.rpc with onStream)
   │
   │  POST (X-PP-RPC: true, X-PP-Function: "streamTaskProgress")
   │  Accept: text/event-stream
   ▼
Spring Boot Monolith
   │
   ├─► PulsePointRpcFilter (detects streaming RPC request)
   └─► TaskStreamService (emits SseEmitter events)
         │
         ▼
      Format: "data: {\"percent\": 40, \"step\": \"Processing...\"}\n\n"
         │
         ▼
PulsePoint onStream callback ➔ pp.state update ➔ UI Progress Bar
```

### 2. WebSocket Flow (`pp.socket`)

```text
Browser Tab 1 (pp.socket)                       Browser Tab 2 (pp.socket)
   │                                                    │
   │ ws://localhost:8080/__pulsepoint/ws?name=tasks    │
   │ (Frame 1: args JSON, Heartbeat: ping/pong)         │
   ▼                                                    ▼
┌──────────────────────────────────────────────────────────────┐
│            Spring Boot WebSocket Handler & Broadcaster       │
│                                                              │
│  - PulsePointWebSocketHandler ("/ __pulsepoint/ws")          │
│  - Heartbeat Manager (catches {"__pp":"ping"} ➔ replies pong)│
│  - Session Registry (tracks active connected clients)        │
│  - TaskEventBroadcaster (broadcasts TASK_CREATED/UPDATED)    │
└──────────────────────────────────────────────────────────────┘
```

---

## 📝 Step-by-Step Execution Plan & Status

### Step 1: SSE Streaming Integration (Phase 3) - [COMPLETED]
- [x] **Spring Boot Streaming Infrastructure:**
  - Created [`PulsePointStreamEmitter.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/stream/PulsePointStreamEmitter.java) and functional interface [`PulsePointStream.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/stream/PulsePointStream.java).
  - Registered streaming RPC function `streamTaskAudit` in [`TaskRpcRegistrar.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/handler/TaskRpcRegistrar.java) to simulate progressive task audits (25%, 50%, 75%, 100%).
- [x] **PulsePoint RPC Filter Update:**
  - Updated [`PulsePointRpcFilter.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/PulsePointRpcFilter.java) to detect `PulsePointStream`, set `Content-Type: text/event-stream;charset=UTF-8`, disable response buffering, and emit chunks as `data: <json>\n\n`.
- [x] **Frontend UI Integration (`tasks.html`):**
  - Added "⚡ Audit (SSE)" button to each task row.
  - Implemented `pp.rpc("streamTaskAudit", { taskId }, { onStream, onStreamComplete, onStreamError })`.
  - Added reactive progress bar (`{auditPercent}%`) and audit step status indicator (`{auditStep}`) powered by `pp.state`.

---

### Step 2: Named WebSockets Integration (Phase 4) - [COMPLETED]
- [x] **Spring Boot WebSocket Infrastructure:**
  - Added `spring-boot-starter-websocket` dependency.
  - Created [`WebSocketConfig.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/config/WebSocketConfig.java) registering `/__pulsepoint/ws`.
  - Created [`PulsePointWebSocketHandler.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/websocket/PulsePointWebSocketHandler.java):
    - Parses channel query parameter `name` (`/__pulsepoint/ws?name=tasks`).
    - Intercepts PulsePoint 25-second control frame heartbeat `{"__pp": "ping"}` and responds with `{"__pp": "pong"}`.
    - Tracks active client sessions in thread-safe concurrent sets.
- [x] **Collaborative Task Broadcasting Service:**
  - Created [`TaskBroadcaster.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/websocket/TaskBroadcaster.java).
  - Injected into [`TaskRpcRegistrar.java`](../basic.sprinng.pulsepoint/src/main/java/basic/sprinng/pulsepoint/pulsepoint/handler/TaskRpcRegistrar.java) to broadcast `TASK_CREATED`, `TASK_UPDATED`, and `TASK_DELETED` events across all open sessions.
- [x] **Frontend UI Integration (`tasks.html`):**
  - Initialized `pp.socket("tasks", {}, { onOpen, onMessage, onClose })`.
  - Synchronizes task state reactively across open browser tabs without full page reloads.
  - Displays live connection status badge: `● Live Sync: {liveSyncStatus}`.

---

### Step 3: Multipart / File Upload via RPC (Phase 5) - [COMPLETED]
- [x] **Spring Boot Multipart Configuration:**
  - Updated `PulsePointRpcFilter` to support `multipart/form-data` requests via `StandardServletMultipartResolver` and `MultipartHttpServletRequest`.
  - Registered `uploadTaskAttachment` RPC handler in `TaskRpcRegistrar`.
- [x] **Frontend UI Integration:**
  - Added `📎 Attach` file input and upload handler.
  - Tracks upload progress reactively using PulsePoint's `onUploadProgress({ loaded, total, percent })`.

---

### Step 4: Verification & Automated Tests - [COMPLETED]
- [x] **Unit & Integration Tests:**
  - `PulsePointRpcFilterTest`: Added tests for SSE stream delivery and multipart file upload handling (9 tests total).
  - `PulsePointWebSocketHandlerTest`: Created unit tests for connection tracking, ping/pong heartbeat, and broadcast dispatching (3 tests total).
  - **All 22/22 tests passing cleanly via `./mvnw clean test`**.
- [x] **Live End-to-End Verification:**
  - SSE stream verified via live endpoint request receiving progressive `data: ...` chunks.
  - Multipart file upload verified receiving JSON confirmation.
  - WebSocket ping/pong and broadcast verified.

---

### Step 5: Update Reports & Documentation - [COMPLETED]
- [x] Updated `WALKTHROUGH.md` with SSE, WebSockets, Multipart, and 22-test breakdown.
- [x] Updated `PULSEPOINT_JAVA_REPORT.md` with real-time architecture, heartbeat protocol, and streaming benchmarks.
- [x] Updated `PULSEPOINT_AGENTIC_TEST.md` marking SSE and WebSocket rows as **Pass**.
- [x] Updated `PROPOSED_PULSEPOINT_SPRING_BOOT_DOCS.md` with SSE and WebSocket guides.
- [x] Updated `README.md` with 22-test instructions and feature details.
