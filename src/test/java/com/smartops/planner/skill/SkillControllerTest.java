package com.smartops.planner.skill;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.GlobalExceptionHandler;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.skill.dto.CreateSkillRequest;
import com.smartops.planner.skill.dto.SkillResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SkillController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class SkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillService skillService;

    @Test
    void findAll_shouldReturnSkills() throws Exception {
        when(skillService.findAll()).thenReturn(List.of(
                new SkillResponse(1L, "Java"),
                new SkillResponse(2L, "Spring Boot")
        ));

        mockMvc.perform(get("/api/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Java"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Spring Boot"));

        verify(skillService).findAll();
    }

    @Test
    void findById_shouldReturnSkill_whenSkillExists() throws Exception {
        when(skillService.findById(1L)).thenReturn(new SkillResponse(1L, "Java"));

        mockMvc.perform(get("/api/skills/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java"));

        verify(skillService).findById(1L);
    }

    @Test
    void findById_shouldReturnNotFound_whenSkillDoesNotExist() throws Exception {
        when(skillService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Skill not found with id 99"));

        mockMvc.perform(get("/api/skills/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Skill not found with id 99"))
                .andExpect(jsonPath("$.path").value("/api/skills/99"));
    }

    @Test
    void create_shouldCreateSkill() throws Exception {
        when(skillService.create(any(CreateSkillRequest.class)))
                .thenReturn(new SkillResponse(1L, "Java"));

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Java"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/skills/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Java"));
    }

    @Test
    void create_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/skills"));
    }

    @Test
    void create_shouldReturnConflict_whenSkillAlreadyExists() throws Exception {
        when(skillService.create(any(CreateSkillRequest.class)))
                .thenThrow(new BadRequestException("Skill already exists with name Java", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Java"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Skill already exists with name Java"));
    }

    @Test
    void deleteById_shouldReturnNoContent_whenSkillExists() throws Exception {
        mockMvc.perform(delete("/api/skills/1"))
                .andExpect(status().isNoContent());

        verify(skillService).deleteById(1L);
    }

    @Test
    void deleteById_shouldReturnNotFound_whenSkillDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Skill not found with id 99"))
                .when(skillService)
                .deleteById(99L);

        mockMvc.perform(delete("/api/skills/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Skill not found with id 99"));
    }
}
