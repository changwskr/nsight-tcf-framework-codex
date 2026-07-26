package com.nh.nsight.marketing.om.application.service;

import com.nh.nsight.marketing.om.client.OmRuntimeRemoteClient;
import com.nh.nsight.marketing.om.config.OmRuntimeDiagnosticsProperties;
import com.nh.nsight.marketing.om.config.OmRuntimeDiagnosticsProperties.Target;
import com.nh.nsight.tcf.util.DateTimeUtil;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class RuntimeStatusCollector {
    private final OmRuntimeDiagnosticsProperties properties;
    private final OmRuntimeRemoteClient remoteClient;
    private final ExecutorService fetchExecutor;

    public RuntimeStatusCollector(
            OmRuntimeDiagnosticsProperties properties,
            OmRuntimeRemoteClient remoteClient) {
        this.properties = properties;
        this.remoteClient = remoteClient;
        int parallel = Math.max(1, properties.getMaxParallelRequests());
        this.fetchExecutor = Executors.newFixedThreadPool(parallel, runnable -> {
            Thread thread = new Thread(runnable, "om-runtime-fetch");
            thread.setDaemon(true);
            return thread;
        });
    }

    public Map<String, Object> collectAll() {
        String checkedAt = DateTimeUtil.nowKst();
        List<Map<String, Object>> targets = mapInParallel(enabledTargets(), this::collectOneTarget);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedAt", checkedAt);
        result.put("targets", targets);
        return result;
    }

    public List<Map<String, Object>> extractActiveTransactions(Map<String, Object> collected) {
        return extractFromStatuses(collected, "activeTransactions", 200);
    }

    public List<Map<String, Object>> extractSlowTransactions(Map<String, Object> collected, int limit) {
        return extractFromStatuses(collected, "slowTransactions", limit);
    }

    public List<Map<String, Object>> extractSlowSql(Map<String, Object> collected, int limit) {
        return extractFromStatuses(collected, "slowSql", limit);
    }

    public List<Map<String, Object>> extractServiceCpuUsage(Map<String, Object> collected, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object targetRows = collected.get("targets");
        if (!(targetRows instanceof List<?> list)) {
            return rows;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> targetRow)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> targetMap = (Map<String, Object>) targetRow;
            String businessCode = String.valueOf(targetMap.get("businessCode"));
            Object statusObj = targetMap.get("status");
            if (!(statusObj instanceof Map<?, ?> statusMap)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Object values = ((Map<String, Object>) statusMap).get("serviceCpuUsage");
            if (!(values instanceof List<?> valueList)) {
                continue;
            }
            for (Object value : valueList) {
                if (!(value instanceof Map<?, ?> valueMap)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> row = new LinkedHashMap<>((Map<String, Object>) valueMap);
                row.putIfAbsent("businessCode", businessCode);
                rows.add(row);
            }
        }
        rows.sort((a, b) -> Double.compare(toDouble(b.get("cpuSharePct")), toDouble(a.get("cpuSharePct"))));
        if (rows.size() > limit) {
            return rows.subList(0, limit);
        }
        return rows;
    }

    private static double toDouble(Object value) {
        if (value == null) {
            return 0;
        }
        return Double.parseDouble(String.valueOf(value));
    }

    public List<Map<String, Object>> collectThreads() {
        List<Map<String, Object>> rows = mapInParallel(enabledTargets(), target -> remoteClient.fetchThreads(target).stream()
                        .map(thread -> {
                            Map<String, Object> row = new LinkedHashMap<>(thread);
                            row.putIfAbsent("businessCode", target.getBusinessCode());
                            return row;
                        })
                        .toList())
                .stream()
                .flatMap(List::stream)
                .sorted((a, b) -> Long.compare(toLong(b.get("elapsedMs")), toLong(a.get("elapsedMs"))))
                .toList();
        return new ArrayList<>(rows);
    }

    public List<Map<String, Object>> collectActiveTransactions() {
        return collectRowsFromTargets(target -> remoteClient.fetchActiveTransactions(target));
    }

    public List<Map<String, Object>> collectSlowTransactions(int limit) {
        List<Map<String, Object>> rows = collectRowsFromTargets(
                target -> remoteClient.fetchSlowTransactions(target, limit));
        if (rows.size() > limit) {
            return new ArrayList<>(rows.subList(0, limit));
        }
        return rows;
    }

    public List<Map<String, Object>> collectSlowSql(int limit) {
        List<Map<String, Object>> rows = collectRowsFromTargets(
                target -> remoteClient.fetchSlowSql(target, limit));
        if (rows.size() > limit) {
            return new ArrayList<>(rows.subList(0, limit));
        }
        return rows;
    }

    private List<Map<String, Object>> collectRowsFromTargets(
            Function<Target, List<Map<String, Object>>> fetcher) {
        List<Map<String, Object>> rows = mapInParallel(enabledTargets(), target -> fetcher.apply(target).stream()
                        .map(row -> {
                            Map<String, Object> copy = new LinkedHashMap<>(row);
                            copy.putIfAbsent("businessCode", target.getBusinessCode());
                            return copy;
                        })
                        .toList())
                .stream()
                .flatMap(List::stream)
                .sorted((a, b) -> Long.compare(toLong(b.get("elapsedMs")), toLong(a.get("elapsedMs"))))
                .toList();
        return new ArrayList<>(rows);
    }

    @PreDestroy
    void shutdownExecutor() {
        fetchExecutor.shutdownNow();
    }

    private Map<String, Object> collectOneTarget(Target target) {
        Map<String, Object> status = remoteClient.fetchStatus(target);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("businessCode", target.getBusinessCode());
        row.put("baseUrl", target.getBaseUrl());
        row.put("reachable", status.getOrDefault("reachable", true));
        row.put("status", status);
        return row;
    }

    private <T, R> List<R> mapInParallel(List<T> items, Function<T, R> mapper) {
        if (items.isEmpty()) {
            return List.of();
        }
        if (items.size() == 1) {
            return List.of(mapper.apply(items.get(0)));
        }
        List<CompletableFuture<R>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> mapper.apply(item), fetchExecutor))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private List<Target> enabledTargets() {
        return properties.getTargets().stream()
                .filter(Target::isEnabled)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFromStatuses(
            Map<String, Object> collected, String field, int limit) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Object targetRows = collected.get("targets");
        if (!(targetRows instanceof List<?> list)) {
            return rows;
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> targetRow)) {
                continue;
            }
            String businessCode = String.valueOf(((Map<String, Object>) targetRow).get("businessCode"));
            Object statusObj = ((Map<String, Object>) targetRow).get("status");
            if (!(statusObj instanceof Map<?, ?> statusMap)) {
                continue;
            }
            Object values = ((Map<String, Object>) statusMap).get(field);
            if (!(values instanceof List<?> valueList)) {
                continue;
            }
            for (Object value : valueList) {
                if (!(value instanceof Map<?, ?> valueMap)) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>((Map<String, Object>) valueMap);
                row.putIfAbsent("businessCode", businessCode);
                rows.add(row);
            }
        }
        rows.sort((a, b) -> Long.compare(toLong(b.get("elapsedMs")), toLong(a.get("elapsedMs"))));
        if (rows.size() > limit) {
            return rows.subList(0, limit);
        }
        return rows;
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
