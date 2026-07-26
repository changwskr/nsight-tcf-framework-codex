package com.nh.nsight.aicrudmeoy.store;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepResultRepository extends JpaRepository<StepResultEntity, Long> {
    List<StepResultEntity> findBySessionId(String sessionId);
    Optional<StepResultEntity> findBySessionIdAndStepId(String sessionId, String stepId);
}
