package basic.sprinng.pulsepoint.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Gatling Real-Time Simulation for Spring Boot + PulsePoint v2.
 * Focuses on concurrency and resilience of:
 * - Named WebSockets (/__pulsepoint/ws?name=tasks)
 * - Heartbeat Ping/Pong frames
 * - SSE Streaming (streamTaskAudit)
 */
public class PulsePointRealtimeSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final String WS_URL = System.getProperty("wsUrl", "ws://localhost:8080");
    private static final int CONCURRENT_SUBSCRIBERS = Integer.getInteger("users", 30);
    private static final int RAMP_DURATION = Integer.getInteger("ramp", 5);

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .wsBaseUrl(WS_URL)
            .acceptHeader("application/json, text/event-stream, */*");

    // 1. Auth chain
    private final ChainBuilder authChain = exec(
            http("RT_01_Get_CSRF")
                    .get("/login")
                    .check(status().is(200))
                    .check(css("input[name='_csrf']", "value").saveAs("loginCsrf"))
    ).exec(
            http("RT_02_Login")
                    .post("/login")
                    .disableFollowRedirect()
                    .formParam("username", "demo")
                    .formParam("password", "demo123")
                    .formParam("_csrf", "#{loginCsrf}")
                    .check(status().is(302))
                    .check(headerRegex("Set-Cookie", "pp_csrf=([a-f0-9\\-]+)").saveAs("ppCsrfToken"))
    );

    // 2. Real-time WebSocket + SSE scenario
    private final ScenarioBuilder realtimeScenario = scenario("PulsePoint Real-Time Channel Scenario")
            .exec(authChain)
            // Connect to Named WebSocket channel
            .exec(ws("RT_03_WS_Connect").connect("/__pulsepoint/ws?name=tasks"))
            .pause(Duration.ofMillis(300))
            // Send Heartbeat Ping #1
            .exec(
                    ws("RT_04_WS_Ping_1")
                            .sendText("{\"__pp\":\"ping\"}")
                            .await(Duration.ofSeconds(3))
                            .on(
                                    ws.checkTextMessage("RT_05_WS_Pong_1")
                                            .check(regex(".*pong.*"))
                            )
            )
            .pause(Duration.ofMillis(500))
            // Concurrently invoke SSE streaming RPC while holding WebSocket open
            .exec(
                    http("RT_06_SSE_streamTaskAudit")
                            .post("/__pulsepoint/rpc")
                            .header("X-PP-RPC", "true")
                            .header("X-PP-Function", "streamTaskAudit")
                            .header("X-CSRF-Token", "#{ppCsrfToken}")
                            .header("Accept", "text/event-stream")
                            .header("Content-Type", "application/json")
                            .body(StringBody("{\"taskId\":1}"))
                            .check(status().is(200))
                            .check(substring("VERIFIED"))
            )
            .pause(Duration.ofMillis(500))
            // Send Heartbeat Ping #2
            .exec(
                    ws("RT_07_WS_Ping_2")
                            .sendText("{\"__pp\":\"ping\"}")
                            .await(Duration.ofSeconds(3))
                            .on(
                                    ws.checkTextMessage("RT_08_WS_Pong_2")
                                            .check(regex(".*pong.*"))
                            )
            )
            .pause(Duration.ofMillis(300))
            .exec(ws("RT_09_WS_Close").close());

    {
        setUp(
                realtimeScenario.injectOpen(
                        rampUsers(CONCURRENT_SUBSCRIBERS).during(Duration.ofSeconds(RAMP_DURATION))
                )
        ).protocols(httpProtocol)
         .assertions(
                 global().successfulRequests().percent().gt(98.0)
         );
    }
}
