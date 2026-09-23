package basic.sprinng.pulsepoint.service.impl;

import basic.sprinng.pulsepoint.dto.CreateTaskRequest;
import basic.sprinng.pulsepoint.dto.TaskResponse;
import basic.sprinng.pulsepoint.dto.UpdateTaskRequest;
import basic.sprinng.pulsepoint.entity.Task;
import basic.sprinng.pulsepoint.entity.TaskPriority;
import basic.sprinng.pulsepoint.entity.TaskStatus;
import basic.sprinng.pulsepoint.exception.ResourceNotFoundException;
import basic.sprinng.pulsepoint.repository.TaskRepository;
import basic.sprinng.pulsepoint.service.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks() {
        return taskRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(Long id) {
        Task task = findTaskById(id);
        return mapToResponse(task);
    }

    @Override
    public TaskResponse createTask(CreateTaskRequest request) {
        TaskStatus status = parseStatus(request.getStatus(), TaskStatus.TODO);
        TaskPriority priority = parsePriority(request.getPriority(), TaskPriority.MEDIUM);

        Task task = Task.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .status(status)
                .priority(priority)
                .build();

        Task saved = taskRepository.save(task);
        return mapToResponse(saved);
    }

    @Override
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = findTaskById(id);

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription().trim());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            task.setStatus(parseStatus(request.getStatus(), task.getStatus()));
        }
        if (request.getPriority() != null && !request.getPriority().isBlank()) {
            task.setPriority(parsePriority(request.getPriority(), task.getPriority()));
        }

        Task updated = taskRepository.save(task);
        return mapToResponse(updated);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = findTaskById(id);
        taskRepository.delete(task);
    }

    private Task findTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with ID: " + id));
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .priority(task.getPriority().name())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private TaskStatus parseStatus(String statusStr, TaskStatus defaultStatus) {
        if (statusStr == null || statusStr.isBlank()) {
            return defaultStatus;
        }
        try {
            return TaskStatus.valueOf(statusStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + statusStr + ". Allowed values: TODO, IN_PROGRESS, DONE");
        }
    }

    private TaskPriority parsePriority(String priorityStr, TaskPriority defaultPriority) {
        if (priorityStr == null || priorityStr.isBlank()) {
            return defaultPriority;
        }
        try {
            return TaskPriority.valueOf(priorityStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid priority: " + priorityStr + ". Allowed values: LOW, MEDIUM, HIGH");
        }
    }
}
