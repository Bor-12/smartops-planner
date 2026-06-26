package com.smartops.planner.config;

import com.smartops.planner.user.Role;
import com.smartops.planner.user.User;
import com.smartops.planner.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class InitialAdminLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminLoader.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public InitialAdminLoader(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.initial-admin.username:}") String username,
            @Value("${app.initial-admin.password:}") String password
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(username) && !StringUtils.hasText(password)) {
            return;
        }

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("Initial admin was not created because username or password is missing");
            return;
        }

        String normalizedUsername = username.trim();
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            log.info("Initial admin '{}' already exists", normalizedUsername);
            return;
        }

        userRepository.save(new User(
                normalizedUsername,
                passwordEncoder.encode(password),
                Role.ADMIN
        ));
        log.info("Initial admin '{}' created", normalizedUsername);
    }
}
