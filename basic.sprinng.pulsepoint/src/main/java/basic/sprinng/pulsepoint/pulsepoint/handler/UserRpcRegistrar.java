package basic.sprinng.pulsepoint.pulsepoint.handler;

import basic.sprinng.pulsepoint.dto.CreateUserRequest;
import basic.sprinng.pulsepoint.pulsepoint.PulsePointRpcRegistry;
import basic.sprinng.pulsepoint.pulsepoint.exception.PulsePointValidationException;
import basic.sprinng.pulsepoint.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class UserRpcRegistrar {

    private final UserService userService;
    private final PulsePointRpcRegistry registry;
    private final Validator validator;

    public UserRpcRegistrar(UserService userService,
                            PulsePointRpcRegistry registry,
                            Validator validator) {
        this.userService = userService;
        this.registry = registry;
        this.validator = validator;
    }

    @PostConstruct
    public void registerFunctions() {
        registry.register("listUsers", this::listUsers);
        registry.register("createUser", this::createUser);
        registry.register("deleteUser", this::deleteUser);
    }

    private Object listUsers(Map<String, Object> params) {
        checkAdminAccess();
        return userService.listUsers();
    }

    private Object createUser(Map<String, Object> params) {
        checkAdminAccess();
        String username = (String) params.get("username");
        String email = (String) params.get("email");
        String role = (String) params.get("role");

        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .email(email)
                .role(role)
                .build();

        validateRequest(request);
        return userService.createUser(request);
    }

    private Object deleteUser(Map<String, Object> params) {
        checkAdminAccess();
        Long id = extractLong(params, "id");
        userService.deleteUser(id);
        return Map.of("success", true, "id", id);
    }

    private void checkAdminAccess() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new org.springframework.security.access.AccessDeniedException("Permission denied");
        }
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
