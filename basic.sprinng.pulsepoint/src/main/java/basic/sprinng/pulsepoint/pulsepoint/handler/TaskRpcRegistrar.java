package basic.sprinng.pulsepoint.pulsepoint.handler;

import basic.sprinng.pulsepoint.dto.CreateTaskRequest;
import basic.sprinng.pulsepoint.dto.UpdateTaskRequest;
import basic.sprinng.pulsepoint.pulsepoint.PulsePointRpcRegistry;
import basic.sprinng.pulsepoint.pulsepoint.exception.PulsePointValidationException;
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

    public TaskRpcRegistrar(TaskService taskService,
                            PulsePointRpcRegistry registry,
                            Validator validator) {
        this.taskService = taskService;
        this.registry = registry;
        this.validator = validator;
    }

    @PostConstruct
    public void registerFunctions() {
        registry.register("listTasks", this::listTasks);
        registry.register("getTask", this::getTask);
        registry.register("createTask", this::createTask);
        registry.register("updateTask", this::updateTask);
        registry.register("deleteTask", this::deleteTask);
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

        return taskService.createTask(request);
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

        return taskService.updateTask(id, request);
    }

    private Object deleteTask(Map<String, Object> params) {
        Long id = extractLong(params, "id");
        taskService.deleteTask(id);
        return Map.of("success", true, "id", id);
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
