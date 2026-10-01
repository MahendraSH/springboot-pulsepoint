package basic.sprinng.pulsepoint.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Gatling Load & Performance Simulation for Spring Boot + PulsePoint v2.
 * Validates:
 * 1. Form Authentication & CSRF token negotiation
 * 2. PulsePoint RPC CRUD operations (listTasks, createTask, updateTask, getTask, deleteTask)
 * 3. PulsePoint SSE real-time streaming (streamTaskAudit)
 * 4. PulsePoint Named WebSocket full-duplex connection & heartbeat protocol
 */
public class PulsePointFullLoadSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String WS_URL = System.getProperty("wsUrl", "ws://localhost:8080");
    private static final int CONCURRENT_USERS = Integer.getInteger("users", 50);
    private static final int RAMP_DURATION = Integer.getInteger("ramp", 10);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .wsBaseUrl(WS_URL)
            .acceptHeader("application/json, text/plain, */*")
            .acceptLanguageHeader("en-US,en;q=0.5")
            .userAgentHeader("Gatling/PulsePoint-LoadTester");

    private static final AtomicInteger TASK_COUNTER = new AtomicInteger(1000);

    private final Iterator<Map<String, Object>> taskFeeder = Stream.generate(() -> {
        int id = TASK_COUNTER.incrementAndGet();
        return Map.<String, Object>of(
                "taskTitle", "Gatling Task " + id,
                "taskDesc", "Automated stress verification task #" + id,
                "taskPriority", (id % 2 == 0) ? "HIGH" : "MEDIUM"
        );
    }).iterator();

    // 1. Authentication Scenario
    private final ChainBuilder loginChain = exec(
            http("01_Get_LoginPage")
                    .get("/login")
                    .check(status().is(200))
                    .check(css("input[name='_csrf']", "value").saveAs("loginCsrf"))
    ).pause(Duration.ofMillis(200))
    .exec(
            http("02_Submit_Login")
                    .post("/login")
                    .disableFollowRedirect()
                    .formParam("username", "demo")
                    .formParam("password", "demo123")
                    .formParam("_csrf", "#{loginCsrf}")
                    .check(status().is(302))
                    .check(headerRegex("Set-Cookie", "pp_csrf=([a-f0-9\\-]+)").saveAs("ppCsrfToken"))
    ).exec(
            http("03_Visit_TasksDashboard")
                    .get("/tasks")
                    .check(status().is(200))
    );

    // 2. PulsePoint RPC Scenario
    private final ChainBuilder rpcChain = feed(taskFeeder)
            // RPC listTasks
            .exec(
                    http("04_RPC_listTasks")
                            .post("/__pulsepoint/rpc")
                            .header("X-PP-RPC", "true")
                            .header("X-PP-Function", "listTasks")
                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                            .header("Content-Type", "application/json")
                            .body(StringBody("{}"))
                            .check(status().is(200))
            )
            .pause(Duration.ofMillis(300))
            // RPC createTask
            .exec(
                    http("05_RPC_createTask")
                            .post("/__pulsepoint/rpc")
                            .header("X-PP-RPC", "true")
                            .header("X-PP-Function", "createTask")
                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                            .header("Content-Type", "application/json")
                            .body(StringBody("{\"title\":\"#{taskTitle}\",\"description\":\"#{taskDesc}\",\"status\":\"TODO\",\"priority\":\"#{taskPriority}\"}"))
                            .check(status().is(200))
                            .check(jsonPath("$.id").saveAs("createdTaskId"))
            )
            .pause(Duration.ofMillis(200))
            // RPC getTask
            .exec(
                    http("06_RPC_getTask")
                            .post("/__pulsepoint/rpc")
                            .header("X-PP-RPC", "true")
                            .header("X-PP-Function", "getTask")
                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                            .header("Content-Type", "application/json")
                            .body(StringBody("{\"id\":#{createdTaskId}}"))
                            .check(status().is(200))
                            .check(jsonPath("$.title").is(session -> session.getString("taskTitle")))
            )
            .pause(Duration.ofMillis(200))
            // RPC updateTask
            .exec(
                    http("07_RPC_updateTask")
                            .post("/__pulsepoint/rpc")
                            .header("X-PP-RPC", "true")
                            .header("X-PP-Function", "updateTask")
                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                            .header("Content-Type", "application/json")
                            .body(StringBody("{\"id\":#{createdTaskId},\"title\":\"#{taskTitle} [Updated]\",\"description\":\"#{taskDesc}\",\"status\":\"IN_PROGRESS\",\"priority\":\"HIGH\"}"))
                            .check(status().is(200))
                            .check(jsonPath("$.status").is("IN_PROGRESS"))
            )
            .pause(Duration.ofMillis(200))
            // RPC deleteTask
            .exec(
                    http("08_RPC_deleteTask")
                            .post("/__pulsepoint/rpc")
                            .header("X-PP-RPC", "true")
                            .header("X-PP-Function", "deleteTask")
                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                            .header("Content-Type", "application/json")
                            .body(StringBody("{\"id\":#{createdTaskId}}"))
                            .check(status().is(200))
                            .check(jsonPath("$.success").is("true"))
            );

    // 3. PulsePoint SSE Streaming Scenario
    private final ChainBuilder sseChain = exec(
            http("09_SSE_streamTaskAudit")
                    .post("/__pulsepoint/rpc")
                    .header("X-PP-RPC", "true")
                    .header("X-PP-Function", "streamTaskAudit")
                    .header("X-CSRF-Token", "#{ppCsrfToken}")
                    .header("Accept", "text/event-stream")
                    .header("Content-Type", "application/json")
                    .body(StringBody("{\"taskId\":1}"))
                    .check(status().is(200))
                    .check(substring("VERIFIED"))
    );

    // 4. PulsePoint WebSocket Scenario
    private final ChainBuilder webSocketChain = exec(
            ws("10_WS_Connect").connect("/__pulsepoint/ws?name=tasks")
    ).pause(Duration.ofMillis(500))
    .exec(
            ws("11_WS_HeartbeatPing")
                    .sendText("{\"__pp\":\"ping\"}")
                    .await(Duration.ofSeconds(3))
                    .on(
                            ws.checkTextMessage("12_WS_HeartbeatPong")
                                    .check(regex(".*pong.*"))
                    )
    ).pause(Duration.ofMillis(500))
    .exec(
            ws("13_WS_Close").close()
    );

    // Complete End-to-End User Scenario
    private final ScenarioBuilder userJourney = scenario("PulsePoint Full E2E User Journey")
            .exec(loginChain)
            .exec(webSocketChain)
            .exec(rpcChain)
            .exec(sseChain);

    {
        setUp(
                userJourney.injectOpen(
                        rampUsers(CONCURRENT_USERS).during(Duration.ofSeconds(RAMP_DURATION))
                )
        ).protocols(httpProtocol)
         .assertions(
                 global().successfulRequests().percent().gt(95.0),
                 global().responseTime().percentile3().lt(2000) // 95th percentile < 2s
         );
    }
}
