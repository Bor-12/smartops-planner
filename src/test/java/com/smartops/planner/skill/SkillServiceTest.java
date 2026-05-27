package com.smartops.planner.skill;

import com.smartops.planner.skill.dto.CreateSkillRequest;
import com.smartops.planner.skill.dto.SkillResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

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
    void create_shouldCreateSkill_whenNameDoesNotExist() {
        CreateSkillRequest request = new CreateSkillRequest(" Java ");

        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(false);
        when(skillRepository.save(any(Skill.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SkillResponse response = skillService.create(request);

        assertEquals("Java", response.name());
        verify(skillRepository).existsByNameIgnoreCase("Java");
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    void create_shouldThrowConflict_whenSkillAlreadyExists() {
        CreateSkillRequest request = new CreateSkillRequest("Java");

        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> skillService.create(request)
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(skillRepository).existsByNameIgnoreCase("Java");
        verify(skillRepository, never()).save(any(Skill.class));
    }

    @Test
    void findById_shouldThrowNotFound_whenSkillDoesNotExist() {
        when(skillRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> skillService.findById(99L)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(skillRepository).findById(99L);
    }

    @Test
    void deleteById_shouldDeleteSkill_whenSkillExists() {
        when(skillRepository.existsById(1L)).thenReturn(true);

        skillService.deleteById(1L);

        verify(skillRepository).existsById(1L);
        verify(skillRepository).deleteById(1L);
    }

    @Test
    void deleteById_shouldThrowNotFound_whenSkillDoesNotExist() {
        when(skillRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> skillService.deleteById(99L)
        );

        assertEquals(404, exception.getStatusCode().value());
        verify(skillRepository).existsById(99L);
        verify(skillRepository, never()).deleteById(any());
    }
}