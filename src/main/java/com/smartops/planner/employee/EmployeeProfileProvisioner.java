package com.smartops.planner.employee;

import com.smartops.planner.user.Role;
import com.smartops.planner.user.User;
import org.springframework.stereotype.Service;

@Service
public class EmployeeProfileProvisioner {

    private static final String DEMO_EMAIL_DOMAIN = "@smartops.demo";
    private static final int DEFAULT_MAX_WEEKLY_HOURS = 40;

    private final EmployeeRepository employeeRepository;

    public EmployeeProfileProvisioner(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public void createProfileForEmployeeUserIfMissing(User user) {
        if (user.getRole() != Role.EMPLOYEE) {
            return;
        }

        String username = user.getUsername().trim();
        String email = username + DEMO_EMAIL_DOMAIN;
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Employee employee = new Employee(
                username,
                email,
                DEFAULT_MAX_WEEKLY_HOURS,
                SeniorityLevel.JUNIOR
        );
        employee.setCurrentWeeklyHours(0);
        employeeRepository.save(employee);
    }
}
