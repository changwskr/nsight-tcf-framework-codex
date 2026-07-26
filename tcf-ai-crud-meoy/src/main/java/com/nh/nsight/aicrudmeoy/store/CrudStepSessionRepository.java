package com.nh.nsight.aicrudmeoy.store;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrudStepSessionRepository extends JpaRepository<CrudStepSessionEntity, Long> {

    Optional<CrudStepSessionEntity> findBySessionIdAndStepId(String sessionId, String stepId);

    List<CrudStepSessionEntity> findBySessionIdOrderByStepOrderAscStepIdAsc(String sessionId);

    void deleteBySessionId(String sessionId);

    @Query("""
            select s from CrudStepSessionEntity s
            where (:sessionId is null or s.sessionId = :sessionId)
              and (:stepId is null or s.stepId = :stepId)
              and (:status is null or s.status = :status)
              and (:businessCode is null or lower(s.businessCode) = lower(:businessCode))
              and (:q is null
                   or lower(coalesce(s.sessionName, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(s.stepId) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(s.stepTitle, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(s.domainCode, '')) like lower(concat('%', cast(:q as string), '%'))
                   or lower(coalesce(s.businessCode, '')) like lower(concat('%', cast(:q as string), '%')))
            order by s.updatedAt desc
            """)
    List<CrudStepSessionEntity> search(
            @Param("q") String q,
            @Param("sessionId") String sessionId,
            @Param("stepId") String stepId,
            @Param("status") String status,
            @Param("businessCode") String businessCode);
}
