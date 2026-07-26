package com.nh.nsight.tcf.core.support.runtime;

import com.nh.nsight.tcf.core.support.runtime.model.ActiveTransactionInfo;
import com.nh.nsight.tcf.core.support.runtime.model.RuntimeTransactionStep;
import com.nh.nsight.tcf.core.support.runtime.model.TransactionStepEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ActiveTransactionRegistry {
    private final Map<String, ActiveTransactionInfo> activeByGuid = new ConcurrentHashMap<>();
    private final Map<String, List<TransactionStepEvent>> stepHistoryByGuid = new ConcurrentHashMap<>();

    public void register(ActiveTransactionInfo info) {
        if (info == null || info.guid() == null || info.guid().isBlank()) {
            return;
        }
        activeByGuid.put(info.guid(), info);
        stepHistoryByGuid.put(info.guid(), new ArrayList<>());
        recordEvent(info.guid(), "REQUEST_ENTRY", "요청 진입", info.startTimeMillis(), false);
        if (info.currentStep() != null) {
            recordEvent(info.guid(), info.currentStep().name(),
                    TransactionStepHistorySupport.labelFor(info.currentStep()),
                    info.startTimeMillis(), false);
        }
    }

    public void remove(String guid) {
        if (guid == null || guid.isBlank()) {
            return;
        }
        activeByGuid.remove(guid);
        stepHistoryByGuid.remove(guid);
    }

    public void updateStep(String guid, RuntimeTransactionStep step) {
        if (guid == null || step == null) {
            return;
        }
        long now = System.currentTimeMillis();
        sealPreviousDuration(guid, now);
        recordEvent(guid, step.name(), TransactionStepHistorySupport.labelFor(step), now, false);
        activeByGuid.computeIfPresent(guid, (key, current) -> current.withStep(step));
    }

    public void updateSqlId(String guid, String sqlId) {
        if (guid == null || sqlId == null || sqlId.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        ActiveTransactionInfo current = activeByGuid.get(guid);
        if (current != null && current.currentStep() == RuntimeTransactionStep.WAIT_DB_CONNECTION
                && current.dbWaitStartMillis() > 0) {
            long waitMs = Math.max(0L, now - current.dbWaitStartMillis());
            sealPreviousDuration(guid, now);
            recordEvent(guid, "DB_CONNECTION_ACQUIRED", "DB Connection 획득", now, waitMs >= 1_000L);
            List<TransactionStepEvent> history = stepHistoryByGuid.get(guid);
            if (history != null && !history.isEmpty()) {
                TransactionStepEvent last = history.get(history.size() - 1);
                history.set(history.size() - 1, last.withDuration(waitMs).withHighlight(waitMs >= 1_000L));
            }
        }
        sealPreviousDuration(guid, now);
        recordEvent(guid, "SQL_EXECUTION_START", "SQL 실행 시작", now, false);
        activeByGuid.computeIfPresent(guid, (key, info) -> info.withSqlId(sqlId)
                .withStep(RuntimeTransactionStep.EXECUTING_SQL));
    }

    public void markDbWait(String guid) {
        if (guid == null) {
            return;
        }
        long now = System.currentTimeMillis();
        sealPreviousDuration(guid, now);
        recordEvent(guid, "DB_CONNECTION_REQUEST", "DB Connection 요청", now, false);
        activeByGuid.computeIfPresent(guid, (key, current) -> current.withDbWaitStart(now));
    }

    public void markExternalWait(String guid, String externalSystemCode) {
        if (guid == null) {
            return;
        }
        long now = System.currentTimeMillis();
        sealPreviousDuration(guid, now);
        recordEvent(guid, RuntimeTransactionStep.WAIT_EXTERNAL.name(),
                TransactionStepHistorySupport.labelFor(RuntimeTransactionStep.WAIT_EXTERNAL), now, false);
        activeByGuid.computeIfPresent(guid, (key, current) -> current.withExternalSystem(externalSystemCode));
    }

    public List<TransactionStepEvent> getStepHistory(String guid) {
        if (guid == null) {
            return List.of();
        }
        List<TransactionStepEvent> source = stepHistoryByGuid.get(guid);
        if (source == null) {
            return List.of();
        }
        return TransactionStepHistorySupport.exportEvents(new ArrayList<>(source), System.currentTimeMillis());
    }

    public ActiveTransactionInfo findByGuid(String guid) {
        if (guid == null || guid.isBlank()) {
            return null;
        }
        ActiveTransactionInfo exact = activeByGuid.get(guid);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, ActiveTransactionInfo> entry : activeByGuid.entrySet()) {
            if (guid.equals(entry.getKey()) || entry.getKey().contains(guid)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void recordEvent(String guid, String stepKey, String label, long timestampMillis, boolean highlight) {
        stepHistoryByGuid.computeIfAbsent(guid, key -> new ArrayList<>())
                .add(new TransactionStepEvent(stepKey, label, timestampMillis, null, highlight));
    }

    private void sealPreviousDuration(String guid, long nowMillis) {
        List<TransactionStepEvent> history = stepHistoryByGuid.get(guid);
        if (history == null || history.isEmpty()) {
            return;
        }
        int lastIndex = history.size() - 1;
        TransactionStepEvent last = history.get(lastIndex);
        if (last.durationMs() == null) {
            long duration = Math.max(0L, nowMillis - last.timestampMillis());
            history.set(lastIndex, last.withDuration(duration).withHighlight(duration >= 1_000L));
        }
    }

    public List<ActiveTransactionInfo> snapshot() {
        return activeByGuid.values().stream()
                .sorted(Comparator.comparingLong(ActiveTransactionInfo::elapsedMillis).reversed())
                .toList();
    }

    public int count() {
        return activeByGuid.size();
    }

    public int countByBusinessCode(String businessCode) {
        if (businessCode == null) {
            return 0;
        }
        int count = 0;
        for (ActiveTransactionInfo info : activeByGuid.values()) {
            if (businessCode.equalsIgnoreCase(info.businessCode())) {
                count++;
            }
        }
        return count;
    }

    public int countByServiceId(String serviceId) {
        if (serviceId == null) {
            return 0;
        }
        int count = 0;
        for (ActiveTransactionInfo info : activeByGuid.values()) {
            if (serviceId.equals(info.serviceId())) {
                count++;
            }
        }
        return count;
    }

    public List<ActiveTransactionInfo> snapshotByBusinessCode(String businessCode) {
        List<ActiveTransactionInfo> rows = new ArrayList<>();
        for (ActiveTransactionInfo info : activeByGuid.values()) {
            if (businessCode == null || businessCode.equalsIgnoreCase(info.businessCode())) {
                rows.add(info);
            }
        }
        rows.sort(Comparator.comparingLong(ActiveTransactionInfo::elapsedMillis).reversed());
        return rows;
    }
}
