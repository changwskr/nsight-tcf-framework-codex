package com.nh.nsight.tcf.core.support.runtime;

import com.nh.nsight.tcf.core.support.runtime.model.RuntimeTransactionStep;
import com.nh.nsight.tcf.core.support.runtime.model.TransactionStepEvent;
import java.util.ArrayList;
import java.util.List;

final class TransactionStepHistorySupport {
    private static final long SLOW_STEP_MS = 1_000L;

    private TransactionStepHistorySupport() {}

    static String labelFor(RuntimeTransactionStep step) {
        if (step == null) {
            return "미수집";
        }
        return switch (step) {
            case STF -> "STF 시작";
            case WAIT_HANDLER -> "Handler 대기";
            case HANDLER -> "Handler 시작";
            case FACADE -> "Facade 시작";
            case SERVICE -> "Service 시작";
            case RULE -> "Rule 시작";
            case WAIT_DB_CONNECTION -> "DB Connection 대기";
            case EXECUTING_SQL -> "SQL 실행 중";
            case WAIT_EXTERNAL -> "외부연계";
            case ETF -> "ETF";
            case COMPLETED -> "완료";
        };
    }

    static String labelForKey(String stepKey) {
        if (stepKey == null) {
            return "미수집";
        }
        return switch (stepKey) {
            case "REQUEST_ENTRY" -> "요청 진입";
            case "DB_CONNECTION_REQUEST" -> "DB Connection 요청";
            case "DB_CONNECTION_ACQUIRED" -> "DB Connection 획득";
            case "SQL_EXECUTION_START" -> "SQL 실행 시작";
            default -> labelFor(RuntimeTransactionStep.valueOf(stepKey));
        };
    }

    static List<TransactionStepEvent> exportEvents(List<TransactionStepEvent> source, long nowMillis) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<TransactionStepEvent> rows = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            TransactionStepEvent event = source.get(i);
            Long duration = event.durationMs();
            if (duration == null) {
                if (i < source.size() - 1) {
                    duration = Math.max(0L, source.get(i + 1).timestampMillis() - event.timestampMillis());
                } else {
                    duration = Math.max(0L, nowMillis - event.timestampMillis());
                }
            }
            boolean highlight = event.highlight() || duration >= SLOW_STEP_MS;
            rows.add(event.withDuration(duration).withHighlight(highlight));
        }
        return rows;
    }
}
