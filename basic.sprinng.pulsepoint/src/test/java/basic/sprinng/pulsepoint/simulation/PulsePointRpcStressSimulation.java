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
 * Gatling High-Throughput RPC Stress Test for Spring Boot + PulsePoint.
 * Stresses:
 * - Filter chain overhead (PulsePointRpcFilter)
 * - SecurityContext persistence across RPC calls
 * - Service & Repository query performance under concurrent load
 * - CSRF verification under high concurrency
 */
public class PulsePointRpcStressSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int CONCURRENT_USERS = Integer.getInteger("users", 100);
    private static final int RAMP_DURATION = Integer.getInteger("ramp", 15);
    private static final int REPEAT_COUNT = Integer.getInteger("repeat", 5);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .shareConnections();

    private static final AtomicInteger COUNTER = new AtomicInteger(5000);

    private final Iterator<Map<String, Object>> feeder = Stream.generate(() -> {
        int id = COUNTER.incrementAndGet();
        return Map.<String, Object>of(
                "title", "Stress Task " + id,
                "desc", "High volume stress testing payload " + id
        );
    }).iterator();

    private final ScenarioBuilder rpcStressScenario = scenario("PulsePoint RPC Concurrency Stress")
            // 1. Initial Login to establish authenticated session
            .exec(
                    http("Stress_01_Get_CSRF")
                            .get("/login")
                            .check(status().is(200))
                            .check(css("input[name='_csrf']", "value").saveAs("loginCsrf"))
            )
            .exec(
                    http("Stress_02_Login")
                            .post("/login")
                            .disableFollowRedirect()
                            .formParam("username", "demo")
                            .formParam("password", "demo123")
                            .formParam("_csrf", "#{loginCsrf}")
                            .check(status().is(302))
                            .check(headerRegex("Set-Cookie", "pp_csrf=([a-f0-9\\-]+)").saveAs("ppCsrfToken"))
            )
            // 2. High Frequency RPC loop
            .repeat(REPEAT_COUNT).on(
                    feed(feeder)
                            .exec(
                                    http("Stress_03_RPC_listTasks")
                                            .post("/__pulsepoint/rpc")
                                            .header("X-PP-RPC", "true")
                                            .header("X-PP-Function", "listTasks")
                                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                                            .header("Content-Type", "application/json")
                                            .body(StringBody("{}"))
                                            .check(status().is(200))
                            )
                            .pause(Duration.ofMillis(50))
                            .exec(
                                    http("Stress_04_RPC_createTask")
                                            .post("/__pulsepoint/rpc")
                                            .header("X-PP-RPC", "true")
                                            .header("X-PP-Function", "createTask")
                                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                                            .header("Content-Type", "application/json")
                                            .body(StringBody("{\"title\":\"#{title}\",\"description\":\"#{desc}\",\"status\":\"TODO\",\"priority\":\"HIGH\"}"))
                                            .check(status().is(200))
                                            .check(jsonPath("$.id").saveAs("taskId"))
                            )
                            .pause(Duration.ofMillis(50))
                            .exec(
                                    http("Stress_05_RPC_updateTask")
                                            .post("/__pulsepoint/rpc")
                                            .header("X-PP-RPC", "true")
                                            .header("X-PP-Function", "updateTask")
                                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                                            .header("Content-Type", "application/json")
                                            .body(StringBody("{\"id\":#{taskId},\"title\":\"#{title} [DONE]\",\"description\":\"#{desc}\",\"status\":\"DONE\",\"priority\":\"LOW\"}"))
                                            .check(status().is(200))
                            )
                            .pause(Duration.ofMillis(50))
                            .exec(
                                    http("Stress_06_RPC_deleteTask")
                                            .post("/__pulsepoint/rpc")
                                            .header("X-PP-RPC", "true")
                                            .header("X-PP-Function", "deleteTask")
                                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                                            .header("Content-Type", "application/json")
                                            .body(StringBody("{\"id\":#{taskId}}"))
                                            .check(status().is(200))
                            )
            );

    {
        setUp(
                rpcStressScenario.injectOpen(
                        rampUsers(CONCURRENT_USERS).during(Duration.ofSeconds(RAMP_DURATION))
                )
        ).protocols(httpProtocol)
         .assertions(
                 global().successfulRequests().percent().gt(98.0),
                 global().responseTime().percentile3().lt(500) // 95% of requests under 500ms
         );
    }
}
