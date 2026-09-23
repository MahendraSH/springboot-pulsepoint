package basic.sprinng.pulsepoint.pulsepoint.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Service for broadcasting real-time task events (created, updated, deleted)
 * to all connected PulsePoint WebSocket clients on the "tasks" channel.
 */
@Component
public class TaskBroadcaster {

    private static final String CHANNEL_TASKS = "tasks";

    private final PulsePointWebSocketHandler webSocketHandler;

    public TaskBroadcaster(PulsePointWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void broadcastTaskCreated(Object task) {
        webSocketHandler.broadcast(CHANNEL_TASKS, Map.of(
                "event", "TASK_CREATED",
                "task", task
        ));
    }

    public void broadcastTaskUpdated(Object task) {
        webSocketHandler.broadcast(CHANNEL_TASKS, Map.of(
                "event", "TASK_UPDATED",
                "task", task
        ));
    }

    public void broadcastTaskDeleted(Long id) {
        webSocketHandler.broadcast(CHANNEL_TASKS, Map.of(
                "event", "TASK_DELETED",
                "id", id
        ));
    }
}
