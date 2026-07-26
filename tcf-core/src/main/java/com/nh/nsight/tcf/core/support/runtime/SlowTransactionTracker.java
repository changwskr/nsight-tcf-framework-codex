package com.nh.nsight.tcf.core.support.runtime;

import com.nh.nsight.tcf.core.support.runtime.model.ActiveTransactionInfo;
import com.nh.nsight.tcf.core.support.runtime.model.SlowTransactionInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SlowTransactionTracker {
    private static final int MAX_SIZE = 200;
    private final long slowThresholdMillis;
    private final Deque<SlowTransactionInfo> recent = new ArrayDeque<>();

    public SlowTransactionTracker(
            @Value("${nsight.tcf.runtime.slow-transaction-threshold-ms:3000}") long slowThresholdMillis) {
        this.slowThresholdMillis = slowThresholdMillis;
    }

    public long slowThresholdMillis() {
        return slowThresholdMillis;
    }

    public synchronized void onComplete(ActiveTransactionInfo info) {
        if (info == null) {
            return;
        }
        long elapsed = info.elapsedMillis();
        if (elapsed < slowThresholdMillis) {
            return;
        }
        long now = System.currentTimeMillis();
        record(new SlowTransactionInfo(
                info.guid(),
                info.businessCode(),
                info.serviceId(),
                info.startTimeMillis(),
                now,
                elapsed,
                info.currentStep(),
                info.currentSqlId(),
                now));
    }

    public synchronized void record(SlowTransactionInfo info) {
        if (info == null) {
            return;
        }
        recent.addFirst(info);
        while (recent.size() > MAX_SIZE) {
            recent.removeLast();
        }
    }

    public synchronized List<SlowTransactionInfo> snapshot(int limit) {
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, MAX_SIZE);
        List<SlowTransactionInfo> rows = new ArrayList<>(Math.min(safeLimit, recent.size()));
        int index = 0;
        for (SlowTransactionInfo info : recent) {
            rows.add(info);
            index++;
            if (index >= safeLimit) {
                break;
            }
        }
        return rows;
    }

    public synchronized int countRecent(long sinceMillis) {
        int count = 0;
        for (SlowTransactionInfo info : recent) {
            if (info.recordedAtMillis() >= sinceMillis) {
                count++;
            }
        }
        return count;
    }
}
