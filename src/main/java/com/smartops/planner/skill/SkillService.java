package com.smartops.planner.skill;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.skill.dto.CreateSkillRequest;
import com.smartops.planner.skill.dto.SkillResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> findAll() {
        return skillRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse findById(Long id) {
        return skillRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id " + id));
    }

    @Transactional
    public SkillResponse create(CreateSkillRequest request) {
        String name = request.name().trim();
        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw new BadRequestException("Skill already exists with name " + name, HttpStatus.CONFLICT);
        }

        Skill skill = skillRepository.save(new Skill(name));
        return toResponse(skill);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Skill not found with id " + id);
        }

        skillRepository.deleteById(id);
    }

    private SkillResponse toResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName());
    }
}
