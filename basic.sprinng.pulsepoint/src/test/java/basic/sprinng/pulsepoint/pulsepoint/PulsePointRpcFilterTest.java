package basic.sprinng.pulsepoint.pulsepoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import basic.sprinng.pulsepoint.dto.CreateTaskRequest;
import basic.sprinng.pulsepoint.dto.TaskResponse;
import basic.sprinng.pulsepoint.pulsepoint.handler.TaskRpcRegistrar;
import basic.sprinng.pulsepoint.service.TaskService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import basic.sprinng.pulsepoint.entity.TaskPriority;
import basic.sprinng.pulsepoint.entity.TaskStatus;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PulsePointRpcFilterTest {

    private MockMvc mockMvc;

    @Mock
    private TaskService taskService;

    @Mock
    private basic.sprinng.pulsepoint.pulsepoint.websocket.TaskBroadcaster taskBroadcaster;

    private PulsePointRpcRegistry registry;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        registry = new PulsePointRpcRegistry();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        TaskRpcRegistrar registrar = new TaskRpcRegistrar(taskService, registry, validator, taskBroadcaster);
        registrar.registerFunctions();

        PulsePointRpcFilter rpcFilter = new PulsePointRpcFilter(registry, objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(new Object())
                .addFilters(rpcFilter)
                .build();
    }

    @Test
    void rpcCall_ListTasks_ShouldReturnJsonArray() throws Exception {
        TaskResponse task = TaskResponse.builder()
                .id(1L)
                .title("Sample Task")
                .status("TODO")
                .priority("HIGH")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskService.listTasks()).thenReturn(List.of(task));

        mockMvc.perform(post("/tasks")
                        .header("X-PP-RPC", "true")
                        .header("X-PP-Function", "listTasks")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Sample Task"))
                .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    void rpcCall_CreateTask_WithValidPayload_ShouldReturnCreatedTask() throws Exception {
        TaskResponse created = TaskResponse.builder()
                .id(5L)
                .title("New RPC Task")
                .description("Created via PulsePoint")
                .status("IN_PROGRESS")
                .priority("MEDIUM")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(created);

        String jsonPayload = """
                {
                    "title": "New RPC Task",
                    "description": "Created via PulsePoint",
                    "status": "IN_PROGRESS",
                    "priority": "MEDIUM"
                }
                """;

        mockMvc.perform(post("/tasks")
                        .header("X-PP-RPC", "true")
                        .header("X-PP-Function", "createTask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("New RPC Task"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void rpcCall_CreateTask_WithMissingTitle_ShouldReturn400WithFieldErrors() throws Exception {
        String invalidPayload = """
                {
                    "title": "",
                    "description": "No title provided"
                }
                """;

        mockMvc.perform(post("/tasks")
                        .header("X-PP-RPC", "true")
                        .header("X-PP-Function", "createTask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title[0]").value("Title is required"));
    }

    @Test
    void rpcCall_CreateTask_WithTooLongTitle_ShouldReturnSizeValidationError() throws Exception {
        String invalidPayload = String.format("""
                {
                    "title": "%s"
                }
                """, "a".repeat(205));

        mockMvc.perform(post("/tasks")
                        .header("X-PP-RPC", "true")
                        .header("X-PP-Function", "createTask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title[0]").value("Title must not exceed 200 characters"));
    }

    @Test
    void rpcCall_UnknownFunction_ShouldReturn404() throws Exception {
        mockMvc.perform(post("/tasks")
                        .header("X-PP-RPC", "true")
                        .header("X-PP-Function", "nonExistentFunction")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RPC function not found: nonExistentFunction"));
    }

    @Test
    void rpcCall_MissingFunctionHeader_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/tasks")
                        .header("X-PP-RPC", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing X-PP-Function header"));
    }

    @Test
    void rpcCall_DeleteTask_ShouldInvokeServiceAndReturnSuccess() throws Exception {
        String deletePayload = """
                {
                    "id": 42
                }
                """;

        mockMvc.perform(post("/tasks")
                        .header("X-PP-RPC", "true")
                        .header("X-PP-Function", "deleteTask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deletePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value(42));

        verify(taskService).deleteTask(42L);
    }

    @Test
    void rpcCall_StreamTaskAudit_ShouldReturnEventStream() throws Exception {
        TaskResponse sample = new TaskResponse(
                10L, "Sample Task", "Description",
                "TODO", "HIGH",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(taskService.getTask(10L)).thenReturn(sample);

        String payload = "{\"taskId\": 10}";

        mockMvc.perform(post("/tasks")
                        .header("X-PP-RPC", "true")
                        .header("X-PP-Function", "streamTaskAudit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/event-stream;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"percent\":25")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"VERIFIED\"")));
    }

    @Test
    void rpcCall_UploadTaskAttachment_ShouldParseMultipartAndReturnSuccess() throws Exception {
        TaskResponse sample = new TaskResponse(
                10L, "Sample Task", "Description",
                "TODO", "HIGH",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(taskService.getTask(10L)).thenReturn(sample);

        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "file", "report.pdf", "application/pdf", "PDF binary content".getBytes()
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/tasks")
                        .file(mockFile)
                        .param("taskId", "10")
                        .param("note", "Q3 Report")
                        .header("X-PP-RPC", "true")
                        .header("X-PP-Function", "uploadTaskAttachment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.taskId").value(10))
                .andExpect(jsonPath("$.filename").value("report.pdf"))
                .andExpect(jsonPath("$.note").value("Q3 Report"));
    }
}
