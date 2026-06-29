package com.smartops.planner.config;

import com.smartops.planner.employee.EmployeeProfileProvisioner;
import com.smartops.planner.user.Role;
import com.smartops.planner.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EmployeeUserProfileLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeProfileProvisioner employeeProfileProvisioner;

    public EmployeeUserProfileLoader(
            UserRepository userRepository,
            EmployeeProfileProvisioner employeeProfileProvisioner
    ) {
        this.userRepository = userRepository;
        this.employeeProfileProvisioner = employeeProfileProvisioner;
    }

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == Role.EMPLOYEE)
                .forEach(employeeProfileProvisioner::createProfileForEmployeeUserIfMissing);
    }
}
