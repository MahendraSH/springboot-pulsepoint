# PulsePoint v2 Agentic & AI Evaluation Report

This report evaluates how effectively an AI coding agent (unfamiliar with PulsePoint prior to receiving `llms.md`) was able to interpret documentation, design the monolithic architecture, implement Java integration code, handle wire protocol contracts, and debug runtime issues without direct guidance from the PulsePoint creator.

---

## 1. Agentic Test Results Matrix

| Agentic Test | Result | Notes / Details |
|---|:---:|---|
| **Agent understands PulsePoint architecture** | **Pass** | Understood backend-agnostic single-file runtime model, absence of build steps, HTML host template role, and server-side source of truth. |
| **Agent locates correct documentation** | **Pass** | Successfully referenced and parsed `llms.md` and `pp-reactive-v2.min.js` wire contracts. |
| **Agent understands `pp.state()`** | **Pass** | Correctly structured React-style hook getters/setters and computed states within `<template pp-component>` scripts. |
| **Agent understands components** | **Pass** | Correctly created boundary with `<template pp-component="task_manager">`, avoided foster-parenting issues, and isolated component scripts. |
| **Agent implements RPC correctly** | **Pass** | Intercepted `POST` with `X-PP-RPC: true`, extracted `X-PP-Function`, parsed JSON payloads, and returned JSON results. |
| **Agent understands PulsePoint headers/wire protocol** | **Pass** | Correctly mapped `X-PP-RPC`, `X-PP-Function`, `X-CSRF-Token`, and `Accept: application/json`. |
| **Agent implements Java RPC handler** | **Pass** | Created decoupled, reusable infrastructure (`PulsePointRpcFilter`, `PulsePointRpcRegistry`, `TaskRpcRegistrar`) without coupling domain logic to servlet filters. |
| **Agent handles CSRF correctly** | **Pass** | Identified that PulsePoint expects non-HttpOnly `pp_csrf` cookie and sends `X-CSRF-Token` header, bridging Spring Security seamlessly. |
| **Agent implements SSE correctly** | **Pass** | Implemented `PulsePointStream` and `PulsePointStreamEmitter` returning `text/event-stream;charset=UTF-8` with `data: <json>\n\n` chunks consumed reactively via `onStream`. |
| **Agent implements WebSocket integration** | **Pass** | Implemented `PulsePointWebSocketHandler` at `/__pulsepoint/ws?name=tasks`, handling 25s `{"__pp": "ping"}` heartbeat with `{"__pp": "pong"}` and multi-tab live task broadcast sync. |
| **Agent handles PulsePoint errors correctly** | **Pass** | Discovered and fixed `PP-JAVA-001` (HTML error bubbling causing `SyntaxError`) and structured all error envelopes as `{ "error": ..., "errors": ... }`. |
| **Agent invents nonexistent PulsePoint APIs** | **No** | Did NOT invent APIs. Strictly respected the closed set of directives (`pp-for`, `key`, `hidden`, `pp-component`) and avoided nonexistent directives (`pp-if`, `pp-model`). |
| **Human intervention required** | **Low** | Only 2 interventions required: (1) agreeing on simplified Task CRUD scope instead of full complex multi-tenant store, and (2) confirming local PostgreSQL instead of cloud Neon DB. |
| **Documentation ambiguity encountered** | **Yes** | Client-side `response.json()` failure on HTML errors; CSRF default cookie naming mismatch (`pp_csrf` vs `XSRF-TOKEN`); and async `event.currentTarget` recycling in forms. |
| **Missing documentation** | **Yes** | Lack of Java/Spring Boot specific integration guidance (Jackson Date/Time configuration, Spring Security filter ordering, JSON entry points, WebSocket control frame contract). |

---

## 2. Key Question Evaluation

> **Could an AI agent unfamiliar with PulsePoint read the documentation and build a working Spring Boot integration without help from the PulsePoint creator?**

### Verdict: **YES (with minor integration hurdles)**

### Detailed Assessment:
1. **Clarity of `llms.md`:** The provided `llms.md` document is exceptionally dense and informative for AI models. It explicitly enumerates closed lists (e.g., "There is NO pp-if, pp-model...", "Closed list of hooks"), which prevented the AI agent from hallucinating common Vue/Alpine/React patterns.
2. **Wire Protocol Specification:** The definition of headers (`X-PP-RPC`, `X-PP-Function`, `X-CSRF-Token`), streaming contracts, and expected JSON shapes allowed the agent to implement a working Java servlet filter in the first attempt.
3. **Where Friction Occurred:**
   - **Error Handling Assumptions:** PulsePoint assumes the server *always* emits valid JSON on failure. In Java, unhandled exceptions or container-level security rejections (401, 403, 500) trigger the servlet container's HTML error dispatcher (`/error`). The PulsePoint client runtime crashed attempting `JSON.parse("<!DOCTYPE html>...")` instead of gracefully surfacing the HTTP error.
   - **CSRF Defaults:** PulsePoint mandates cookie name `pp_csrf` and header `X-CSRF-Token`, whereas the entire Spring ecosystem standardizes on `XSRF-TOKEN` and `X-XSRF-TOKEN`. While easily configurable in Spring Security, this was an undocumented friction point.
   - **Date/Time Serialization:** Standard Java 21 `LocalDateTime` objects require Jackson's `JavaTimeModule` when serialized directly in servlet filters.
   - **WebSocket Heartbeat:** PulsePoint sends a ping every 25 seconds expecting `{"__pp": "pong"}` within 45 seconds or it disconnects. Documenting this explicitly saves time.

---

## 3. Complexity & Usability Assessment

Scale:
- **1** = Extremely Easy / Seamless
- **5** = Moderate / Normal Engineering Effort
- **10** = Extremely Difficult / High Friction

| Area | Score (1-10) | Explanation |
|---|:---:|---|
| **Initial Installation** | **1 / 10** | Dropping a single `pp-reactive-v2.min.js` file into `src/main/resources/static/js` and importing it via `<script type="module">` was effortless. No Node.js, npm, or bundler needed. |
| **Learning PulsePoint** | **3 / 10** | Concepts mirror React hooks (`pp.state`, `pp.effect`) and vanilla HTML. The closed directive list makes cognitive load very low. |
| **Spring Boot Integration** | **4 / 10** | Required writing custom servlet filter and registry infrastructure. A Spring Boot Starter would reduce this to 1/10. |
| **Thymeleaf Integration** | **2 / 10** | Natural fit. Thymeleaf syntax (`${...}`) and PulsePoint bindings (`{...}`) co-exist without brace collisions. Just must avoid `th:*` inside client loop templates. |
| **Database Integration** | **2 / 10** | Completely standard Spring Data JPA / PostgreSQL. PulsePoint remains strictly client-side and interacts via standard Java DTOs. |
| **RPC Implementation** | **3 / 10** | Wire protocol is straightforward: inspect headers, dispatch to registered Java function, write JSON response. |
| **Streaming (SSE)** | **3 / 10** | Straightforward once wire protocol (`data: <json>\n\n` with flushed output stream) was integrated into `PulsePointRpcFilter`. |
| **WebSockets** | **4 / 10** | Clean integration using Spring `TextWebSocketHandler`. Requires handling the `{"__pp": "ping"}` 25s control frame and routing query parameter `?name=...`. |
| **Security Integration** | **4 / 10** | Works with standard Spring Security sessions; requires custom `CookieCsrfTokenRepository` configuration and JSON authentication entry points. |
| **Error Handling** | **5 / 10** | High friction initially because PulsePoint client crashes on HTML responses. Once JSON handlers are established, client error consumption is clean. |
| **Documentation Quality** | **4 / 10** | `llms.md` is great for JS/Node developers, but lacks Java/Spring specifics (CSRF names, filter order, Jackson date formats). |
| **Debugging Experience** | **4 / 10** | When templates fail with unquoted braces, debugging is easy. When client crashes on HTML responses with `Unexpected token '<'`, root cause takes inspection. |
| **AI-Agent Usability** | **2 / 10** | Outstanding for AI agents. The closed rules, explicit non-features, and lack of build toolchain prevent agent drift. |
| **Production Readiness** | **7 / 10** | Complete RPC, SSE, WebSockets, and CSRF support implemented. Production-ready for enterprise Spring Boot monoliths. |
