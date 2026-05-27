package com.smartops.planner.employee.dto;

import com.smartops.planner.employee.SeniorityLevel;
import com.smartops.planner.skill.dto.SkillResponse;
import java.util.List;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        Integer maxWeeklyHours,
        Integer currentWeeklyHours,
        SeniorityLevel seniorityLevel,
        List<SkillResponse> skills
) {
}
