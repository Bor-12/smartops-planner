package com.smartops.planner.employee;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.employee.dto.CreateEmployeeRequest;
import com.smartops.planner.employee.dto.EmployeeResponse;
import com.smartops.planner.employee.dto.UpdateEmployeeRequest;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.skill.SkillRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void findAll_shouldReturnAllEmployees() {
        Employee ada = employee(1L, "Ada Lovelace", "ada@smartops.test");
        Employee grace = employee(2L, "Grace Hopper", "grace@smartops.test");

        when(employeeRepository.findAll()).thenReturn(List.of(ada, grace));

        List<EmployeeResponse> responses = employeeService.findAll();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals("Ada Lovelace", responses.get(0).name());
        assertEquals(2L, responses.get(1).id());
        assertEquals("Grace Hopper", responses.get(1).name());

        verify(employeeRepository).findAll();
    }

    @Test
    void findById_shouldReturnEmployee_whenEmployeeExists() {
        Skill java = skill(10L, "Java");
        Employee employee = employee(1L, "Ada Lovelace", "ada@smartops.test");
        employee.setSkills(Set.of(java));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("Ada Lovelace", response.name());
        assertEquals(1, response.skills().size());
        assertEquals(10L, response.skills().get(0).id());
        assertEquals("Java", response.skills().get(0).name());

        verify(employeeRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowResourceNotFoundException_whenEmployeeDoesNotExist() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.findById(99L)
        );

        assertEquals("Employee not found with id 99", exception.getMessage());
        verify(employeeRepository).findById(99L);
    }

    @Test
    void create_shouldCreateEmployee_whenDataIsValid() {
        Skill java = skill(10L, "Java");
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                " Ada Lovelace ",
                " ada@smartops.test ",
                40,
                10,
                SeniorityLevel.SENIOR,
                Set.of(10L)
        );

        when(employeeRepository.existsByEmailIgnoreCase("ada@smartops.test")).thenReturn(false);
        when(skillRepository.findAllById(Set.of(10L))).thenReturn(List.of(java));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee savedEmployee = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedEmployee, "id", 1L);
            return savedEmployee;
        });

        EmployeeResponse response = employeeService.create(request);

        assertEquals(1L, response.id());
        assertEquals("Ada Lovelace", response.name());
        assertEquals("ada@smartops.test", response.email());
        assertEquals(40, response.maxWeeklyHours());
        assertEquals(10, response.currentWeeklyHours());
        assertEquals(SeniorityLevel.SENIOR, response.seniorityLevel());
        assertEquals(1, response.skills().size());
        assertEquals("Java", response.skills().get(0).name());

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee savedEmployee = employeeCaptor.getValue();
        assertEquals("Ada Lovelace", savedEmployee.getName());
        assertEquals("ada@smartops.test", savedEmployee.getEmail());
        assertEquals(1, savedEmployee.getSkills().size());
    }

    @Test
    void create_shouldThrowConflict_whenEmailAlreadyExists() {
        CreateEmployeeRequest request = validCreateRequest(Set.of());

        when(employeeRepository.existsByEmailIgnoreCase("ada@smartops.test")).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> employeeService.create(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Employee already exists with email ada@smartops.test", exception.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void create_shouldThrowBadRequest_whenCurrentWeeklyHoursGreaterThanMaxWeeklyHours() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Ada Lovelace",
                "ada@smartops.test",
                20,
                25,
                SeniorityLevel.SENIOR,
                Set.of()
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> employeeService.create(request)
        );

        assertEquals("currentWeeklyHours cannot be greater than maxWeeklyHours", exception.getMessage());
        verify(employeeRepository, never()).existsByEmailIgnoreCase(any());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void create_shouldThrowBadRequest_whenSkillDoesNotExist() {
        CreateEmployeeRequest request = validCreateRequest(Set.of(10L, 99L));

        when(employeeRepository.existsByEmailIgnoreCase("ada@smartops.test")).thenReturn(false);
        when(skillRepository.findAllById(Set.of(10L, 99L))).thenReturn(List.of(skill(10L, "Java")));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> employeeService.create(request)
        );

        assertEquals("Skill not found with id 99", exception.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void update_shouldUpdateEmployee_whenDataIsValid() {
        Employee employee = employee(1L, "Ada Lovelace", "ada@smartops.test");
        Skill spring = skill(20L, "Spring Boot");
        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                " Ada Byron ",
                " ada.byron@smartops.test ",
                35,
                12,
                SeniorityLevel.LEAD,
                Set.of(20L)
        );

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("ada.byron@smartops.test", 1L)).thenReturn(false);
        when(skillRepository.findAllById(Set.of(20L))).thenReturn(List.of(spring));

        EmployeeResponse response = employeeService.update(1L, request);

        assertEquals(1L, response.id());
        assertEquals("Ada Byron", response.name());
        assertEquals("ada.byron@smartops.test", response.email());
        assertEquals(35, response.maxWeeklyHours());
        assertEquals(12, response.currentWeeklyHours());
        assertEquals(SeniorityLevel.LEAD, response.seniorityLevel());
        assertEquals(1, response.skills().size());
        assertEquals("Spring Boot", response.skills().get(0).name());

        assertEquals("Ada Byron", employee.getName());
        assertEquals("ada.byron@smartops.test", employee.getEmail());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenEmployeeDoesNotExist() {
        UpdateEmployeeRequest request = validUpdateRequest(Set.of());

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.update(99L, request)
        );

        assertEquals("Employee not found with id 99", exception.getMessage());
        verify(employeeRepository, never()).existsByEmailIgnoreCaseAndIdNot(any(), any());
    }

    @Test
    void update_shouldThrowConflict_whenEmailBelongsToAnotherEmployee() {
        Employee employee = employee(1L, "Ada Lovelace", "ada@smartops.test");
        UpdateEmployeeRequest request = validUpdateRequest(Set.of());

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("ada@smartops.test", 1L)).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> employeeService.update(1L, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Employee already exists with email ada@smartops.test", exception.getMessage());
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void deleteById_shouldDeleteEmployee_whenEmployeeExists() {
        when(employeeRepository.existsById(1L)).thenReturn(true);

        employeeService.deleteById(1L);

        verify(employeeRepository).existsById(1L);
        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void deleteById_shouldThrowResourceNotFoundException_whenEmployeeDoesNotExist() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> employeeService.deleteById(99L)
        );

        assertEquals("Employee not found with id 99", exception.getMessage());
        verify(employeeRepository).existsById(99L);
        verify(employeeRepository, never()).deleteById(any());
    }

    private CreateEmployeeRequest validCreateRequest(Set<Long> skillIds) {
        return new CreateEmployeeRequest(
                "Ada Lovelace",
                "ada@smartops.test",
                40,
                10,
                SeniorityLevel.SENIOR,
                skillIds
        );
    }

    private UpdateEmployeeRequest validUpdateRequest(Set<Long> skillIds) {
        return new UpdateEmployeeRequest(
                "Ada Lovelace",
                "ada@smartops.test",
                40,
                10,
                SeniorityLevel.SENIOR,
                skillIds
        );
    }

    private Employee employee(Long id, String name, String email) {
        Employee employee = new Employee(name, email, 40, SeniorityLevel.SENIOR);
        employee.setCurrentWeeklyHours(10);
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private Skill skill(Long id, String name) {
        Skill skill = new Skill(name);
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }
}
