package com.smartops.planner.employee;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.GlobalExceptionHandler;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.employee.dto.CreateEmployeeRequest;
import com.smartops.planner.employee.dto.EmployeeResponse;
import com.smartops.planner.employee.dto.UpdateEmployeeRequest;
import com.smartops.planner.skill.dto.SkillResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void createsEmployee() throws Exception {
        when(employeeService.create(any(CreateEmployeeRequest.class)))
                .thenReturn(employeeResponse(1L, "Ada Lovelace", "ada@smartops.test", List.of(new SkillResponse(1L, "Java"))));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ada Lovelace",
                                  "email": "ada@smartops.test",
                                  "maxWeeklyHours": 40,
                                  "currentWeeklyHours": 10,
                                  "seniorityLevel": "SENIOR",
                                  "skillIds": [1]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@smartops.test"))
                .andExpect(jsonPath("$.maxWeeklyHours").value(40))
                .andExpect(jsonPath("$.currentWeeklyHours").value(10))
                .andExpect(jsonPath("$.seniorityLevel").value("SENIOR"))
                .andExpect(jsonPath("$.skills[0].id").value(1))
                .andExpect(jsonPath("$.skills[0].name").value("Java"));
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Grace Hopper",
                                  "email": "not-an-email",
                                  "maxWeeklyHours": 40,
                                  "currentWeeklyHours": 5,
                                  "seniorityLevel": "LEAD",
                                  "skillIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsInvalidMaxWeeklyHours() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Margaret Hamilton",
                                  "email": "margaret@smartops.test",
                                  "maxWeeklyHours": 0,
                                  "currentWeeklyHours": 0,
                                  "seniorityLevel": "SENIOR",
                                  "skillIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsCurrentWeeklyHoursGreaterThanMaxWeeklyHours() throws Exception {
        when(employeeService.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new BadRequestException("currentWeeklyHours cannot be greater than maxWeeklyHours"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mary Jackson",
                                  "email": "mary@smartops.test",
                                  "maxWeeklyHours": 20,
                                  "currentWeeklyHours": 25,
                                  "seniorityLevel": "MID",
                                  "skillIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("currentWeeklyHours cannot be greater than maxWeeklyHours"));
    }

    @Test
    void rejectsUnknownSkill() throws Exception {
        when(employeeService.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new BadRequestException("Skill not found with id 999999"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Katherine Johnson",
                                  "email": "katherine@smartops.test",
                                  "maxWeeklyHours": 40,
                                  "currentWeeklyHours": 12,
                                  "seniorityLevel": "MID",
                                  "skillIds": [999999]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Skill not found with id 999999"));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        when(employeeService.create(any(CreateEmployeeRequest.class)))
                .thenThrow(new BadRequestException("Employee already exists with email dorothy@smartops.test", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Dorothy Vaughan",
                                  "email": "dorothy@smartops.test",
                                  "maxWeeklyHours": 40,
                                  "currentWeeklyHours": 8,
                                  "seniorityLevel": "LEAD",
                                  "skillIds": []
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee already exists with email dorothy@smartops.test"));
    }

    @Test
    void listsEmployees() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of(
                employeeResponse(1L, "Barbara Liskov", "barbara@smartops.test", List.of())
        ));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Barbara Liskov"));
    }

    @Test
    void findsEmployeeById() throws Exception {
        when(employeeService.findById(1L))
                .thenReturn(employeeResponse(1L, "Evelyn Boyd Granville", "evelyn@smartops.test", List.of()));

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Evelyn Boyd Granville"))
                .andExpect(jsonPath("$.email").value("evelyn@smartops.test"));
    }

    @Test
    void returnsNotFoundWhenEmployeeDoesNotExist() throws Exception {
        when(employeeService.findById(999999L))
                .thenThrow(new ResourceNotFoundException("Employee not found with id 999999"));

        mockMvc.perform(get("/api/employees/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id 999999"));
    }

    @Test
    void updatesEmployee() throws Exception {
        when(employeeService.update(any(Long.class), any(UpdateEmployeeRequest.class)))
                .thenReturn(employeeResponse(
                        1L,
                        "Annie J. Easley",
                        "annie.easley@smartops.test",
                        35,
                        14,
                        SeniorityLevel.SENIOR,
                        List.of(new SkillResponse(1L, "Java"))
                ));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Annie J. Easley",
                                  "email": "annie.easley@smartops.test",
                                  "maxWeeklyHours": 35,
                                  "currentWeeklyHours": 14,
                                  "seniorityLevel": "SENIOR",
                                  "skillIds": [1]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Annie J. Easley"))
                .andExpect(jsonPath("$.email").value("annie.easley@smartops.test"))
                .andExpect(jsonPath("$.maxWeeklyHours").value(35))
                .andExpect(jsonPath("$.currentWeeklyHours").value(14))
                .andExpect(jsonPath("$.seniorityLevel").value("SENIOR"))
                .andExpect(jsonPath("$.skills[0].name").value("Java"));
    }

    @Test
    void returnsConflictWhenUpdatingEmployeeWithDuplicateEmail() throws Exception {
        when(employeeService.update(any(Long.class), any(UpdateEmployeeRequest.class)))
                .thenThrow(new BadRequestException("Employee already exists with email radia@smartops.test", HttpStatus.CONFLICT));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Frances Allen",
                                  "email": "radia@smartops.test",
                                  "maxWeeklyHours": 40,
                                  "currentWeeklyHours": 10,
                                  "seniorityLevel": "SENIOR",
                                  "skillIds": []
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Employee already exists with email radia@smartops.test"));
    }

    @Test
    void deletesEmployee() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteById(1L);
    }

    @Test
    void returnsNotFoundWhenDeletingEmployeeDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Employee not found with id 999999"))
                .when(employeeService)
                .deleteById(999999L);

        mockMvc.perform(delete("/api/employees/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id 999999"));
    }

    private EmployeeResponse employeeResponse(Long id, String name, String email, List<SkillResponse> skills) {
        return employeeResponse(id, name, email, 40, 10, SeniorityLevel.SENIOR, skills);
    }

    private EmployeeResponse employeeResponse(
            Long id,
            String name,
            String email,
            Integer maxWeeklyHours,
            Integer currentWeeklyHours,
            SeniorityLevel seniorityLevel,
            List<SkillResponse> skills
    ) {
        return new EmployeeResponse(
                id,
                name,
                email,
                maxWeeklyHours,
                currentWeeklyHours,
                seniorityLevel,
                skills
        );
    }
}
