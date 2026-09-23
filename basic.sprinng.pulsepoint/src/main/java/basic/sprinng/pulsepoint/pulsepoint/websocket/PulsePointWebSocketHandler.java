package basic.sprinng.pulsepoint.pulsepoint.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Handles PulsePoint Named WebSocket connections (`pp.socket`) at `/__pulsepoint/ws?name=<channelName>`.
 * <p>
 * Implements the PulsePoint heartbeat protocol:
 * - Listens for control frame: `{"__pp": "ping"}`
 * - Immediately replies with: `{"__pp": "pong"}`
 * <p>
 * Maintains active client sessions organized by channel name for multi-client broadcasting.
 */
@Component
public class PulsePointWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(PulsePointWebSocketHandler.class);
    private static final String HEARTBEAT_PONG = "{\"__pp\":\"pong\"}";

    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();

    public PulsePointWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String channel = extractChannelName(session);
        channelSessions.computeIfAbsent(channel, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("PulsePoint WebSocket client connected: session={}, channel={}", session.getId(), channel);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload().trim();

        // Check for PulsePoint heartbeat frame: {"__pp": "ping"}
        if (payload.contains("\"__pp\"") && payload.contains("\"ping\"")) {
            session.sendMessage(new TextMessage(HEARTBEAT_PONG));
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("__pp") && "ping".equals(node.get("__pp").asText())) {
                session.sendMessage(new TextMessage(HEARTBEAT_PONG));
                return;
            }
        } catch (Exception ignored) {
            // Not a JSON heartbeat
        }

        log.debug("Received WebSocket message on session {}: {}", session.getId(), payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String channel = extractChannelName(session);
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions != null) {
            sessions.remove(session);
        }
        log.info("PulsePoint WebSocket client disconnected: session={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("PulsePoint WebSocket transport error on session {}: {}", session.getId(), exception.getMessage());
    }

    /**
     * Broadcasts a JSON message to all active clients subscribed to the specified channel.
     */
    public void broadcast(String channel, Object payload) {
        Set<WebSocketSession> sessions = channelSessions.get(channel);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(message);
                        }
                    } catch (IOException e) {
                        log.warn("Failed to send WebSocket message to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize broadcast payload for channel {}: {}", channel, e.getMessage(), e);
        }
    }

    private String extractChannelName(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null && uri.getQuery() != null) {
            for (String param : uri.getQuery().split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "name".equalsIgnoreCase(pair[0])) {
                    return pair[1].trim();
                }
            }
        }
        return "default";
    }
}
