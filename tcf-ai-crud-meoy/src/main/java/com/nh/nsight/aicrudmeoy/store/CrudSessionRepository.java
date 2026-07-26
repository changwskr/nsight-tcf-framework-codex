package com.nh.nsight.aicrudmeoy.store;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrudSessionRepository extends JpaRepository<CrudSessionEntity, String> {
    List<CrudSessionEntity> findAllByOrderByUpdatedAtDesc();

    boolean existsBySampleFlagTrue();
}
