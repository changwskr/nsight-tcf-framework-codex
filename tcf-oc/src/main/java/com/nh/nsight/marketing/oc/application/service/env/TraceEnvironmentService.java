package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.support.EnvSettingDefinition;
import com.nh.nsight.marketing.oc.support.IntegratedEnvironmentGuideCatalog;
import com.nh.nsight.marketing.oc.application.dto.env.EnvSettingCategoryView;
import com.nh.nsight.marketing.oc.application.dto.env.EnvSettingItemView;
import com.nh.nsight.marketing.oc.application.dto.env.IntegratedEnvironmentView;
import com.nh.nsight.marketing.oc.application.dto.env.SettingMatchStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TraceEnvironmentService {

    private final Environment environment;
    private final RuntimeConfigResolver runtimeConfigResolver;
    private final ProjectBaselineService projectBaselineService;

    @Value("${spring.application.name}")
    private String applicationName;

    public TraceEnvironmentService(
            Environment environment,
            RuntimeConfigResolver runtimeConfigResolver,
            ProjectBaselineService projectBaselineService
    ) {
        this.environment = environment;
        this.runtimeConfigResolver = runtimeConfigResolver;
        this.projectBaselineService = projectBaselineService;
    }

    public IntegratedEnvironmentView loadIntegratedSettings() {
        return loadIntegratedSettings(runtimeConfigResolver.resolveAll());
    }

    public IntegratedEnvironmentView loadIntegratedSettings(Map<String, String> config) {
        Map<String, List<EnvSettingItemView>> byCategory = new LinkedHashMap<>();
        int match = 0;
        int warn = 0;
        int compared = 0;

        for (EnvSettingDefinition def : IntegratedEnvironmentGuideCatalog.definitions()) {
            String actual = resolveActual(def, config);
            SettingMatchStatus status = def.evaluate(actual);
            if (def.matchType() != EnvSettingDefinition.MatchType.INFO_ONLY && actual != null && !actual.isBlank()) {
                compared++;
                if (status == SettingMatchStatus.MATCH) {
                    match++;
                } else if (status == SettingMatchStatus.WARN) {
                    warn++;
                }
            }

            EnvSettingItemView item = new EnvSettingItemView(
                    def.key(),
                    def.label(),
                    def.guideValue(),
                    displayActual(actual),
                    def.source() != null ? def.source() : "—",
                    def.layer(),
                    status,
                    def.note()
            );
            byCategory.computeIfAbsent(def.categoryId(), k -> new ArrayList<>()).add(item);
        }

        var baseline = projectBaselineService.loadBaseline(null, null);
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("기준 TPS", baseline.baseTps() + " / 피크 " + baseline.peakTps()
                + " / 고피크 " + baseline.highPeakTps() + " / 스트레스 " + baseline.stressTps());
        summary.put("전체 사용자", baseline.totalUsers() + "명");
        summary.put("실요청 사용자", baseline.actualRequestUsers() + "명 (피크 "
                + baseline.actualRequestPeakPercent() + "%)");
        summary.put("세션 설계", baseline.sessionDesignCount() + " (여유 "
                + baseline.sessionBufferedMin() + "~" + baseline.sessionBufferedMax() + ")");
        summary.put("목표 응답", "p95 " + (baseline.targetP95Ms() / 1000) + "초");
        summary.put("AP VM", baseline.apVmSpec() + " · AP " + baseline.apCount() + "대");
        baseline.deploymentSummary().forEach(summary::put);

        return new IntegratedEnvironmentView(
                IntegratedEnvironmentGuideCatalog.GUIDE_TITLE,
                IntegratedEnvironmentGuideCatalog.GUIDE_VERSION,
                applicationName,
                List.of(environment.getActiveProfiles().length == 0
                        ? new String[]{"default"} : environment.getActiveProfiles()),
                environment.getProperty("nsight.ap-id", "—"),
                summary,
                IntegratedEnvironmentGuideCatalog.designRules(),
                IntegratedEnvironmentGuideCatalog.configurationCriteria(),
                buildCategories(byCategory),
                match,
                warn,
                compared
        );
    }

    private String resolveActual(EnvSettingDefinition def, Map<String, String> config) {
        if (def.propertyKey() == null) {
            return null;
        }
        String value = config.get(def.propertyKey());
        if (value == null && "nsight.absolute-session-timeout-hours".equals(def.propertyKey())) {
            String hours = environment.getProperty(def.propertyKey());
            return hours != null ? hours + "h" : null;
        }
        return value;
    }

    private String displayActual(String actual) {
        if (actual == null || actual.isBlank()) {
            return "— (미설정·외부)";
        }
        return actual;
    }

    private List<EnvSettingCategoryView> buildCategories(Map<String, List<EnvSettingItemView>> byCategory) {
        Map<String, EnvSettingDefinition> meta = IntegratedEnvironmentGuideCatalog.definitions().stream()
                .collect(Collectors.toMap(EnvSettingDefinition::categoryId, d -> d, (a, b) -> a));

        List<EnvSettingCategoryView> result = new ArrayList<>();
        for (Map.Entry<String, List<EnvSettingItemView>> e : byCategory.entrySet()) {
            EnvSettingDefinition sample = meta.get(e.getKey());
            result.add(new EnvSettingCategoryView(
                    e.getKey(),
                    sample.categoryTitle(),
                    sample.categoryDescription(),
                    e.getValue()
            ));
        }
        return result;
    }
}
