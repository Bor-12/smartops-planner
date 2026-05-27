package com.smartops.planner.planning;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanningRunRepository extends JpaRepository<PlanningRun, Long> {

    Optional<PlanningRun> findTopByOrderByStartedAtDesc();
}
