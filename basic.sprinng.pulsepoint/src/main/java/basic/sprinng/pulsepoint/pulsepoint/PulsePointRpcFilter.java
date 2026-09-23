
package basic.sprinng.pulsepoint.pulsepoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import basic.sprinng.pulsepoint.exception.ConflictException;
import basic.sprinng.pulsepoint.exception.ResourceNotFoundException;
import basic.sprinng.pulsepoint.pulsepoint.exception.PulsePointValidationException;
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

        Map<String, Object> parameters = Collections.emptyMap();
        if (request.getContentLengthLong() != 0 && request.getInputStream() != null) {
            try {
                byte[] bodyBytes = request.getInputStream().readAllBytes();
                if (bodyBytes.length > 0) {
                    parameters = objectMapper.readValue(bodyBytes, new TypeReference<Map<String, Object>>() {});
                }
            } catch (Exception e) {
                log.error("Failed to parse RPC JSON payload for function: {}", functionName, e);
                sendError(response, HttpStatus.BAD_REQUEST, "Malformed JSON request body", null);
                return;
            }
        }

        try {
            Object result = function.invoke(parameters != null ? parameters : Collections.emptyMap());
            sendSuccess(response, result);
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
}
