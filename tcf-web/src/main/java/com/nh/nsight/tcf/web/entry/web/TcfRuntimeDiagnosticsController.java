package com.nh.nsight.tcf.web.entry.web;

import com.nh.nsight.tcf.web.support.runtime.TcfRuntimeMonitor;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/runtime")
@ConditionalOnProperty(prefix = "nsight.tcf", name = "runtime-monitor-enabled", havingValue = "true", matchIfMissing = true)
public class TcfRuntimeDiagnosticsController {
    private final TcfRuntimeMonitor runtimeMonitor;

    public TcfRuntimeDiagnosticsController(TcfRuntimeMonitor runtimeMonitor) {
        this.runtimeMonitor = runtimeMonitor;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return runtimeMonitor.createStatusSnapshot();
    }

    @GetMapping("/slow-transactions")
    public List<Map<String, Object>> slowTransactions(
            @RequestParam(defaultValue = "50") int limit) {
        return runtimeMonitor.getSlowTransactions(limit);
    }

    @GetMapping("/active-transactions")
    public List<Map<String, Object>> activeTransactions() {
        return runtimeMonitor.getActiveTransactions();
    }

    @GetMapping("/slow-sql")
    public List<Map<String, Object>> slowSql(@RequestParam(defaultValue = "50") int limit) {
        return runtimeMonitor.getSlowSqlList(limit);
    }

    @GetMapping("/threads")
    public List<Map<String, Object>> threads() {
        return runtimeMonitor.getThreadDetails();
    }
}
