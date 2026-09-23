# PulsePoint Java Integration Issue Report

This document records all issues, edge cases, friction points, and documentation ambiguities discovered during the integration of **PulsePoint v2** with **Spring Boot 3.x / Java 21**.

---

### Issue Index

1. [PP-JAVA-001: Non-JSON HTML Error Responses Break PulsePoint Client Parser](#pp-java-001)
2. [PP-JAVA-002: Default Spring CSRF Cookie & Header Mismatch with PulsePoint Wire Protocol](#pp-java-002)
3. [PP-JAVA-003: Jackson Java 8/21 Date-Time Deserialization Failure Bubbles to Whitelabel Error](#pp-java-003)
4. [PP-JAVA-004: Native Form Submission Recycling Invalidates event.currentTarget in Async Handlers](#pp-java-004)
5. [PP-JAVA-005: Thymeleaf Server-Side Attribute Evaluation Clashing in Client Component Templates](#pp-java-005)
6. [PP-JAVA-006: Unhandled Static Resource 404s (e.g. Favicon) Trigger Internal Server Error](#pp-java-006)

---

### PP-JAVA-001

| Field | Information |
|---|---|
| **Issue ID** | `PP-JAVA-001` |
| **Area** | RPC / Error Handling / Wire Protocol |
| **Severity** | High |
| **Environment** | Chrome / macOS / Spring Boot 3.x / Java 21 |
| **Steps to Reproduce** | 1. Trigger a backend error (e.g., 401 unauthenticated, 403 CSRF failure, or unhandled 500 error) during a `pp.rpc("someFunction")` call.<br>2. Observe the browser console. |
| **Expected Behavior** | PulsePoint should catch non-2xx HTTP status codes, check the `Content-Type` header, and if non-JSON (like `text/html`) is received, reject with an `RpcError` detailing HTTP status and text, rather than throwing an unhandled `SyntaxError: Unexpected token '<'`. |
| **Actual Behavior** | The client runtime calls `response.json()` unconditionally. The raw `<!DOCTYPE html>` string crashes JSON parsing with: `SyntaxError: Unexpected token '<', "<!DOCTYPE "... is not valid JSON`. |
| **Console Output** | `installHook.js:1 RPC error: SyntaxError: Unexpected token '<', "<!DOCTYPE "... is not valid JSON (at VM206:1:1)` |
| **Backend Output** | Standard Spring Security 401/403 or Spring Boot Whitelabel Error response (`Content-Type: text/html`). |
| **Root Cause** | PulsePoint client assumes all HTTP responses from the backend are valid JSON, even when status is 4xx or 500. Standard Java / Spring Boot error dispatchers produce HTML by default. |
| **PulsePoint Issue?** | Yes |
| **Java Integration Issue?** | Yes |
| **Documentation Issue?** | Yes (Docs mention server should return `{ error, errors }` but don't specify what happens if an upstream gateway or filter emits HTML). |
| **Workaround** | Configure Spring Security's `defaultAuthenticationEntryPointFor` and `defaultAccessDeniedHandlerFor` specifically matching request header `X-PP-RPC: true` to always force `application/json` output, and catch all exceptions inside `PulsePointRpcFilter` to prevent container forward to `/error`. |
| **Recommended PulsePoint Fix** | In `pp-reactive-v2.min.js`, check `response.headers.get("content-type")`. If not JSON, use `response.text()` and populate `RpcError.body` with the raw text/HTML rather than crashing `JSON.parse`. |

---

### PP-JAVA-002

| Field | Information |
|---|---|
| **Issue ID** | `PP-JAVA-002` |
| **Area** | Security / CSRF / Wire Protocol |
| **Severity** | High |
| **Environment** | Spring Security 6.x / PulsePoint v2 |
| **Steps to Reproduce** | 1. Enable standard Spring Security CSRF via `CookieCsrfTokenRepository.withHttpOnlyFalse()`.<br>2. Mount a PulsePoint component and issue an RPC call. |
| **Expected Behavior** | PulsePoint and Spring Security communicate CSRF tokens automatically without manual header/cookie renaming. |
| **Actual Behavior** | Spring Security defaults to cookie name `XSRF-TOKEN` and header `X-XSRF-TOKEN`. PulsePoint v2 looks specifically for cookie `pp_csrf` and sends header `X-CSRF-Token`. As a result, PulsePoint fails to locate the token, performs recursive GET fallbacks, or gets rejected with 403 Forbidden. |
| **Console Output** | `[PP-WARN] CSRF cookie missing` or 403 Forbidden response. |
| **Backend Output** | `Invalid CSRF token found for http://localhost:8080/tasks` |
| **Root Cause** | Hardcoded cookie (`pp_csrf`) and header (`X-CSRF-Token`) names in PulsePoint client runtime that diverge from standard Java Spring conventions (`XSRF-TOKEN`). |
| **PulsePoint Issue?** | Yes |
| **Java Integration Issue?** | Yes |
| **Documentation Issue?** | Yes |
| **Workaround** | Explicitly reconfigure Spring Security `CookieCsrfTokenRepository`: `csrfRepository.setCookieName("pp_csrf")` and `csrfRepository.setHeaderName("X-CSRF-Token")`, and ensure `CsrfTokenRequestAttributeHandler.setCsrfRequestAttributeName(null)` is set. |
| **Recommended PulsePoint Fix** | Allow configuring CSRF cookie and header names in `ComponentInit.bootstrap({ csrfCookie: 'XSRF-TOKEN', csrfHeader: 'X-XSRF-TOKEN' })` or support standard `XSRF-TOKEN` fallback automatically. |

---

### PP-JAVA-003

| Field | Information |
|---|---|
| **Issue ID** | `PP-JAVA-003` |
| **Area** | JSON Serialization / Spring Boot / DTO |
| **Severity** | Medium |
| **Environment** | Java 21 / Spring Boot 3.x / Jackson Databind |
| **Steps to Reproduce** | 1. Return an entity or DTO with `java.time.LocalDateTime` from an RPC function.<br>2. Serialize result with standard Jackson `ObjectMapper`. |
| **Expected Behavior** | Object serializes cleanly into standard ISO-8601 string (`"2026-09-23T12:00:00"`). |
| **Actual Behavior** | Jackson throws `InvalidDefinitionException: Java 8 date/time type java.time.LocalDateTime not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"`. In servlet filters, this uncaught exception causes container forward to HTML error page, triggering `PP-JAVA-001`. |
| **Console Output** | `SyntaxError: Unexpected token '<', "<!DOCTYPE "... is not valid JSON` |
| **Backend Output** | `com.fasterxml.jackson.databind.exc.InvalidDefinitionException: Java 8 date/time type java.time.LocalDateTime not supported by default` |
| **Root Cause** | In modular Spring Boot / Java 21 setups, `jackson-datatype-jsr310` is not automatically registered unless custom `ObjectMapper` explicitly registers `new JavaTimeModule()`. |
| **PulsePoint Issue?** | No |
| **Java Integration Issue?** | Yes |
| **Documentation Issue?** | Yes (Java documentation should highlight Jackson date-time serialization best practices for RPC filters). |
| **Workaround** | Add `jackson-datatype-jsr310` to `pom.xml`, create a `@Configuration` registering `JavaTimeModule()` and disabling `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS`. |
| **Recommended PulsePoint Fix** | Document this requirement in official Java/Spring Boot integration guide. |

---

### PP-JAVA-004

| Field | Information |
|---|---|
| **Issue ID** | `PP-JAVA-004` |
| **Area** | Component Event Handling / DOM Lifecycle |
| **Severity** | Medium |
| **Environment** | Modern Browsers (Chrome / Firefox / Safari) / PulsePoint v2 |
| **Steps to Reproduce** | 1. Write form submit handler: `const handleCreateTask = async (event) => { await pp.rpc(...); event.currentTarget.reset(); }`<br>2. Submit form in browser. |
| **Expected Behavior** | Form resets its input fields after the promise resolves. |
| **Actual Behavior** | Browser native event object recycles `event.currentTarget` to `null` after awaiting the promise. Calling `event.currentTarget.reset()` fails with `TypeError: Cannot read properties of null (reading 'reset')`. |
| **Console Output** | `Uncaught (in promise) TypeError: Cannot read properties of null (reading 'reset')` |
| **Backend Output** | None (RPC succeeds on server, but client state crashes). |
| **Root Cause** | Standard JavaScript event loop behavior where asynchronous tasks run after the native DOM event phase has finished. |
| **PulsePoint Issue?** | Unsure |
| **Java Integration Issue?** | No |
| **Documentation Issue?** | Yes (The `llms.md` code snippet examples show `event.currentTarget.reset()` after `await pp.rpc(...)`, which can fail asynchronously). |
| **Workaround** | Cache form reference before the first `await`: `const form = event.currentTarget; ... await pp.rpc(...); form.reset();` |
| **Recommended PulsePoint Fix** | Update all code snippets in documentation and `llms.md` to show `const form = event.currentTarget` cached prior to async calls. |

---

### PP-JAVA-005

| Field | Information |
|---|---|
| **Issue ID** | `PP-JAVA-005` |
| **Area** | Thymeleaf Integration / Template Compilation |
| **Severity** | Medium |
| **Environment** | Thymeleaf 3.1+ / PulsePoint v2 |
| **Steps to Reproduce** | 1. Use Thymeleaf attributes like `th:attr="data-status='{task.status}'"` inside a `<template pp-component>` block.<br>2. Server renders page. |
| **Expected Behavior** | Thymeleaf should pass client template expressions untouched. |
| **Actual Behavior** | Thymeleaf attempts server-side evaluation of `task.status`, which does not exist in Spring's server model (it only exists in the browser's PulsePoint state). |
| **Console Output** | Template compilation warning or empty attribute rendering. |
| **Backend Output** | `org.thymeleaf.exceptions.TemplateProcessingException` |
| **Root Cause** | Mixing server-side Thymeleaf execution directives (`th:*`) inside client-evaluated PulsePoint loop templates (`<template pp-for="task in tasks">`). |
| **PulsePoint Issue?** | No |
| **Java Integration Issue?** | Yes |
| **Documentation Issue?** | Yes |
| **Workaround** | Keep all template markup inside `<template pp-component>` pure HTML with PulsePoint `{...}` expressions. Use Thymeleaf `th:*` attributes strictly outside the reactive component boundaries. |
| **Recommended PulsePoint Fix** | Clearly outline the boundary between server-rendered Thymeleaf outer layout and client-rendered PulsePoint component templates in the documentation. |

---

### PP-JAVA-006

| Field | Information |
|---|---|
| **Issue ID** | `PP-JAVA-006` |
| **Area** | Exception Handling / Static Assets |
| **Severity** | Low |
| **Environment** | Spring Boot 3.2+ |
| **Steps to Reproduce** | 1. Browser requests `/favicon.ico` or chrome devtools json when file does not exist in `static/`.<br>2. Catch-all `@ExceptionHandler(Exception.class)` in `@RestControllerAdvice` triggers. |
| **Expected Behavior** | Server returns HTTP 404 Not Found. |
| **Actual Behavior** | Spring Boot 3.2+ throws `NoResourceFoundException`, which falls into generic 500 error handler, returning HTTP 500. |
| **Console Output** | `favicon.ico:1 Failed to load resource: the server responded with a status of 500 ()` |
| **Backend Output** | `Resolved [org.springframework.web.servlet.resource.NoResourceFoundException: No static resource favicon.ico for request '/favicon.ico'.]` |
| **Root Cause** | Spring Boot 3.2 changed static resource misses to throw `NoResourceFoundException` instead of silently dispatching 404. |
| **PulsePoint Issue?** | No |
| **Java Integration Issue?** | Yes |
| **Documentation Issue?** | No |
| **Workaround** | Add explicit `@ExceptionHandler(NoResourceFoundException.class)` returning 404, permitAll `/favicon.ico`, and include a default static icon file. |
| **Recommended PulsePoint Fix** | Include standard Spring Boot 3.2+ exception handling template in the Java integration guide. |
