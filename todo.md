# PulsePoint Java Integration & Validation Task

## Objective

Integrate **PulsePoint v2** into a real Java web application and determine how effectively PulsePoint can provide a modern reactive frontend while allowing Java to remain the primary application server.

PulsePoint:
https://pulsepoint.tsnc.tech/

The goal is **not simply to make a demo work**.

The purpose of this assignment is to identify:

* How easy PulsePoint is to integrate into the Java ecosystem.
* Whether Spring Boot can handle templates, authentication, database access, RPC endpoints, streaming, WebSockets, validation, and errors while PulsePoint handles browser reactivity.
* What PulsePoint functionality works immediately.
* What requires custom Java integration code.
* What PulsePoint bugs or limitations are discovered.
* What documentation is missing or unclear.
* How understandable PulsePoint is to an AI coding agent.
* Whether Java developers could realistically adopt PulsePoint without React/Vue or a separate frontend build environment.
* What improvements should be made to PulsePoint specifically for Java/Spring developers.

## Recommended Test Stack

Use a realistic Java stack rather than a minimal static example:

**Backend:** Spring Boot 3.x
**Java:** Java 21+
**Templates:** Thymeleaf
**Database:** PostgreSQL or MySQL
**Persistence:** Spring Data JPA / Hibernate
**Security:** Spring Security
**Frontend reactivity:** PulsePoint v2
**Build:** Maven or Gradle

Do **not** introduce React, Vue, Angular, Next.js, Vite, or another frontend framework.

The browser should receive server-rendered HTML from Spring Boot and use PulsePoint for reactivity.

---

## Demo Application

Build a small but complete **Product / Inventory Management application**.

It should contain products such as:

```text
Product
- id
- name
- description
- category
- price
- quantity
- status
- createdAt
- updatedAt
```

The application must persist its data in a real database.

The UI should feel reactive without requiring full page reloads for normal operations.

## Required Integration Tests

| Area                 | What to implement/test                                                                | Expected result                                                              |
| -------------------- | ------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| PulsePoint bootstrap | Load PulsePoint directly from Spring Boot static resources                            | PulsePoint initializes without Node, Vite, React, or another frontend server |
| Server Rendering     | Render the initial page using Thymeleaf                                               | Java remains responsible for initial HTML rendering                          |
| Reactive State       | Use `pp.state()` for product data, loading states, selected records, form state, etc. | State changes update only the relevant UI                                    |
| Effects              | Test `pp.effect()` with dependent state                                               | Effects execute predictably without unexpected rerenders                     |
| List Rendering       | Render database records using PulsePoint list functionality                           | Add/update/delete operations correctly reconcile rows                        |
| Components           | Create reusable product form, table, notification, modal, and navigation components   | Component state remains correctly scoped                                     |
| Database CRUD        | Create, read, update and delete products using Spring Data JPA                        | Database operations update PulsePoint state without full page reloads        |
| RPC                  | Connect PulsePoint RPC calls to Spring controllers                                    | RPC calls cleanly map browser actions to Java methods/endpoints              |
| RPC Contract         | Investigate headers, request payload, response structure and function identification  | Produce a documented Java implementation of the PulsePoint RPC contract      |
| JSON                 | Send/receive normal JSON payloads                                                     | Java DTOs serialize and deserialize correctly                                |
| Multipart            | Test form/file upload through PulsePoint                                              | Multipart RPC works correctly with Spring                                    |
| Validation           | Trigger Java Bean Validation errors                                                   | Validation errors can be displayed reactively beside fields                  |
| Loading State        | Show loading state during requests                                                    | State reliably resets after success and failure                              |
| Error Handling       | Test 400, 401, 403, 404, validation errors and 500 exceptions                         | Frontend can identify and display appropriate errors                         |
| Network Errors       | Disconnect network or terminate backend during RPC                                    | PulsePoint fails gracefully and application can recover                      |
| Timeout              | Simulate a slow Java endpoint                                                         | UI does not become permanently stuck                                         |
| SSE Streaming        | Implement a Spring SSE endpoint and consume it through PulsePoint                     | Streamed events incrementally update reactive state                          |
| Streaming Error      | Interrupt SSE connection                                                              | Determine behavior and required reconnection strategy                        |
| WebSockets           | Implement a Spring WebSocket endpoint                                                 | Server messages update PulsePoint state in real time                         |
| Security             | Enable Spring Security and CSRF protection                                            | PulsePoint RPC continues working securely                                    |
| Authentication       | Implement login/session authentication                                                | RPC respects authenticated Spring Security session                           |
| Authorization        | Restrict delete/update operations by role                                             | Authorization remains server-side and cannot be bypassed through JS          |
| XSS Safety           | Store intentionally unsafe-looking strings in database                                | Determine whether template/state interpolation is safely handled             |
| SPA Navigation       | Test PulsePoint navigation between server-rendered Spring routes                      | Navigation works without introducing a separate SPA framework                |
| Browser Navigation   | Test Back/Forward/Refresh/direct URL entry                                            | Routing remains predictable                                                  |
| Third-party JS       | Integrate one normal JavaScript library                                               | Confirm PulsePoint does not require adapters/wrappers                        |
| Large Lists          | Test approximately 100, 1,000 and 5,000 rows                                          | Record rendering behavior and responsiveness                                 |
| Concurrent Updates   | Update state while requests or streams are active                                     | Identify race conditions or stale-state behavior                             |
| Cleanup              | Navigate/remove components containing effects or sockets                              | Confirm listeners/effects/connections are cleaned up correctly               |

## Java-Specific Integration

Do not merely make Spring controllers that happen to respond to `fetch()`.

Investigate whether a reusable Java integration layer can be created.

For example:

```text
pulsepoint-java/
    PulsePointRpcHandler
    PulsePointRequest
    PulsePointResponse
    PulsePointExceptionHandler
    PulsePointCsrfSupport
    PulsePointStreamSupport
    PulsePointWebSocketSupport
```

The exact API is up to you.

The purpose is to determine whether common PulsePoint integration logic can eventually become reusable Java/Spring infrastructure instead of developers manually implementing the protocol in every controller.

Document:

```text
Browser
   ↓
PulsePoint
   ↓
RPC / HTTP / SSE / WebSocket
   ↓
Spring Boot
   ↓
Service Layer
   ↓
JPA / Hibernate
   ↓
PostgreSQL / MySQL
```

Java must remain the source of truth for business logic and database operations.

---

# Security Validation

Pay particular attention to Spring Security.

Test:

```text
PulsePoint → CSRF token → Spring Security
PulsePoint → session cookie → Spring Security
PulsePoint RPC → authorization check
PulsePoint stream → authenticated user
PulsePoint WebSocket → authenticated user
```

Never rely on PulsePoint state for authorization.

For example, hiding a Delete button for normal users is useful UX, but the Java server must still reject an unauthorized delete request.

Document any special configuration PulsePoint needs for Spring Security.

---

# Error Handling Test

Create deliberate backend failures.

Examples:

```text
400 Invalid Request
401 Not Authenticated
403 Not Authorized
404 RPC/function not found
409 Conflict
422 Validation Error
500 Internal Server Error
Database unavailable
Malformed response
Request timeout
SSE connection interrupted
WebSocket disconnected
```

For every case determine:

```text
1. What does PulsePoint receive?
2. What exception/error API is exposed?
3. Can the UI react to it easily?
4. Does loading state reset?
5. Can the user retry?
6. Is the console error understandable?
7. Does the documentation explain the behavior?
```

---

# Agentic / AI Test

This is an important part of the assignment.

Test whether PulsePoint is genuinely easy for an AI coding agent to understand.

Use an AI coding agent such as Claude Code, Codex, Cursor, Copilot or another coding agent.

Give the agent the PulsePoint documentation and `llms.md` when available.

Then ask it something comparable to:

```text
Integrate PulsePoint v2 into this Spring Boot application.

Use Spring Boot, Thymeleaf and Spring Data JPA.

Create a reactive product management interface with database CRUD,
PulsePoint RPC, validation, error handling and SSE streaming.

Do not use React, Vue, Angular, Node.js or Vite.

Follow the PulsePoint documentation and wire protocol.
```

Do not manually explain undocumented PulsePoint behavior to the agent initially.

Observe what happens.

Record:

| Agentic test                                       | Result                |
| -------------------------------------------------- | --------------------- |
| Agent understands PulsePoint architecture          | Pass / Partial / Fail |
| Agent locates correct documentation                | Pass / Partial / Fail |
| Agent understands `pp.state()`                     | Pass / Partial / Fail |
| Agent understands components                       | Pass / Partial / Fail |
| Agent implements RPC correctly                     | Pass / Partial / Fail |
| Agent understands PulsePoint headers/wire protocol | Pass / Partial / Fail |
| Agent implements Java RPC handler                  | Pass / Partial / Fail |
| Agent handles CSRF correctly                       | Pass / Partial / Fail |
| Agent implements SSE correctly                     | Pass / Partial / Fail |
| Agent implements WebSocket integration             | Pass / Partial / Fail |
| Agent handles PulsePoint errors correctly          | Pass / Partial / Fail |
| Agent invents nonexistent PulsePoint APIs          | Yes / No              |
| Human intervention required                        | Low / Medium / High   |
| Documentation ambiguity encountered                | Describe              |
| Missing documentation                              | Describe              |

Also record how many times you had to correct the agent.

A very important result would be:

> Could an AI agent unfamiliar with PulsePoint read the documentation and build a working Spring Boot integration without help from the PulsePoint creator?

---

# PulsePoint Issue Reporting

For every issue discovered, create an entry using this format:

| Field                      | Information                                                |
| -------------------------- | ---------------------------------------------------------- |
| Issue ID                   | PP-JAVA-001                                                |
| Area                       | RPC / State / Streaming / Component / Documentation / etc. |
| Severity                   | Critical / High / Medium / Low                             |
| Environment                | Browser + Java/Spring versions                             |
| Steps to reproduce         | Exact reproduction steps                                   |
| Expected behavior          | What should happen                                         |
| Actual behavior            | What actually happens                                      |
| Console output             | Error/warning                                              |
| Backend output             | Java exception/log                                         |
| Root cause                 | If identified                                              |
| PulsePoint issue?          | Yes / No / Unsure                                          |
| Java integration issue?    | Yes / No / Unsure                                          |
| Documentation issue?       | Yes / No                                                   |
| Workaround                 | If one exists                                              |
| Recommended PulsePoint fix | Suggested change                                           |

Do not silently work around PulsePoint problems.

If you discover a problem and find a workaround, document **both** the original problem and the workaround.

---

# Documentation Review

While integrating, evaluate whether the documentation answers a Java developer's natural questions.

Specifically identify missing documentation around:

```text
Spring Boot
Thymeleaf
Spring Security
CSRF
Spring MVC
Jackson serialization
Multipart uploads
SSE
WebSockets
Exception handling
Database CRUD
Authentication
Session handling
RPC routing
Deployment
Production configuration
```

For every missing area, propose what should be added to the official PulsePoint documentation.

If possible, create a draft:

```text
/docs/java
```

or:

```text
/docs/spring-boot
```

that could eventually become the official Java integration guide.

---

# Complexity Assessment

At the end, evaluate the integration separately in these areas:

| Area                    | Score | Explanation |
| ----------------------- | ----: | ----------- |
| Initial installation    |  1–10 |             |
| Learning PulsePoint     |  1–10 |             |
| Spring Boot integration |  1–10 |             |
| Thymeleaf integration   |  1–10 |             |
| Database integration    |  1–10 |             |
| RPC implementation      |  1–10 |             |
| Streaming               |  1–10 |             |
| WebSockets              |  1–10 |             |
| Security integration    |  1–10 |             |
| Error handling          |  1–10 |             |
| Documentation quality   |  1–10 |             |
| Debugging experience    |  1–10 |             |
| AI-agent usability      |  1–10 |             |
| Production readiness    |  1–10 |             |

For **complexity scores**, use:

```text
1 = extremely easy
5 = moderate
10 = extremely difficult
```

For quality/readiness categories, clearly state the meaning of the scale before scoring.

Do not give PulsePoint a favorable score simply because the integration succeeds. The purpose of the test is to expose friction.

---

# Final Deliverables

The completed work should contain:

```text
1. Working Spring Boot + PulsePoint application
2. Real database integration
3. Complete CRUD implementation
4. PulsePoint RPC integration
5. Spring Security + CSRF test
6. SSE streaming example
7. WebSocket example
8. Error-handling examples
9. Java/PulsePoint reusable integration code
10. Automated Java tests where appropriate
11. Browser/integration tests where appropriate
12. PULSEPOINT_JAVA_REPORT.md
13. PULSEPOINT_JAVA_ISSUES.md
14. PULSEPOINT_AGENTIC_TEST.md
15. Proposed Java/Spring Boot documentation
```

## Most Important Question

The final report should answer:

> **Can PulsePoint give a Java/Spring Boot developer a modern reactive application architecture while allowing Java to remain the only application backend/runtime, without introducing React, Vue, Angular or a separate frontend build system?**

And separately:

> **Can an AI coding agent understand PulsePoint well enough from its documentation to implement that architecture correctly without significant human assistance?**

The report must explain **why**, identify every significant problem encountered, and recommend concrete changes to PulsePoint where necessary.