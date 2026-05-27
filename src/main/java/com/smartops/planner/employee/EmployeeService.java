package com.smartops.planner.employee;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.employee.dto.CreateEmployeeRequest;
import com.smartops.planner.employee.dto.EmployeeResponse;
import com.smartops.planner.employee.dto.UpdateEmployeeRequest;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.skill.SkillRepository;
import com.smartops.planner.skill.dto.SkillResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SkillRepository skillRepository;

    public EmployeeService(EmployeeRepository employeeRepository, SkillRepository skillRepository) {
        this.employeeRepository = employeeRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return employeeRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        validateWeeklyHours(request.currentWeeklyHours(), request.maxWeeklyHours());

        String email = request.email().trim();
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Employee already exists with email " + email, HttpStatus.CONFLICT);
        }

        Employee employee = new Employee(
                request.name().trim(),
                email,
                request.maxWeeklyHours(),
                request.seniorityLevel()
        );
        employee.setCurrentWeeklyHours(request.currentWeeklyHours());
        employee.setSkills(loadSkills(request.skillIds()));

        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        validateWeeklyHours(request.currentWeeklyHours(), request.maxWeeklyHours());

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));

        String email = request.email().trim();
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BadRequestException("Employee already exists with email " + email, HttpStatus.CONFLICT);
        }

        employee.setName(request.name().trim());
        employee.setEmail(email);
        employee.setMaxWeeklyHours(request.maxWeeklyHours());
        employee.setCurrentWeeklyHours(request.currentWeeklyHours());
        employee.setSeniorityLevel(request.seniorityLevel());
        employee.setSkills(loadSkills(request.skillIds()));

        return toResponse(employee);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id " + id);
        }

        employeeRepository.deleteById(id);
    }

    private void validateWeeklyHours(Integer currentWeeklyHours, Integer maxWeeklyHours) {
        if (currentWeeklyHours > maxWeeklyHours) {
            throw new BadRequestException("currentWeeklyHours cannot be greater than maxWeeklyHours");
        }
    }

    private Set<Skill> loadSkills(Set<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> uniqueSkillIds = new HashSet<>(skillIds);
        List<Skill> skills = skillRepository.findAllById(uniqueSkillIds);
        if (skills.size() != uniqueSkillIds.size()) {
            Set<Long> foundIds = skills.stream()
                    .map(Skill::getId)
                    .collect(java.util.stream.Collectors.toSet());

            Long missingId = uniqueSkillIds.stream()
                    .filter(skillId -> !foundIds.contains(skillId))
                    .findFirst()
                    .orElseThrow();

            throw new BadRequestException("Skill not found with id " + missingId);
        }

        return new HashSet<>(skills);
    }

    private EmployeeResponse toResponse(Employee employee) {
        List<SkillResponse> skills = employee.getSkills()
                .stream()
                .map(skill -> new SkillResponse(skill.getId(), skill.getName()))
                .sorted(java.util.Comparator.comparing(SkillResponse::id))
                .toList();

        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmail(),
                employee.getMaxWeeklyHours(),
                employee.getCurrentWeeklyHours(),
                employee.getSeniorityLevel(),
                skills
        );
    }
}
