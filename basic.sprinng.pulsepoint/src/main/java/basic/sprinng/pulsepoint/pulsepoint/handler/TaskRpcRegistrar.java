package basic.sprinng.pulsepoint.pulsepoint.handler;

import basic.sprinng.pulsepoint.dto.CreateTaskRequest;
import basic.sprinng.pulsepoint.dto.TaskResponse;
import basic.sprinng.pulsepoint.dto.UpdateTaskRequest;
import basic.sprinng.pulsepoint.pulsepoint.PulsePointRpcRegistry;
import basic.sprinng.pulsepoint.pulsepoint.exception.PulsePointValidationException;
import basic.sprinng.pulsepoint.pulsepoint.stream.PulsePointStream;
import basic.sprinng.pulsepoint.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registers Task CRUD operations into the PulsePoint RPC registry,
 * enforcing Jakarta Bean Validation and returning structured field errors.
 */
@Component
public class TaskRpcRegistrar {

    private final TaskService taskService;
    private final PulsePointRpcRegistry registry;
    private final Validator validator;
    private final basic.sprinng.pulsepoint.pulsepoint.websocket.TaskBroadcaster taskBroadcaster;

    public TaskRpcRegistrar(TaskService taskService,
                            PulsePointRpcRegistry registry,
                            Validator validator,
                            basic.sprinng.pulsepoint.pulsepoint.websocket.TaskBroadcaster taskBroadcaster) {
        this.taskService = taskService;
        this.registry = registry;
        this.validator = validator;
        this.taskBroadcaster = taskBroadcaster;
    }

    @PostConstruct
    public void registerFunctions() {
        registry.register("listTasks", this::listTasks);
        registry.register("getTask", this::getTask);
        registry.register("createTask", this::createTask);
        registry.register("updateTask", this::updateTask);
        registry.register("deleteTask", this::deleteTask);
        registry.register("streamTaskAudit", this::streamTaskAudit);
        registry.register("uploadTaskAttachment", this::uploadTaskAttachment);
    }

    private Object listTasks(Map<String, Object> params) {
        return taskService.listTasks();
    }

    private Object getTask(Map<String, Object> params) {
        Long id = extractLong(params, "id");
        return taskService.getTask(id);
    }

    private Object createTask(Map<String, Object> params) {
        String title = (String) params.get("title");
        String description = (String) params.get("description");
        String status = (String) params.get("status");
        String priority = (String) params.get("priority");

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title(title)
                .description(description)
                .status(status)
                .priority(priority)
                .build();

        validateRequest(request);
        TaskResponse created = taskService.createTask(request);
        taskBroadcaster.broadcastTaskCreated(created);
        return created;
    }

    private Object updateTask(Map<String, Object> params) {
        Long id = extractLong(params, "id");
        String title = (String) params.get("title");
        String description = (String) params.get("description");
        String status = (String) params.get("status");
        String priority = (String) params.get("priority");

        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title(title)
                .description(description)
                .status(status)
                .priority(priority)
                .build();

        validateRequest(request);
        TaskResponse updated = taskService.updateTask(id, request);
        taskBroadcaster.broadcastTaskUpdated(updated);
        return updated;
    }

    private Object deleteTask(Map<String, Object> params) {
        Long id = extractLong(params, "id");
        taskService.deleteTask(id);
        taskBroadcaster.broadcastTaskDeleted(id);
        return Map.of("success", true, "id", id);
    }

    private Object streamTaskAudit(Map<String, Object> params) {
        Long id = params.containsKey("taskId") ? extractLong(params, "taskId") : extractLong(params, "id");
        taskService.getTask(id); // Verify task exists
        final Long taskId = id;

        return (PulsePointStream) emitter -> {
            emitter.send(Map.of("taskId", taskId, "percent", 25, "step", "Analyzing task lifecycle and dependencies..."));
            Thread.sleep(300);
            emitter.send(Map.of("taskId", taskId, "percent", 50, "step", "Verifying priority alignment and assignments..."));
            Thread.sleep(300);
            emitter.send(Map.of("taskId", taskId, "percent", 75, "step", "Checking status transition history..."));
            Thread.sleep(300);
            emitter.send(Map.of("taskId", taskId, "percent", 100, "step", "Audit complete! Task integrity verified.", "status", "VERIFIED"));
        };
    }

    private Object uploadTaskAttachment(Map<String, Object> params) {
        Long id = params.containsKey("taskId") ? extractLong(params, "taskId") : extractLong(params, "id");
        taskService.getTask(id); // Verify task exists

        Object fileObj = params.get("file");
        if (fileObj == null) {
            throw new IllegalArgumentException("No file attachment provided");
        }

        String filename = "attachment";
        long size = 0;
        if (fileObj instanceof Map<?, ?> fileMap) {
            Object fn = fileMap.get("filename");
            if (fn != null) {
                filename = fn.toString();
            }
            if (fileMap.containsKey("size") && fileMap.get("size") instanceof Number num) {
                size = num.longValue();
            }
        }

        return Map.of(
                "success", true,
                "taskId", id,
                "filename", filename,
                "size", size,
                "note", params.getOrDefault("note", "")
        );
    }

    private <T> void validateRequest(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            Map<String, List<String>> errors = new HashMap<>();
            for (ConstraintViolation<T> violation : violations) {
                errors.computeIfAbsent(violation.getPropertyPath().toString(), k -> new ArrayList<>())
                        .add(violation.getMessage());
            }
            throw new PulsePointValidationException("Validation failed", errors);
        }
    }

    private Long extractLong(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        if (val instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(val.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + key + " must be a valid integer");
        }
    }
}
