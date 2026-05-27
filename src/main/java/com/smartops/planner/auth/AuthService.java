package com.smartops.planner.auth;

import com.smartops.planner.auth.dto.AuthResponse;
import com.smartops.planner.auth.dto.LoginRequest;
import com.smartops.planner.auth.dto.RegisterRequest;
import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.security.JwtService;
import com.smartops.planner.user.User;
import com.smartops.planner.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BadRequestException("User already exists with username " + username, HttpStatus.CONFLICT);
        }

        User user = userRepository.save(new User(
                username,
                passwordEncoder.encode(request.password()),
                request.role()
        ));

        return toResponse(user, jwtService.generateToken(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.username(),
                request.password()
        ));

        User user = userRepository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new BadRequestException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        return toResponse(user, jwtService.generateToken(user));
    }

    private AuthResponse toResponse(User user, String token) {
        return new AuthResponse(user.getId(), user.getUsername(), user.getRole(), token);
    }
}
