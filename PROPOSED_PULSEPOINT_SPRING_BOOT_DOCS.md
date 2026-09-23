# Official Integration Guide: PulsePoint v2 for Spring Boot

This guide explains how to integrate **PulsePoint v2** into a **Java / Spring Boot** application without Node.js, Webpack, Vite, or a separate frontend build system.

---

## 1. Quick Setup

### Step 1: Add PulsePoint Runtime to Static Assets
Copy `pp-reactive-v2.min.js` to your Spring Boot static resources:

```text
src/main/resources/static/js/pp-reactive-v2.min.js
```

### Step 2: Include in Thymeleaf Template
In your HTML `<head>`:

```html
<script type="module">
    import { ComponentInit as PP } from "/js/pp-reactive-v2.min.js";
    PP.bootstrap();
</script>
```

---

## 2. Spring Security & CSRF Configuration

PulsePoint expects the CSRF token in a non-HttpOnly cookie named `pp_csrf` and sends it back in the `X-CSRF-Token` HTTP header on every RPC request.

Configure your Spring Security filter chain:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieName("pp_csrf");
        csrfRepository.setHeaderName("X-CSRF-Token");

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/error", "/favicon.ico").permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfRepository)
                .csrfTokenRequestHandler(requestHandler)
            )
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    (request, response, authEx) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\":\"Authentication required\"}");
                        response.getWriter().flush();
                    },
                    new RequestHeaderRequestMatcher("X-PP-RPC", "true")
                )
                .defaultAccessDeniedHandlerFor(
                    (request, response, accessEx) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\":\"Permission denied\"}");
                        response.getWriter().flush();
                    },
                    new RequestHeaderRequestMatcher("X-PP-RPC", "true")
                )
            );

        return http.build();
    }
}
```

---

## 3. Reusable RPC Dispatch Infrastructure

PulsePoint sends RPC calls as `POST` requests to the current page URL with `X-PP-RPC: true` and `X-PP-Function: <functionName>`.

### The RPC Function Interface
```java
@FunctionalInterface
public interface PulsePointRpcFunction {
    Object invoke(Map<String, Object> parameters) throws Exception;
}
```

### The RPC Registry
```java
@Component
public class PulsePointRpcRegistry {
    private final Map<String, PulsePointRpcFunction> functions = new ConcurrentHashMap<>();

    public void register(String name, PulsePointRpcFunction function) {
        functions.put(name, function);
    }

    public PulsePointRpcFunction lookup(String name) {
        return functions.get(name);
    }
}
```

### The RPC Servlet Filter
```java
@Component
@Order(20)
public class PulsePointRpcFilter extends OncePerRequestFilter {

    private final PulsePointRpcRegistry registry;
    private final ObjectMapper objectMapper;

    public PulsePointRpcFilter(PulsePointRpcRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod()) ||
            !"true".equalsIgnoreCase(request.getHeader("X-PP-RPC"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String functionName = request.getHeader("X-PP-Function");
        PulsePointRpcFunction function = registry.lookup(functionName);
        if (function == null) {
            sendError(response, HttpStatus.NOT_FOUND, "RPC function not found: " + functionName, null);
            return;
        }

        Map<String, Object> parameters = Collections.emptyMap();
        if (request.getContentLengthLong() > 0) {
            parameters = objectMapper.readValue(request.getInputStream(), new TypeReference<>() {});
        }

        try {
            Object result = function.invoke(parameters);
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), result);
            response.getWriter().flush();
        } catch (Exception ex) {
            sendError(response, HttpStatus.BAD_REQUEST, ex.getMessage(), null);
        }
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String msg, Map<String, Object> errors) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new HashMap<>();
        body.put("error", msg);
        if (errors != null) body.put("errors", errors);
        objectMapper.writeValue(response.getWriter(), body);
        response.getWriter().flush();
    }
}
```

---

## 4. Jackson Date/Time Serialization

When serializing Java 21 `LocalDateTime` objects directly from filters, configure `JavaTimeModule`:

```java
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

---

## 5. Writing Reactive Templates

Author your templates as standard HTML wrapped in `<template pp-component="id">`:

```html
<template pp-component="task_list">
    <div>
        <h3>Tasks ({tasks.length})</h3>

        <form onsubmit="addTask(event)">
            <input name="title" required />
            <button type="submit" disabled="{isSubmitting}">Add</button>
        </form>

        <div hidden="{!error}" style="color: red;">{error}</div>

        <ul>
            <template pp-for="task in tasks">
                <li key="{task.id}">
                    <span>{task.title}</span>
                    <button onclick="removeTask(task.id)">Delete</button>
                </li>
            </template>
        </ul>

        <script>
            const [tasks, setTasks] = pp.state([]);
            const [error, setError] = pp.state('');
            const [isSubmitting, setIsSubmitting] = pp.state(false);

            pp.effect(() => {
                pp.rpc("listTasks").then(setTasks).catch(e => setError(e.message));
            }, []);

            const addTask = async (event) => {
                event.preventDefault();
                const form = event.currentTarget; // Cache form reference before await
                setIsSubmitting(true);
                setError('');
                try {
                    const data = Object.fromEntries(new FormData(form).entries());
                    const created = await pp.rpc("createTask", data);
                    setTasks([created, ...tasks]);
                    form.reset();
                } catch (e) {
                    setError(e.message);
                } finally {
                    setIsSubmitting(false);
                }
            };

            const removeTask = async (id) => {
                try {
                    await pp.rpc("deleteTask", { id });
                    setTasks(tasks.filter(t => t.id !== id));
                } catch (e) {
                    setError(e.message);
                }
            };
        </script>
    </div>
</template>
```

---

## 6. Important Best Practices & Gotchas

1. **Avoid Thymeleaf `th:*` inside `<template pp-for>`**: Loop elements are generated client-side by PulsePoint. Mixing server-side Thymeleaf attributes inside client loops can cause template processing exceptions.
2. **Always Quote Attribute Expressions**: Write `key="{task.id}"` and `class="badge {task.status}"`. Unquoted braces (`key={task.id}`) break HTML parsing.
3. **Always Flush Response Writers**: When writing JSON directly via `objectMapper.writeValue(response.getWriter(), ...)`, always invoke `response.getWriter().flush()`.
4. **Cache Event Targets Prior to Await**: Native browser events may clear `event.currentTarget` after an asynchronous promise resolution. Save `const form = event.currentTarget;` before any `await pp.rpc(...)`.
