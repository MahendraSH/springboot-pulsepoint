package basic.sprinng.pulsepoint.service;

import basic.sprinng.pulsepoint.dto.CreateTaskRequest;
import basic.sprinng.pulsepoint.dto.TaskResponse;
import basic.sprinng.pulsepoint.dto.UpdateTaskRequest;
import basic.sprinng.pulsepoint.entity.Task;
import basic.sprinng.pulsepoint.entity.TaskPriority;
import basic.sprinng.pulsepoint.entity.TaskStatus;
import basic.sprinng.pulsepoint.exception.ResourceNotFoundException;
import basic.sprinng.pulsepoint.repository.TaskRepository;
import basic.sprinng.pulsepoint.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Sample description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void listTasks_ShouldReturnAllTasksOrdered() {
        when(taskRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(sampleTask));

        List<TaskResponse> responses = taskService.listTasks();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getTitle()).isEqualTo("Test Task");
        assertThat(responses.getFirst().getStatus()).isEqualTo("TODO");
    }

    @Test
    void getTask_WhenFound_ShouldReturnTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        TaskResponse response = taskService.getTask(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Task");
    }

    @Test
    void getTask_WhenNotFound_ShouldThrowException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Task not found with ID: 99");
    }

    @Test
    void createTask_ShouldSaveAndReturnTaskResponse() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .description("New Description")
                .status("IN_PROGRESS")
                .priority("HIGH")
                .build();

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(2L);
            t.setCreatedAt(LocalDateTime.now());
            t.setUpdatedAt(LocalDateTime.now());
            return t;
        });

        TaskResponse response = taskService.createTask(request);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getTitle()).isEqualTo("New Task");
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getPriority()).isEqualTo("HIGH");
    }

    @Test
    void updateTask_ShouldUpdateFields() {
        UpdateTaskRequest updateReq = UpdateTaskRequest.builder()
                .title("Updated Title")
                .status("DONE")
                .build();

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskResponse response = taskService.updateTask(1L, updateReq);

        assertThat(response.getTitle()).isEqualTo("Updated Title");
        assertThat(response.getStatus()).isEqualTo("DONE");
    }

    @Test
    void deleteTask_ShouldCallRepositoryDelete() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        taskService.deleteTask(1L);

        verify(taskRepository).delete(sampleTask);
    }
}
