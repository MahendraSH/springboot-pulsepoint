package basic.sprinng.pulsepoint.pulsepoint.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PulsePointWebSocketHandlerTest {

    private PulsePointWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new PulsePointWebSocketHandler(objectMapper);
    }

    @Test
    void handleTextMessage_WithPing_ShouldReplyWithPong() throws Exception {
        TextMessage pingMessage = new TextMessage("{\"__pp\":\"ping\"}");

        handler.handleTextMessage(session1, pingMessage);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(captor.capture());

        assertEquals("{\"__pp\":\"pong\"}", captor.getValue().getPayload());
    }

    @Test
    void broadcast_ShouldSendToSubscribedSessions() throws Exception {
        when(session1.getUri()).thenReturn(URI.create("ws://localhost:8080/__pulsepoint/ws?name=tasks"));
        when(session1.isOpen()).thenReturn(true);
        when(session2.getUri()).thenReturn(URI.create("ws://localhost:8080/__pulsepoint/ws?name=tasks"));
        when(session2.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);

        handler.broadcast("tasks", Map.of("event", "TASK_CREATED", "id", 99));

        ArgumentCaptor<TextMessage> captor1 = ArgumentCaptor.forClass(TextMessage.class);
        ArgumentCaptor<TextMessage> captor2 = ArgumentCaptor.forClass(TextMessage.class);

        verify(session1).sendMessage(captor1.capture());
        verify(session2).sendMessage(captor2.capture());

        assertTrue(captor1.getValue().getPayload().contains("\"TASK_CREATED\""));
        assertTrue(captor2.getValue().getPayload().contains("\"id\":99"));
    }

    @Test
    void afterConnectionClosed_ShouldRemoveSession() throws Exception {
        when(session1.getUri()).thenReturn(URI.create("ws://localhost:8080/__pulsepoint/ws?name=tasks"));

        handler.afterConnectionEstablished(session1);
        handler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        handler.broadcast("tasks", Map.of("event", "PING"));

        // No message should be sent since session was closed and removed
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }
}
