package com.smartops.planner.employee;

import com.smartops.planner.skill.Skill;
import com.smartops.planner.skill.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private SkillRepository skillRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        skillRepository.deleteAll();
    }

    @Test
    void createsEmployee() throws Exception {
        Skill java = skillRepository.save(new Skill("Java"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ada Lovelace",
                                  "email": "ada@smartops.test",
                                  "maxWeeklyHours": 40,
                                  "currentWeeklyHours": 10,
                                  "seniorityLevel": "SENIOR",
                                  "skillIds": [%d]
                                }
                                """.formatted(java.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@smartops.test"))
                .andExpect(jsonPath("$.maxWeeklyHours").value(40))
                .andExpect(jsonPath("$.currentWeeklyHours").value(10))
                .andExpect(jsonPath("$.seniorityLevel").value("SENIOR"))
                .andExpect(jsonPath("$.skills[0].id").value(java.getId().intValue()))
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
        String requestBody = """
                {
                  "name": "Dorothy Vaughan",
                  "email": "dorothy@smartops.test",
                  "maxWeeklyHours": 40,
                  "currentWeeklyHours": 8,
                  "seniorityLevel": "LEAD",
                  "skillIds": []
                }
                """;

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee already exists with email dorothy@smartops.test"));
    }

    @Test
    void listsEmployees() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Barbara Liskov",
                                  "email": "barbara@smartops.test",
                                  "maxWeeklyHours": 35,
                                  "currentWeeklyHours": 7,
                                  "seniorityLevel": "LEAD",
                                  "skillIds": []
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Barbara Liskov"));
    }

    @Test
    void findsEmployeeById() throws Exception {
        Employee employee = employeeRepository.save(new Employee(
                "Evelyn Boyd Granville",
                "evelyn@smartops.test",
                40,
                SeniorityLevel.SENIOR
        ));

        mockMvc.perform(get("/api/employees/{id}", employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employee.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Evelyn Boyd Granville"))
                .andExpect(jsonPath("$.email").value("evelyn@smartops.test"));
    }

    @Test
    void returnsNotFoundWhenEmployeeDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/employees/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id 999999"));
    }

    @Test
    void updatesEmployee() throws Exception {
        Skill java = skillRepository.save(new Skill("Java"));
        Employee employee = employeeRepository.save(new Employee(
                "Annie Easley",
                "annie@smartops.test",
                30,
                SeniorityLevel.MID
        ));

        mockMvc.perform(put("/api/employees/{id}", employee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Annie J. Easley",
                                  "email": "annie.easley@smartops.test",
                                  "maxWeeklyHours": 35,
                                  "currentWeeklyHours": 14,
                                  "seniorityLevel": "SENIOR",
                                  "skillIds": [%d]
                                }
                                """.formatted(java.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(employee.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Annie J. Easley"))
                .andExpect(jsonPath("$.email").value("annie.easley@smartops.test"))
                .andExpect(jsonPath("$.maxWeeklyHours").value(35))
                .andExpect(jsonPath("$.currentWeeklyHours").value(14))
                .andExpect(jsonPath("$.seniorityLevel").value("SENIOR"))
                .andExpect(jsonPath("$.skills[0].name").value("Java"));
    }

    @Test
    void returnsConflictWhenUpdatingEmployeeWithDuplicateEmail() throws Exception {
        employeeRepository.save(new Employee(
                "Radia Perlman",
                "radia@smartops.test",
                40,
                SeniorityLevel.LEAD
        ));
        Employee employee = employeeRepository.save(new Employee(
                "Frances Allen",
                "frances@smartops.test",
                40,
                SeniorityLevel.SENIOR
        ));

        mockMvc.perform(put("/api/employees/{id}", employee.getId())
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
        Employee employee = employeeRepository.save(new Employee(
                "Karen Sparck Jones",
                "karen@smartops.test",
                40,
                SeniorityLevel.SENIOR
        ));

        mockMvc.perform(delete("/api/employees/{id}", employee.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void returnsNotFoundWhenDeletingEmployeeDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/employees/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id 999999"));
    }
}
