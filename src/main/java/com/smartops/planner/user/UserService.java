package com.smartops.planner.user;

import com.smartops.planner.auth.dto.RegisterRequest;
import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.employee.EmployeeProfileProvisioner;
import com.smartops.planner.user.dto.UserResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeProfileProvisioner employeeProfileProvisioner;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmployeeProfileProvisioner employeeProfileProvisioner
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeProfileProvisioner = employeeProfileProvisioner;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse create(RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("User already exists with username " + username, HttpStatus.CONFLICT);
        }

        User user = userRepository.save(new User(
                username,
                passwordEncoder.encode(request.password()),
                request.role()
        ));
        employeeProfileProvisioner.createProfileForEmployeeUserIfMissing(user);

        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }
}
