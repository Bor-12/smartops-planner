package com.smartops.planner.auth;

import com.smartops.planner.auth.dto.LoginRequest;
import com.smartops.planner.auth.dto.RegisterRequest;
import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.security.JwtService;
import com.smartops.planner.user.Role;
import com.smartops.planner.user.User;
import com.smartops.planner.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUserWithEncryptedPassword() {
        RegisterRequest request = new RegisterRequest(" manager ", "password123", Role.MANAGER);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("manager");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    void register_shouldThrowConflict_whenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest(" manager ", "password123", Role.MANAGER);
        when(userRepository.existsByUsernameIgnoreCase("manager")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(BadRequestException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).isEqualTo("User already exists with username manager");
                });

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        User user = new User("admin", "encoded-password", Role.ADMIN);
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        var response = authService.login(new LoginRequest("admin", "password123"));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
        assertThat(response.token()).isEqualTo("jwt-token");
    }

    @Test
    void login_shouldThrowUnauthorized_whenUserDoesNotExistAfterAuthentication() {
        when(userRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "password123")))
                .isInstanceOfSatisfying(BadRequestException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getMessage()).isEqualTo("Invalid credentials");
                });

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }
}
