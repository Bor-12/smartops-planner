package com.smartops.planner.skill;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.skill.dto.CreateSkillRequest;
import com.smartops.planner.skill.dto.SkillResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService skillService;

    @Test
    void findAll_shouldReturnAllSkills() {
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring Boot");

        when(skillRepository.findAll()).thenReturn(List.of(java, spring));

        List<SkillResponse> responses = skillService.findAll();

        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).id());
        assertEquals("Java", responses.get(0).name());
        assertEquals(2L, responses.get(1).id());
        assertEquals("Spring Boot", responses.get(1).name());

        verify(skillRepository).findAll();
    }

    @Test
    void findById_shouldReturnSkill_whenSkillExists() {
        Skill skill = skill(1L, "Java");

        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill));

        SkillResponse response = skillService.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("Java", response.name());

        verify(skillRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowResourceNotFoundException_whenSkillDoesNotExist() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> skillService.findById(99L)
        );

        assertEquals("Skill not found with id 99", exception.getMessage());

        verify(skillRepository).findById(99L);
    }

    @Test
    void create_shouldCreateSkill_whenNameDoesNotExist() {
        CreateSkillRequest request = new CreateSkillRequest(" Java ");

        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(false);
        when(skillRepository.save(any(Skill.class)))
                .thenAnswer(invocation -> {
                    Skill savedSkill = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedSkill, "id", 1L);
                    return savedSkill;
                });

        SkillResponse response = skillService.create(request);

        assertEquals(1L, response.id());
        assertEquals("Java", response.name());

        ArgumentCaptor<Skill> skillCaptor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(skillCaptor.capture());

        Skill savedSkill = skillCaptor.getValue();
        assertEquals("Java", savedSkill.getName());

        verify(skillRepository).existsByNameIgnoreCase("Java");
    }

    @Test
    void create_shouldThrowBadRequestException_whenSkillAlreadyExists() {
        CreateSkillRequest request = new CreateSkillRequest("Java");

        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> skillService.create(request)
        );

        assertEquals("Skill already exists with name Java", exception.getMessage());

        verify(skillRepository).existsByNameIgnoreCase("Java");
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void deleteById_shouldDeleteSkill_whenSkillExists() {
        when(skillRepository.existsById(1L)).thenReturn(true);

        skillService.deleteById(1L);

        verify(skillRepository).existsById(1L);
        verify(skillRepository).deleteById(1L);
    }

    @Test
    void deleteById_shouldThrowResourceNotFoundException_whenSkillDoesNotExist() {
        when(skillRepository.existsById(99L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> skillService.deleteById(99L)
        );

        assertEquals("Skill not found with id 99", exception.getMessage());

        verify(skillRepository).existsById(99L);
        verify(skillRepository, never()).deleteById(any());
    }

    private Skill skill(Long id, String name) {
        Skill skill = new Skill(name);
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }
}