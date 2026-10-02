package basic.sprinng.pulsepoint.service;

import basic.sprinng.pulsepoint.dto.CreateUserRequest;
import basic.sprinng.pulsepoint.dto.UserResponse;

import java.util.List;

public interface UserService {
    List<UserResponse> listUsers();
    UserResponse getUser(Long id);
    UserResponse createUser(CreateUserRequest request);
    void deleteUser(Long id);
}
