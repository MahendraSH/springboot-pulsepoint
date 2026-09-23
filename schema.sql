-- =============================================================================
-- Schema and Seed Data for Spring Boot + PulsePoint Task Application
-- Matching PLAN.md specifications
-- =============================================================================

-- Drop table if already exists
DROP TABLE IF EXISTS tasks;

-- 1. Create Tasks Table Schema
CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_task_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT chk_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);

-- 2. Insert 40 Realistic Seed Records
INSERT INTO tasks (title, description, status, priority, created_at, updated_at) VALUES
('Set up Spring Boot 3 skeleton', 'Initialize Maven multi-module structure with Java 21 and Spring Boot 3.x dependencies', 'DONE', 'HIGH', NOW() - INTERVAL '15 days', NOW() - INTERVAL '14 days'),
('Configure PostgreSQL connection pool', 'Add HikariCP tuning parameters and connection pooling settings in application.yaml', 'DONE', 'HIGH', NOW() - INTERVAL '14 days', NOW() - INTERVAL '13 days'),
('Design JPA entities and relationships', 'Define Task, User, and TaskHistory JPA entities with appropriate constraints and indices', 'DONE', 'MEDIUM', NOW() - INTERVAL '13 days', NOW() - INTERVAL '12 days'),
('Implement Spring Security session login', 'Configure form login, custom authentication entry point, and session fixation protection', 'DONE', 'HIGH', NOW() - INTERVAL '12 days', NOW() - INTERVAL '11 days'),
('Enable CSRF token filter', 'Configure CookieCsrfTokenRepository to expose pp_csrf cookie for PulsePoint client', 'DONE', 'HIGH', NOW() - INTERVAL '11 days', NOW() - INTERVAL '10 days'),
('Create TaskService interface and implementation', 'Implement core CRUD methods with validation and transactional boundaries', 'DONE', 'MEDIUM', NOW() - INTERVAL '10 days', NOW() - INTERVAL '9 days'),
('Build Global Exception Handler', 'Catch ResourceNotFoundException, MethodArgumentNotValidException and map to RFC 7807 JSON format', 'DONE', 'MEDIUM', NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days'),
('Set up PulsePoint RPC dispatch filter', 'Intercept X-PP-RPC headers and route incoming RPC functions to registered handlers', 'IN_PROGRESS', 'HIGH', NOW() - INTERVAL '8 days', NOW() - INTERVAL '1 day'),
('Implement TaskRpcRegistrar', 'Register listTasks, getTask, createTask, updateTask, and deleteTask handlers', 'IN_PROGRESS', 'HIGH', NOW() - INTERVAL '7 days', NOW() - INTERVAL '2 days'),
('Integrate pp-reactive-v2.min.js runtime', 'Bundle PulsePoint frontend script into static resources and load in Thymeleaf template', 'IN_PROGRESS', 'MEDIUM', NOW() - INTERVAL '6 days', NOW() - INTERVAL '1 day'),
('Design Thymeleaf login template', 'Clean modern login UI with flash error messages and CSRF hidden field', 'DONE', 'LOW', NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days'),
('Build Task management reactive component', 'Use pp-component="task_list" with pp.state() and pp.effect() to render task list', 'IN_PROGRESS', 'HIGH', NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day'),
('Implement task search and filter UI', 'Add search input and priority filter dropdown with client-side reactive state', 'TODO', 'MEDIUM', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
('Add pagination support in TaskRepository', 'Create Pageable queries and return PaginatedResponse DTO to PulsePoint client', 'TODO', 'MEDIUM', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
('Write TaskService unit tests', 'Mock repository calls using Mockito to test CRUD operations and edge cases', 'DONE', 'HIGH', NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days'),
('Write PulsePoint RPC integration tests', 'Use MockMvc to verify RPC headers, function dispatching, and error payloads', 'IN_PROGRESS', 'HIGH', NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day'),
('Implement task priority badge styling', 'Add CSS badges (green for LOW, amber for MEDIUM, red for HIGH) in tasks.html', 'DONE', 'LOW', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days'),
('Add confirmation modal for task deletion', 'Prompt user before triggering deleteTask RPC call to prevent accidental data loss', 'TODO', 'LOW', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
('Validate maximum length constraints on title', 'Add @Size(max=200) on CreateTaskRequest and check client-side error rendering', 'DONE', 'MEDIUM', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days'),
('Add audit timestamps listener', 'Verify @PrePersist and @PreUpdate lifecycle hooks properly populate created_at and updated_at', 'DONE', 'LOW', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
('Optimize PostgreSQL queries with composite indices', 'Add index on (status, priority, created_at) to speed up filtered task queries', 'TODO', 'MEDIUM', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
('Implement task status inline dropdown', 'Allow changing task status (TODO -> IN_PROGRESS -> DONE) directly from the table row', 'TODO', 'MEDIUM', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),
('Add toast notification system', 'Display subtle feedback banner on successful task creation, edit, or deletion', 'TODO', 'LOW', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
('Configure logging with Logback', 'Define structured JSON logging appenders for RPC requests and security events', 'TODO', 'LOW', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
('Implement rate limiting on RPC endpoints', 'Add Bucket4j token bucket rate limiter to prevent abuse on /rpc endpoints', 'TODO', 'HIGH', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
('Test CSRF token expiry handling', 'Verify that expired CSRF tokens return standard 403 Forbidden with prompt to re-authenticate', 'TODO', 'HIGH', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
('Add bulk task deletion feature', 'Support multi-row selection with checkbox and execute batch delete in a single RPC call', 'TODO', 'MEDIUM', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
('Create dark mode theme toggle', 'Persist theme preference in localStorage and apply CSS variables across the application', 'TODO', 'LOW', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
('Refactor DTO mapping to MapStruct', 'Replace manual builder/setter mapping with type-safe MapStruct mapper interfaces', 'TODO', 'LOW', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
('Implement task export to CSV', 'Generate and stream CSV download of all tasks with filtered criteria', 'TODO', 'LOW', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
('Add health check endpoint via Spring Actuator', 'Expose /actuator/health with custom database connection readiness probe', 'DONE', 'MEDIUM', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
('Support markdown rendering in task descriptions', 'Render markdown formatting safely using DOMPurify on the task details modal', 'TODO', 'LOW', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '18 hours'),
('Implement keyboard shortcuts for power users', 'Support "N" for new task modal, "/" for search bar focus, and "Esc" to close modals', 'TODO', 'LOW', NOW() - INTERVAL '16 hours', NOW() - INTERVAL '16 hours'),
('Configure database connection retry logic', 'Add resilience4j retry mechanism on transient database connection timeouts', 'TODO', 'MEDIUM', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours'),
('Conduct end-to-end integration test suite', 'Run full Cypress / Playwright test suite against Spring Boot + PulsePoint UI', 'TODO', 'HIGH', NOW() - INTERVAL '10 hours', NOW() - INTERVAL '10 hours'),
('Benchmark RPC latency vs REST', 'Profile network roundtrip and JSON serialization overhead of PulsePoint RPC protocol', 'TODO', 'MEDIUM', NOW() - INTERVAL '8 hours', NOW() - INTERVAL '8 hours'),
('Prepare deployment Dockerfile', 'Create multi-stage distroless Java 21 Dockerfile optimized for low memory footprint', 'TODO', 'MEDIUM', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
('Write integration documentation in README.md', 'Document setup steps, RPC wire protocol structure, and troubleshooting tips', 'TODO', 'HIGH', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '4 hours'),
('Review security headers configuration', 'Ensure Strict-Transport-Security, X-Frame-Options, and Content-Security-Policy are active', 'TODO', 'HIGH', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours'),
('Final release smoke testing', 'Perform complete manual walk-through of all CRUD and edge cases before sign-off', 'TODO', 'HIGH', NOW(), NOW());
