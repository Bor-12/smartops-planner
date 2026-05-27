package com.smartops.planner.planning;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByPlanningRunId(Long planningRunId);
}
