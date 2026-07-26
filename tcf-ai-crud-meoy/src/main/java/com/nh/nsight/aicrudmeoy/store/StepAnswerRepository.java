package com.nh.nsight.aicrudmeoy.store;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepAnswerRepository extends JpaRepository<StepAnswerEntity, Long> {
    List<StepAnswerEntity> findBySessionIdOrderByStepIdAscQuestionIdAsc(String sessionId);
    List<StepAnswerEntity> findBySessionIdAndStepId(String sessionId, String stepId);
    Optional<StepAnswerEntity> findBySessionIdAndStepIdAndQuestionId(String sessionId, String stepId, String questionId);
}
