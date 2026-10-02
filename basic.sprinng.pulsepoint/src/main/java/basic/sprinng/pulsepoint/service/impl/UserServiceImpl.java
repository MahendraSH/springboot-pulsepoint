package basic.sprinng.pulsepoint.service.impl;

import basic.sprinng.pulsepoint.dto.CreateUserRequest;
import basic.sprinng.pulsepoint.dto.UserResponse;
import basic.sprinng.pulsepoint.exception.ConflictException;
import basic.sprinng.pulsepoint.exception.ResourceNotFoundException;
import basic.sprinng.pulsepoint.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserServiceImpl implements UserService {

    private final Map<Long, UserResponse> users = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public UserServiceImpl() {
        // Pre-populate initial users
        createUser(new CreateUserRequest("demo", "demo@example.com", "ROLE_USER"));
        createUser(new CreateUserRequest("admin", "admin@example.com", "ROLE_ADMIN"));
    }

    @Override
    public List<UserResponse> listUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public UserResponse getUser(Long id) {
        UserResponse user = users.get(id);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        return user;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        boolean usernameExists = users.values().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(request.getUsername()));
        if (usernameExists) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }

        Long id = idCounter.getAndIncrement();
        UserResponse user = UserResponse.builder()
                .id(id)
                .username(request.getUsername())
                .email(request.getEmail())
                .role(request.getRole())
                .createdAt(LocalDateTime.now())
                .build();

        users.put(id, user);
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        if (!users.containsKey(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        users.remove(id);
    }
}
