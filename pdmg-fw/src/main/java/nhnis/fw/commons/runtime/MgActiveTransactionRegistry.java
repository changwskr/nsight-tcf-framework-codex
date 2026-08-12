package nhnis.fw.commons.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import nhnis.fw.tcf.core.context.TransactionContext;

/**
 * 실행 중 온라인 거래 레지스트리 (런타임 진단용).
 */
@Component
public class MgActiveTransactionRegistry {

    private final ConcurrentHashMap<String, ActiveTx> active = new ConcurrentHashMap<>();

    public void begin(TransactionContext context) {
        if (context == null || !StringUtils.hasText(context.getServiceId())) {
            return;
        }
        String key = keyOf(context);
        active.put(key, new ActiveTx(
                context.getServiceId().trim(),
                resolveBusinessCode(context.getServiceId()),
                context.getGuid(),
                Thread.currentThread().threadId(),
                Thread.currentThread().getName(),
                System.currentTimeMillis(),
                "RUNNING"));
    }

    public void end(TransactionContext context) {
        if (context == null) {
            return;
        }
        active.remove(keyOf(context));
    }

    public int count() {
        return active.size();
    }

    public List<Map<String, Object>> snapshot(int limit) {
        int max = Math.max(1, limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (ActiveTx tx : active.values()) {
            if (rows.size() >= max) {
                break;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("businessCode", tx.businessCode());
            row.put("serviceId", tx.serviceId());
            row.put("guid", tx.guid());
            row.put("threadId", tx.threadId());
            row.put("threadName", tx.threadName());
            row.put("elapsedMs", Math.max(0L, now - tx.startedAtMs()));
            row.put("currentStep", tx.currentStep());
            rows.add(row);
        }
        return rows;
    }

    private static String keyOf(TransactionContext context) {
        String guid = context.getGuid();
        if (StringUtils.hasText(guid)) {
            return guid.trim();
        }
        return context.getServiceId() + "@" + Thread.currentThread().threadId() + "@" + System.identityHashCode(context);
    }

    static String resolveBusinessCode(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return "MG";
        }
        String id = serviceId.trim();
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < id.length() && letters.length() < 2; i++) {
            char c = id.charAt(i);
            if (Character.isLetter(c)) {
                letters.append(Character.toUpperCase(c));
            } else if (letters.length() > 0) {
                break;
            }
        }
        return letters.length() > 0 ? letters.toString() : "MG";
    }

    private record ActiveTx(
            String serviceId,
            String businessCode,
            String guid,
            long threadId,
            String threadName,
            long startedAtMs,
            String currentStep) {
    }
}
