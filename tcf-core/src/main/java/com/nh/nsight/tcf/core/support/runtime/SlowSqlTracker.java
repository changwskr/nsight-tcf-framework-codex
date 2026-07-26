package com.nh.nsight.tcf.core.support.runtime;

import com.nh.nsight.tcf.core.support.runtime.model.SlowSqlInfo;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SlowSqlTracker {
    private static final int MAX_SIZE = 200;
    private final Deque<SlowSqlInfo> recent = new ArrayDeque<>();

    public synchronized void record(SlowSqlInfo info) {
        if (info == null) {
            return;
        }
        recent.addFirst(info);
        while (recent.size() > MAX_SIZE) {
            recent.removeLast();
        }
    }

    public synchronized List<SlowSqlInfo> snapshot(int limit) {
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, MAX_SIZE);
        List<SlowSqlInfo> rows = new ArrayList<>(Math.min(safeLimit, recent.size()));
        int index = 0;
        for (SlowSqlInfo info : recent) {
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
        for (SlowSqlInfo info : recent) {
            if (info.recordedAtMillis() >= sinceMillis) {
                count++;
            }
        }
        return count;
    }
}
