
package basic.sprinng.pulsepoint.pulsepoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import basic.sprinng.pulsepoint.exception.ConflictException;
import basic.sprinng.pulsepoint.exception.ResourceNotFoundException;
import basic.sprinng.pulsepoint.pulsepoint.exception.PulsePointValidationException;
import basic.sprinng.pulsepoint.pulsepoint.stream.PulsePointStream;
import basic.sprinng.pulsepoint.pulsepoint.stream.PulsePointStreamEmitter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter that intercepts PulsePoint RPC requests (POST with `X-PP-RPC: true` header)
 * and dispatches them to registered RPC functions.
 */
@Component
@Order(20)
public class PulsePointRpcFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PulsePointRpcFilter.class);

    private static final String HEADER_RPC = "X-PP-RPC";
    private static final String HEADER_FUNCTION = "X-PP-Function";

    private final PulsePointRpcRegistry registry;
    private final ObjectMapper objectMapper;

    public PulsePointRpcFilter(PulsePointRpcRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Check if this is a PulsePoint RPC request
        if (!isRpcRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String functionName = request.getHeader(HEADER_FUNCTION);
        if (functionName == null || functionName.isBlank()) {
            sendError(response, HttpStatus.BAD_REQUEST, "Missing X-PP-Function header", null);
            return;
        }

        PulsePointRpcFunction function = registry.lookup(functionName);
        if (function == null) {
            log.warn("PulsePoint RPC function not found: {}", functionName);
            sendError(response, HttpStatus.NOT_FOUND, "RPC function not found: " + functionName, null);
            return;
        }

        Map<String, Object> parameters;
        try {
            parameters = parseParameters(request);
        } catch (Exception e) {
            log.error("Failed to parse RPC payload for function: {}", functionName, e);
            sendError(response, HttpStatus.BAD_REQUEST, "Malformed request body", null);
            return;
        }

        try {
            Object result = function.invoke(parameters != null ? parameters : Collections.emptyMap());
            if (result instanceof PulsePointStream stream) {
                sendStream(response, stream);
            } else {
                sendSuccess(response, result);
            }
        } catch (PulsePointValidationException e) {
            log.warn("Field validation error during RPC execution: {}", e.getMessage());
            sendError(response, HttpStatus.BAD_REQUEST, e.getMessage(), e.getErrors());
        } catch (ConflictException e) {
            log.warn("Conflict during RPC execution: {}", e.getMessage());
            sendError(response, HttpStatus.CONFLICT, e.getMessage(), null);
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found during RPC execution: {}", e.getMessage());
            sendError(response, HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error during RPC execution: {}", e.getMessage());
            sendError(response, HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            log.error("Unhandled error during RPC execution of {}: {}", functionName, e.getMessage(), e);
            sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error: " + e.getMessage(), null);
        }
    }

    private boolean isRpcRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) &&
                "true".equalsIgnoreCase(request.getHeader(HEADER_RPC));
    }

    private void sendSuccess(HttpServletResponse response, Object result) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), result);
        response.getWriter().flush();
    }

    private void sendStream(HttpServletResponse response, PulsePointStream stream) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.setCharacterEncoding("UTF-8");
        response.flushBuffer();

        java.io.PrintWriter writer = response.getWriter();
        PulsePointStreamEmitter emitter = new PulsePointStreamEmitter() {
            @Override
            public void send(Object data) throws IOException {
                String json = objectMapper.writeValueAsString(data);
                writer.write("data: " + json + "\n\n");
                writer.flush();
            }

            @Override
            public void sendRaw(String text) throws IOException {
                writer.write("data: " + text + "\n\n");
                writer.flush();
            }

            @Override
            public void complete() {
                writer.flush();
            }

            @Override
            public void error(Throwable t) {
                log.error("Streaming error emitted", t);
                try {
                    writer.write("data: {\"error\":\"" + t.getMessage().replace("\"", "\\\"") + "\"}\n\n");
                    writer.flush();
                } catch (Exception ignored) {}
            }
        };

        try {
            stream.stream(emitter);
        } catch (Exception e) {
            log.error("Error during SSE stream execution", e);
            emitter.error(e);
        } finally {
            emitter.complete();
        }
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message, Map<String, ?> errors) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("error", message);
        if (errors != null && !errors.isEmpty()) {
            errorPayload.put("errors", errors);
        }
        objectMapper.writeValue(response.getWriter(), errorPayload);
        response.getWriter().flush();
    }

    private Map<String, Object> parseParameters(HttpServletRequest request) throws IOException {
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            return extractMultipartParams(multipartRequest);
        }

        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
            if (resolver.isMultipart(request)) {
                MultipartHttpServletRequest multipartRequest = resolver.resolveMultipart(request);
                return extractMultipartParams(multipartRequest);
            }
        }

        if (request.getContentLengthLong() != 0 && request.getInputStream() != null) {
            byte[] bodyBytes = request.getInputStream().readAllBytes();
            if (bodyBytes.length > 0) {
                return objectMapper.readValue(bodyBytes, new TypeReference<Map<String, Object>>() {});
            }
        }
        return Collections.emptyMap();
    }

    private Map<String, Object> extractMultipartParams(MultipartHttpServletRequest multipartRequest) throws IOException {
        Map<String, Object> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : multipartRequest.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                String raw = values[0];
                if (raw.startsWith("{") || raw.startsWith("[")) {
                    try {
                        params.put(entry.getKey(), objectMapper.readValue(raw, Object.class));
                        continue;
                    } catch (Exception ignored) {}
                }
                params.put(entry.getKey(), raw);
            }
        }
        for (Map.Entry<String, MultipartFile> entry : multipartRequest.getFileMap().entrySet()) {
            MultipartFile file = entry.getValue();
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("filename", file.getOriginalFilename());
            fileInfo.put("contentType", file.getContentType());
            fileInfo.put("size", file.getSize());
            fileInfo.put("bytes", file.getBytes());
            params.put(entry.getKey(), fileInfo);
        }
        return params;
    }
}
