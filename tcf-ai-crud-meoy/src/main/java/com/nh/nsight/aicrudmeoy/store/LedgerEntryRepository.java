package com.nh.nsight.aicrudmeoy.store;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {
    List<LedgerEntryEntity> findBySessionIdOrderByEntryKeyAsc(String sessionId);
    Optional<LedgerEntryEntity> findBySessionIdAndEntryKey(String sessionId, String entryKey);
}
