package basic.sprinng.pulsepoint.service;

import basic.sprinng.pulsepoint.dto.CreateTaskRequest;
import basic.sprinng.pulsepoint.dto.TaskResponse;
import basic.sprinng.pulsepoint.dto.UpdateTaskRequest;

import java.util.List;

public interface TaskService {
    List<TaskResponse> listTasks();
    TaskResponse getTask(Long id);
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse updateTask(Long id, UpdateTaskRequest request);
    void deleteTask(Long id);
}
