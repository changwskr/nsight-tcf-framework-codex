package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.support.IntegratedEnvironmentGuideCatalog;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentResultItem;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentRunView;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentStatus;
import com.nh.nsight.marketing.oc.application.dto.env.ConcurrentFlowMapView;
import com.nh.nsight.marketing.oc.application.dto.env.ParsedConfigEntry;
import com.nh.nsight.marketing.oc.application.dto.env.TimeoutMapView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EnvironmentAssessmentService {

    private final TraceEnvironmentService traceEnvironmentService;
    private final RuntimeConfigResolver runtimeConfigResolver;
    private final ConfigParserService configParserService;
    private final EnvironmentRuleEngineService ruleEngineService;
    private final ConcurrentFlowMapService concurrentFlowMapService;
    private final AssessmentRunStore runStore;
    private final ProjectBaselineService projectBaselineService;

    public EnvironmentAssessmentService(
            TraceEnvironmentService traceEnvironmentService,
            RuntimeConfigResolver runtimeConfigResolver,
            ConfigParserService configParserService,
            EnvironmentRuleEngineService ruleEngineService,
            ConcurrentFlowMapService concurrentFlowMapService,
            AssessmentRunStore runStore,
            ProjectBaselineService projectBaselineService
    ) {
        this.traceEnvironmentService = traceEnvironmentService;
        this.runtimeConfigResolver = runtimeConfigResolver;
        this.configParserService = configParserService;
        this.ruleEngineService = ruleEngineService;
        this.concurrentFlowMapService = concurrentFlowMapService;
        this.runStore = runStore;
        this.projectBaselineService = projectBaselineService;
    }

    public AssessmentRunView runAssessment(String projectId, String envCode, boolean mergeUploaded) {
        Map<String, String> config = new LinkedHashMap<>(runtimeConfigResolver.resolveAll());
        if (mergeUploaded) {
            runStore.lastImport().ifPresent(imp -> configParserService.mergeInto(config, imp.entries()));
        }

        List<ParsedConfigEntry> importEntries = mergeUploaded
                ? runStore.lastImport().map(imp -> imp.entries()).orElse(List.of())
                : List.of();

        List<AssessmentResultItem> results = new ArrayList<>();
        results.addAll(ruleEngineService.evaluateThresholdRules(config, importEntries));

        Map<String, Long> chain = buildChain(config);
        AssessmentResultItem relation = ruleEngineService.evaluateTimeoutRelation(chain);
        results.add(relation);

        Map<String, Long> capacityChain = concurrentFlowMapService.buildCapacityChain(config);
        AssessmentResultItem concurrentRelation = concurrentFlowMapService.evaluateConcurrentRelation(capacityChain);
        results.add(concurrentRelation);
        AssessmentResultItem capacityTpsRelation = concurrentFlowMapService.evaluatePeakTpsVsActualRequest(config);
        results.add(capacityTpsRelation);

        int pass = 0;
        int warn = 0;
        int fail = 0;
        boolean critical = false;
        for (AssessmentResultItem item : results) {
            switch (item.status()) {
                case PASS -> pass++;
                case WARN -> warn++;
                case FAIL, EXCEPTION -> fail++;
                default -> { }
            }
            if ("CRITICAL".equalsIgnoreCase(item.severity()) && item.status() == AssessmentStatus.FAIL) {
                critical = true;
            }
            if ("TIMEOUT-REL-001".equals(item.ruleId()) && item.status() == AssessmentStatus.FAIL) {
                critical = true;
            }
            if (ConcurrentFlowMapService.CONCURRENT_REL_RULE.equals(item.ruleId())
                    && item.status() == AssessmentStatus.FAIL) {
                critical = true;
            }
            if (ConcurrentFlowMapService.CAPACITY_TPS_REL_RULE.equals(item.ruleId())
                    && item.status() == AssessmentStatus.FAIL) {
                critical = true;
            }
        }

        String runId = "ENV-RUN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TimeoutMapView timeoutMap = ruleEngineService.buildTimeoutMap(
                runId,
                chain,
                relation.status() == AssessmentStatus.PASS,
                config,
                importEntries
        );
        ConcurrentFlowMapView concurrentFlowMap = concurrentFlowMapService.buildConcurrentFlowMap(
                runId,
                capacityChain,
                concurrentRelation.status() == AssessmentStatus.PASS,
                config,
                importEntries
        );

        var baseline = projectBaselineService.loadBaseline(projectId, envCode);
        AssessmentRunView run = new AssessmentRunView(
                runId,
                baseline.projectId(),
                baseline.envCode(),
                LocalDateTime.now(),
                fail > 0 ? "FAIL" : (warn > 0 ? "WARN" : "PASS"),
                pass,
                warn,
                fail,
                results.size(),
                critical,
                IntegratedEnvironmentGuideCatalog.configurationCriteria(),
                results,
                timeoutMap,
                concurrentFlowMap,
                traceEnvironmentService.loadIntegratedSettings()
        );
        runStore.save(run);
        return run;
    }

    public AssessmentRunView getRun(String runId) {
        return runStore.find(runId)
                .orElseThrow(() -> new IllegalArgumentException("점검 실행을 찾을 수 없습니다: " + runId));
    }

    private Map<String, Long> buildChain(Map<String, String> config) {
        Map<String, Long> chain = runtimeConfigResolver.resolveTimeoutChainMs();
        chain.put("dbQueryMs", RuntimeConfigResolver.parseMs(
                config.getOrDefault("mybatis.default-statement-timeout", chain.get("dbQueryMs") + " ms"),
                chain.get("dbQueryMs")));
        chain.put("hikariMs", RuntimeConfigResolver.parseMs(
                config.getOrDefault("spring.datasource.hikari.connection-timeout",
                        chain.get("hikariMs") + " ms"), chain.get("hikariMs")));
        chain.put("transactionMs", RuntimeConfigResolver.parseSecondsToMs(
                config.getOrDefault("nsight.transaction.default-timeout-seconds",
                        (chain.get("transactionMs") / 1000) + " s"), chain.get("transactionMs")));
        chain.put("proxyMs", RuntimeConfigResolver.parseMs(
                config.getOrDefault("nsight.webtop.read-timeout-ms", chain.get("proxyMs") + " ms"),
                chain.get("proxyMs")));
        chain.put("clientMs", RuntimeConfigResolver.parseMs(
                config.getOrDefault("nsight.webtop.request-timeout-ms", chain.get("clientMs") + " ms"),
                chain.get("clientMs")));
        return chain;
    }
}
