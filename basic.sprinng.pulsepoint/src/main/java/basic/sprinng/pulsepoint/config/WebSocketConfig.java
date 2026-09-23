package basic.sprinng.pulsepoint.config;

import basic.sprinng.pulsepoint.pulsepoint.websocket.PulsePointWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configures Spring WebSocket endpoints to support PulsePoint Named WebSockets (`pp.socket`).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PulsePointWebSocketHandler pulsePointWebSocketHandler;

    public WebSocketConfig(PulsePointWebSocketHandler pulsePointWebSocketHandler) {
        this.pulsePointWebSocketHandler = pulsePointWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pulsePointWebSocketHandler, "/__pulsepoint/ws")
                .setAllowedOriginPatterns("*");
    }
}
