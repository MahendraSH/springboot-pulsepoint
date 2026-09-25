# Spring Boot Monolith — PulsePoint Application Module

This directory contains the Spring Boot 3.x / Java 21 application implementing the PulsePoint v2 integration reference architecture.

For full project documentation, design plans, evaluation reports, and E2E verification videos, please refer to the [Root README (`../README.md`)](../README.md) and [Documentation Hub (`../docs/README.md`)](../docs/README.md).

---

## 🚀 Quick Execution

### Prerequisites
- **Java 21+**
- **PostgreSQL** running locally on port `5432` with user `postgres` / password `postgres`
- Database initialized via:
  ```bash
  createdb pulsepoint_db
  psql -d pulsepoint_db -f ../schema.sql
  ```

### Run Application
```bash
./mvnw spring-boot:run
```

Access at: **[http://localhost:8080](http://localhost:8080)**  
Credentials: `demo` / `demo123`

### Run Automated Tests (22 tests)
```bash
./mvnw clean test
```

---

## 📦 Package Layout

```text
src/main/java/basic/sprinng/pulsepoint/
├── Application.java            # Spring Boot entry point
├── config/                     # SecurityConfig, JacksonConfig, WebSocketConfig
├── controller/                 # TaskController (Thymeleaf views)
├── dto/                        # CreateTaskRequest, UpdateTaskRequest, TaskResponse
├── entity/                     # Task, TaskStatus, TaskPriority
├── exception/                  # GlobalExceptionHandler, ResourceNotFoundException, ConflictException
├── pulsepoint/                 # Reusable PulsePoint Java integration layer
│   ├── PulsePointRpcFilter.java
│   ├── PulsePointRpcRegistry.java
│   ├── PulsePointRpcFunction.java
│   ├── PulsePointCsrfFilter.java
│   ├── exception/              # PulsePointValidationException
│   ├── stream/                 # PulsePointStream, PulsePointStreamEmitter
│   ├── websocket/              # PulsePointWebSocketHandler, TaskBroadcaster
│   └── handler/                # TaskRpcRegistrar
├── repository/                 # TaskRepository (Spring Data JPA)
└── service/                    # TaskService and TaskServiceImpl
```
