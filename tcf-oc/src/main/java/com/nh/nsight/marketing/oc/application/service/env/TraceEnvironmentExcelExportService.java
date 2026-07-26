package com.nh.nsight.marketing.oc.application.service.env;

import com.nh.nsight.marketing.oc.application.dto.env.AssessmentResultItem;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentRunView;
import com.nh.nsight.marketing.oc.application.dto.env.AssessmentStatus;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityDesignView;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerRequest;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityPlannerResult;
import com.nh.nsight.marketing.oc.application.dto.env.CapacityVmResultRow;
import com.nh.nsight.marketing.oc.application.dto.env.StackLayerView;
import com.nh.nsight.marketing.oc.application.dto.env.StackSettingRow;
import com.nh.nsight.marketing.oc.application.dto.env.ConcurrentFlowMapNode;
import com.nh.nsight.marketing.oc.application.dto.env.ConcurrentFlowMapView;
import com.nh.nsight.marketing.oc.application.dto.env.EnvSettingCategoryView;
import com.nh.nsight.marketing.oc.application.dto.env.EnvSettingItemView;
import com.nh.nsight.marketing.oc.application.dto.env.IntegratedEnvironmentView;
import com.nh.nsight.marketing.oc.application.dto.env.JvmSizingRecommendation;
import com.nh.nsight.marketing.oc.application.dto.env.LayerGridRow;
import com.nh.nsight.marketing.oc.application.dto.env.ProjectBaselineView;
import com.nh.nsight.marketing.oc.application.dto.env.SettingMatchStatus;
import com.nh.nsight.marketing.oc.application.dto.env.TimeoutMapNode;
import com.nh.nsight.marketing.oc.application.dto.env.TimeoutMapView;
import com.nh.nsight.marketing.oc.application.dto.env.TraceEnvironmentExportRequest;
import com.nh.nsight.marketing.oc.application.dto.env.TraceEnvironmentExportType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TraceEnvironmentExcelExportService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public byte[] export(TraceEnvironmentExportRequest request) throws IOException {
        TraceEnvironmentExportType type = TraceEnvironmentExportType.from(request.exportType());
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            switch (type) {
                case ENV003 -> exportEnv003(workbook, headerStyle, requireCapacity(request));
                case ENV004 -> exportEnv004(workbook, headerStyle, requireCapacity(request));
                case CHECK -> exportCheck(workbook, headerStyle, requireCapacity(request),
                        request.capacityRequest(), request.assessmentRun());
                case RULE_CHECK -> exportRuleCheck(workbook, headerStyle, request);
                default -> throw new IllegalArgumentException("지원하지 않는 exportType");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public String filename(TraceEnvironmentExportRequest request) {
        TraceEnvironmentExportType type = TraceEnvironmentExportType.from(request.exportType());
        String scenario = request.capacityView() != null ? request.capacityView().scenarioId() : "report";
        if (scenario == null || scenario.isBlank()) {
            scenario = "report";
        }
        String prefix = type == TraceEnvironmentExportType.CHECK ? "NSIGHT_종합보고서" : "NSIGHT_" + type.name();
        return prefix + "_" + scenario + "_" + FILE_TS.format(LocalDateTime.now()) + ".xlsx";
    }

    private CapacityDesignView requireCapacity(TraceEnvironmentExportRequest request) {
        if (request.capacityView() == null || request.capacityView().planner() == null) {
            throw new IllegalArgumentException("산정 데이터가 없습니다. ENV-002에서 「산정 실행」 후 다시 시도하세요.");
        }
        return request.capacityView();
    }

    private void exportEnv003(Workbook wb, CellStyle headerStyle, CapacityDesignView view) {
        CapacityPlannerResult p = view.planner();
        writeSummarySheet(wb, headerStyle, view, p);
        Sheet table = wb.createSheet("TPS·VM결과");
        int row = writeTitle(table, 0, "ENV-003 TPS·VM 산정 결과");
        row = writeTable(table, row, headerStyle,
                new String[]{
                        "요청률%", "Timeout(초)", "실요청자", "목표TPS", "TPMC", "VM사양", "VM TPS(기준)",
                        "필요VM(센터)", "A-A권장", "JVM Heap", "SV Heap", "WAS Threads", "DB Pool/VM",
                        "DB Pool합계", "판정", "판정사유"
                },
                vmResultRows(p.vmResults()));
        autosize(table, 16);
        Sheet formulas = wb.createSheet("산출식");
        int fRow = writeTitle(formulas, 0, "산출식·가이드");
        fRow = writeKeyValues(formulas, fRow, plannerDerivationRows(p));
        autosize(formulas, 2);
    }

    private void exportEnv004(Workbook wb, CellStyle headerStyle, CapacityDesignView view) {
        writeSummarySheet(wb, headerStyle, view, view.planner());
        Sheet criteria = wb.createSheet("점검기준");
        int row = writeTitle(criteria, 0, "ENV-004 점검 기준");
        row = writeKeyValues(criteria, row, kvList(
                kv("응답 Timeout(초)", String.valueOf(view.activeResponseTimeoutSec())),
                kv("세션 Idle(분)", String.valueOf(view.activeSessionMinutes())),
                kv("스택 판정", view.stackValid() ? "STACK OK" : "STACK 점검 필요")
        ));
        autosize(criteria, 2);
        Sheet grid = wb.createSheet("계층Grid");
        row = writeTitle(grid, 0, "계층별 설정 점검");
        row = writeTable(grid, row, headerStyle, layerGridHeaders(), layerGridRows(view.layerGrid()));
        autosize(grid, layerGridHeaders().length);
        if (view.jvmSizing() != null) {
            Sheet jvm = wb.createSheet("JVM사이징");
            int jRow = writeTitle(jvm, 0, "JVM Heap·GC 사이징");
            jRow = writeKeyValues(jvm, jRow, jvmRows(view.jvmSizing()));
            autosize(jvm, 2);
        }
    }

    /** 종합 보고서 화면 §1~6 전체 (UI와 동일 구성). */
    private void exportCheck(
            Workbook wb,
            CellStyle headerStyle,
            CapacityDesignView view,
            CapacityPlannerRequest capacityRequest,
            AssessmentRunView assessment
    ) {
        CapacityPlannerResult p = view.planner();
        Map<String, Integer> layerCounts = countLayerGrid(view.layerGrid());
        CapacityVmResultRow example = pickExampleVmRow(p);

        Sheet conclusion = wb.createSheet("0_종합결론");
        int row = writeTitle(conclusion, 0, "통합 환경설정 점검 · 종합 보고서");
        writeKeyValues(conclusion, row, buildCheckConclusionRows(view, p, layerCounts, assessment, example));
        autosize(conclusion, 2);

        Sheet env002 = wb.createSheet("1_ENV002_조건");
        row = writeTitle(env002, 0, "1. ENV-002 산정 조건");
        writeKeyValues(env002, row, buildEnv002ConditionRows(p, capacityRequest));
        autosize(env002, 2);

        Sheet env003 = wb.createSheet("2_ENV003_TPS·VM");
        row = writeTitle(env003, 0, "2. ENV-003 TPS·VM 산정 결론");
        row = writeKeyValues(env003, row, buildEnv003SummaryRows(p, example));
        row++;
        writeTable(env003, row, headerStyle,
                new String[]{"요청률%", "Timeout(초)", "실요청자", "목표TPS", "필요VM(센터)", "A-A권장", "판정"},
                env003ReportVmRows(p.vmResults()));
        autosize(env003, 7);
        Sheet env003full = wb.createSheet("2b_ENV003_전체표");
        row = writeTitle(env003full, 0, "ENV-003 TPS·VM 전체 시나리오");
        writeTable(env003full, row + 1, headerStyle,
                new String[]{
                        "요청률%", "Timeout(초)", "실요청자", "목표TPS", "TPMC", "VM사양", "VM TPS(기준)",
                        "필요VM(센터)", "A-A권장", "JVM Heap", "SV Heap", "WAS Threads", "DB Pool/VM",
                        "DB Pool합계", "판정", "판정사유"
                },
                vmResultRows(p.vmResults()));
        autosize(env003full, 16);

        Sheet env004 = wb.createSheet("3_ENV004_요약");
        row = writeTitle(env004, 0, "3. ENV-004 계층 점검 요약");
        writeKeyValues(env004, row, buildEnv004SummaryRows(view, layerCounts));
        autosize(env004, 2);

        Sheet coreTps = wb.createSheet("4_Core·VM처리량");
        row = writeTitle(coreTps, 0, "4. Core·VM 처리량 (선정값)");
        writeKeyValues(coreTps, row, buildCoreTpsRows(p));
        autosize(coreTps, 2);

        Sheet stackMaster = wb.createSheet("5a_환경구성_Grid");
        row = writeTitle(stackMaster, 0, "5. 환경 구성 상세 — 통합 Grid");
        row = writeKeyValues(stackMaster, row, kvList(
                kv("요청 흐름", stackFlowLabel(view.stackLayers()))
        ));
        row++;
        writeTable(stackMaster, row, headerStyle, layerGridHeaders(), layerGridRows(view.layerGrid()));
        autosize(stackMaster, layerGridHeaders().length);

        Sheet stackLayers = wb.createSheet("5b_환경구성_계층별");
        row = writeTitle(stackLayers, 0, "5. 환경 구성 — 계층별 상세");
        writeTable(stackLayers, row + 1, headerStyle,
                new String[]{"순서", "계층", "설정", "Property", "권장", "현재", "판정", "사유", "위치", "예시", "조치"},
                stackLayerDetailRows(view));
        autosize(stackLayers, 11);

        Sheet formulas = wb.createSheet("산출식");
        row = writeTitle(formulas, 0, "산출식·가이드");
        writeKeyValues(formulas, row, plannerDerivationRows(p));
        autosize(formulas, 2);

        if (view.jvmSizing() != null) {
            Sheet jvm = wb.createSheet("JVM사이징");
            int jRow = writeTitle(jvm, 0, "JVM Heap·GC (§5 JVM)");
            writeKeyValues(jvm, jRow, jvmRows(view.jvmSizing()));
            autosize(jvm, 2);
        }

        Sheet ruleSect = wb.createSheet("6_RuleEngine");
        row = writeTitle(ruleSect, 0, "6. Rule Engine");
        if (assessment != null) {
            writeKeyValues(ruleSect, row, kvList(
                    kv("Run ID", assessment.runId()),
                    kv("상태", assessment.status()),
                    kv("통과/주의/실패",
                            assessment.passCount() + " / " + assessment.warnCount() + " / " + assessment.failCount()),
                    kv("Critical Blocking", assessment.criticalBlocking() ? "Y" : "N")
            ));
            writeAssessmentSheet(wb, headerStyle, assessment);
            writeTimeoutSheet(wb, headerStyle, assessment.timeoutMap());
            writeConcurrentFlowSheet(wb, headerStyle, assessment.concurrentFlowMap());
            if (assessment.settingsSnapshot() != null) {
                writeSc007Sheet(wb, headerStyle, assessment.settingsSnapshot());
            }
        } else {
            writeKeyValues(ruleSect, row, kvList(kv("상태", "미실행 — Rule 점검 탭에서 점검 실행")));
            autosize(ruleSect, 2);
        }
    }

    private List<String[]> buildCheckConclusionRows(
            CapacityDesignView view,
            CapacityPlannerResult p,
            Map<String, Integer> layerCounts,
            AssessmentRunView assessment,
            CapacityVmResultRow example
    ) {
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("종합 결론", buildOverallConclusionText(p, layerCounts, assessment)));
        rows.add(kv("시나리오 ID", view.scenarioId()));
        rows.add(kv("시나리오", p.scenarioLabel()));
        Map<String, Integer> risk = p.riskSummary() != null ? p.riskSummary() : Map.of();
        rows.add(kv("VM 산정", "정상 " + risk.getOrDefault("normal", 0)
                + " · 경고 " + risk.getOrDefault("warning", 0)
                + " · 위험 " + risk.getOrDefault("critical", 0)));
        rows.add(kv("ENV-004 스택", view.stackValid() ? "STACK OK" : "STACK 점검 필요"));
        rows.add(kv("계층 항목", "정상 " + layerCounts.getOrDefault("NORMAL", 0)
                + " · 경고 " + layerCounts.getOrDefault("WARN", 0)
                + " · 위험 " + layerCounts.getOrDefault("CRITICAL", 0)));
        if (example != null) {
            rows.add(kv("대표 시나리오 (5%·3초)",
                    "실요청 " + example.realRequesters() + "명 → TPS " + example.targetTps()
                            + " → VM " + example.requiredVmSingleCenter() + "대 (A-A "
                            + example.recommendedVmActiveActive() + "대)"));
        }
        if (assessment != null) {
            rows.add(kv("Rule Engine", assessment.status() + " · 통과 " + assessment.passCount()
                    + " / 주의 " + assessment.warnCount() + " / 실패 " + assessment.failCount()));
        } else {
            rows.add(kv("Rule Engine", "미실행"));
        }
        rows.add(kv("보고서 생성", "브라우저 산정 캐시 · Rule은 점검 실행 시 갱신"));
        return rows;
    }

    private String buildOverallConclusionText(
            CapacityPlannerResult p,
            Map<String, Integer> layerCounts,
            AssessmentRunView assessment
    ) {
        int critVm = countVmByStatus(p, "CRITICAL");
        int critLayer = layerCounts.getOrDefault("CRITICAL", 0);
        if (critVm > 0) {
            return "VM 산정 " + critVm + "건 위험 — ENV-003 확인";
        }
        if (critLayer > 0) {
            return "계층 설정 " + critLayer + "건 위험 — ENV-004 확인";
        }
        if (assessment != null && "FAIL".equalsIgnoreCase(assessment.status())) {
            return "Rule Engine " + assessment.failCount() + "건 실패 — Rule 점검 탭에서 상세 확인";
        }
        return "산정·계층 점검 기준 충족";
    }

    private int countVmByStatus(CapacityPlannerResult p, String status) {
        if (p.vmResults() == null) {
            return 0;
        }
        int n = 0;
        for (CapacityVmResultRow r : p.vmResults()) {
            if (status.equalsIgnoreCase(r.status())) {
                n++;
            }
        }
        return n;
    }

    private List<String[]> buildEnv002ConditionRows(CapacityPlannerResult p, CapacityPlannerRequest req) {
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("전체 사용자", p.totalUsers() + "명 (" + p.branchCount() + "지점 × " + p.usersPerBranch() + "명)"));
        rows.add(kv("VM 프로파일", p.vmProfileId() + " · " + p.vmCores() + "코어/" + p.vmMemoryGb() + "GB"));
        rows.add(kv("TPMC/TPS", String.valueOf(p.tpmcPerTps())));
        rows.add(kv("Core TPS 선정", p.tpsPerCoreBase() + " (보수 " + p.tpsPerCoreMin() + " ~ 여유 " + p.tpsPerCoreMax() + ")"));
        rows.add(kv("Core TPMC/초", String.valueOf(p.coreTpmcPerSec())));
        rows.add(kv("TPMC 연동", p.coreTpsLinkedToTpmc() ? "ON" : "OFF"));
        rows.add(kv("세션 설계", p.designSessions() + "명 · Idle " + p.primarySessionMinutes() + "분"));
        rows.add(kv("Active-Active", p.activeActive() ? "적용" : "미적용"));
        rows.add(kv("DB Session 한도", String.valueOf(p.dbSessionLimit())));
        rows.add(kv("Hikari Pool/VM", String.valueOf(p.hikariPoolPerVm())));
        if (req != null) {
            rows.add(kv("시나리오명", nullToDash(req.scenarioName())));
            rows.add(kv("실요청률(%)", joinInts(req.actualRequestPercents())));
            rows.add(kv("응답 Timeout(초)", joinInts(req.responseTimeoutSeconds())));
            rows.add(kv("세션 Idle(분)", joinInts(req.sessionIdleMinutes())));
            rows.add(kv("DR 검증", req.drValidation() ? "Y" : "N"));
            rows.add(kv("DB Pool 검증", req.validateDbPool() ? "Y" : "N"));
            if (req.customVm()) {
                rows.add(kv("Custom VM", req.customCore() + "C / " + req.customMemoryGb() + "GB"));
            }
        }
        rows.add(kv("요약 산출식", nullToDash(p.summaryFormula())));
        return rows;
    }

    private List<String[]> buildEnv003SummaryRows(CapacityPlannerResult p, CapacityVmResultRow example) {
        Map<String, Integer> risk = p.riskSummary() != null ? p.riskSummary() : Map.of();
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("시나리오 조합 수", String.valueOf(p.vmResults() != null ? p.vmResults().size() : 0)));
        rows.add(kv("판정 정상/경고/위험",
                risk.getOrDefault("normal", 0) + " / "
                        + risk.getOrDefault("warning", 0) + " / "
                        + risk.getOrDefault("critical", 0)));
        if (example != null) {
            rows.add(kv("대표 5%·3초",
                    "실요청 " + example.realRequesters() + " → TPS " + example.targetTps()
                            + " → VM " + example.requiredVmSingleCenter() + " (A-A "
                            + example.recommendedVmActiveActive() + ")"));
        }
        return rows;
    }

    private List<String[]> env003ReportVmRows(List<CapacityVmResultRow> vmResults) {
        if (vmResults == null) {
            return List.of();
        }
        List<String[]> rows = new ArrayList<>();
        for (CapacityVmResultRow r : vmResults) {
            rows.add(new String[]{
                    String.valueOf(r.requestRatePercent()),
                    String.valueOf(r.timeoutSec()),
                    String.valueOf(r.realRequesters()),
                    String.valueOf(r.targetTps()),
                    String.valueOf(r.requiredVmSingleCenter()),
                    String.valueOf(r.recommendedVmActiveActive()),
                    r.status()
            });
        }
        return rows;
    }

    private List<String[]> buildEnv004SummaryRows(CapacityDesignView view, Map<String, Integer> layerCounts) {
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("스택 판정", view.stackValid() ? "STACK OK" : "STACK 점검 필요"));
        rows.add(kv("계층 항목 정상", String.valueOf(layerCounts.getOrDefault("NORMAL", 0))));
        rows.add(kv("계층 항목 경고", String.valueOf(layerCounts.getOrDefault("WARN", 0))));
        rows.add(kv("계층 항목 위험", String.valueOf(layerCounts.getOrDefault("CRITICAL", 0))));
        rows.add(kv("응답 Timeout(초)", String.valueOf(view.activeResponseTimeoutSec())));
        rows.add(kv("세션 Idle(분)", String.valueOf(view.activeSessionMinutes())));
        JvmSizingRecommendation jvm = view.jvmSizing();
        if (jvm != null) {
            rows.add(kv("JVM Heap 일반", jvm.heapGeneralMinGb() + "~" + jvm.heapGeneralMaxGb() + " GB"));
            rows.add(kv("JVM Heap SV", "≤" + jvm.heapSingleViewMaxGb() + " GB"));
        }
        return rows;
    }

    private List<String[]> buildCoreTpsRows(CapacityPlannerResult p) {
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("보수 (min) TPS/Core", String.valueOf(p.tpsPerCoreMin())));
        rows.add(kv("기준 (base) TPS/Core · 선정", String.valueOf(p.tpsPerCoreBase())));
        rows.add(kv("여유 (max) TPS/Core", String.valueOf(p.tpsPerCoreMax())));
        rows.add(kv("TPMC/TPS", String.valueOf(p.tpmcPerTps())));
        rows.add(kv("Core TPMC/초", String.valueOf(p.coreTpmcPerSec())));
        if (p.coreTpsLinkedToTpmc()) {
            rows.add(kv("TPMC 연동", "ON · base = Core TPMC ÷ TPMC/TPS"));
        } else {
            rows.add(kv("TPMC 연동", "OFF · Grid는 기준 TPS/Core만 사용"));
        }
        rows.add(kv("VM TPS(선정)", p.vmCores() + " × " + p.tpsPerCoreBase() + " = " + p.vmTpsAt35()));
        return rows;
    }

    private List<String[]> stackLayerDetailRows(CapacityDesignView view) {
        List<String[]> rows = new ArrayList<>();
        List<StackLayerView> layers = view.stackLayers() != null ? view.stackLayers() : List.of();
        for (StackLayerView layer : layers) {
            if (layer.settings() == null) {
                continue;
            }
            for (StackSettingRow s : layer.settings()) {
                rows.add(new String[]{
                        String.valueOf(layer.order()),
                        layer.layerName(),
                        s.settingLabel(),
                        nullToDash(s.propertyKey()),
                        nullToDash(s.recommendedValue()),
                        nullToDash(s.actualValue()),
                        s.statusLabel() != null ? s.statusLabel() : s.status(),
                        nullToDash(s.reason()),
                        nullToDash(s.configFile()),
                        nullToDash(s.settingExample()),
                        nullToDash(s.actionGuide())
                });
            }
        }
        return rows;
    }

    private CapacityVmResultRow pickExampleVmRow(CapacityPlannerResult p) {
        if (p.vmResults() == null || p.vmResults().isEmpty()) {
            return null;
        }
        for (CapacityVmResultRow r : p.vmResults()) {
            if (r.requestRatePercent() == 5 && r.timeoutSec() == 3) {
                return r;
            }
        }
        for (CapacityVmResultRow r : p.vmResults()) {
            if (r.requestRatePercent() == 5) {
                return r;
            }
        }
        return p.vmResults().get(0);
    }

    private Map<String, Integer> countLayerGrid(List<LayerGridRow> grid) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("NORMAL", 0);
        counts.put("WARN", 0);
        counts.put("CRITICAL", 0);
        if (grid == null) {
            return counts;
        }
        for (LayerGridRow r : grid) {
            String st = r.status() != null ? r.status().toUpperCase() : "";
            if ("NORMAL".equals(st)) {
                counts.merge("NORMAL", 1, Integer::sum);
            } else if ("WARN".equals(st)) {
                counts.merge("WARN", 1, Integer::sum);
            } else {
                counts.merge("CRITICAL", 1, Integer::sum);
            }
        }
        return counts;
    }

    private String stackFlowLabel(List<StackLayerView> layers) {
        if (layers == null || layers.isEmpty()) {
            return "—";
        }
        return layers.stream()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .map(StackLayerView::layerName)
                .collect(Collectors.joining(" → "));
    }

    private static String joinInts(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return "—";
        }
        return values.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    private void exportRuleCheck(Workbook wb, CellStyle headerStyle, TraceEnvironmentExportRequest request) {
        if (request.baseline() != null) {
            Sheet baseline = wb.createSheet("기준정보_SC002");
            int row = writeTitle(baseline, 0, "프로젝트 기준정보 (SC-002)");
            row = writeKeyValues(baseline, row, baselineRows(request.baseline()));
            autosize(baseline, 2);
        }
        AssessmentRunView run = request.assessmentRun();
        if (run != null) {
            writeAssessmentSheet(wb, headerStyle, run);
            writeTimeoutSheet(wb, headerStyle, run.timeoutMap());
            writeConcurrentFlowSheet(wb, headerStyle, run.concurrentFlowMap());
            IntegratedEnvironmentView settings = run.settingsSnapshot();
            if (settings != null) {
                writeSc007Sheet(wb, headerStyle, settings);
            }
        } else {
            Sheet note = wb.createSheet("안내");
            writeTitle(note, 0, "Rule 점검 데이터 없음");
            writeKeyValues(note, 1, kvList(kv("안내", "「점검 실행」 후 Excel을 다시 내려받으세요.")));
            autosize(note, 2);
        }
        if (request.capacityView() != null && request.capacityView().planner() != null) {
            Sheet cap = wb.createSheet("ENV002_003_연동");
            int row = writeTitle(cap, 0, "ENV-002·003 산정 연동 요약");
            writeKeyValues(cap, row, plannerDerivationRows(request.capacityView().planner()));
            autosize(cap, 2);
        }
    }

    private void writeSummarySheet(Workbook wb, CellStyle headerStyle, CapacityDesignView view, CapacityPlannerResult p) {
        Sheet sheet = wb.createSheet("산정요약");
        int row = writeTitle(sheet, 0, "산정 요약");
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("시나리오 ID", view.scenarioId()));
        rows.add(kv("시나리오", p.scenarioLabel()));
        rows.add(kv("지점 수", String.valueOf(p.branchCount())));
        rows.add(kv("지점당 사용자", String.valueOf(p.usersPerBranch())));
        rows.add(kv("전체 사용자", String.valueOf(p.totalUsers())));
        rows.add(kv("세션 설계", String.valueOf(p.designSessions())));
        rows.add(kv("VM 프로파일", p.vmProfileId()));
        rows.add(kv("VM 코어/메모리", p.vmCores() + " / " + p.vmMemoryGb() + " GB"));
        rows.add(kv("Core TPS (min/base/max)",
                p.tpsPerCoreMin() + " / " + p.tpsPerCoreBase() + " / " + p.tpsPerCoreMax()));
        rows.add(kv("TPMC/TPS", String.valueOf(p.tpmcPerTps())));
        rows.add(kv("Active-Active", p.activeActive() ? "적용" : "미적용"));
        rows.add(kv("DB Session 한도", String.valueOf(p.dbSessionLimit())));
        rows.add(kv("요약 산출식", p.summaryFormula()));
        if (p.riskSummary() != null) {
            rows.add(kv("판정 정상", String.valueOf(p.riskSummary().getOrDefault("normal", 0))));
            rows.add(kv("판정 경고", String.valueOf(p.riskSummary().getOrDefault("warning", 0))));
            rows.add(kv("판정 위험", String.valueOf(p.riskSummary().getOrDefault("critical", 0))));
        }
        writeKeyValues(sheet, row, rows);
        autosize(sheet, 2);
    }

    private void writeAssessmentSheet(Workbook wb, CellStyle headerStyle, AssessmentRunView run) {
        Sheet sheet = wb.createSheet("Rule결과");
        int row = writeTitle(sheet, 0, "Rule Engine 점검 (" + run.runId() + ")");
        row = writeKeyValues(sheet, row, kvList(
                kv("상태", run.status()),
                kv("통과", String.valueOf(run.passCount())),
                kv("주의", String.valueOf(run.warnCount())),
                kv("실패", String.valueOf(run.failCount())),
                kv("Critical Blocking", run.criticalBlocking() ? "Y" : "N")
        ));
        row++;
        writeTable(sheet, row, headerStyle,
                new String[]{"Rule", "유형", "심각도", "설명", "권장값", "실측값", "판정", "설정파일", "Property"},
                assessmentRows(run.results()));
        autosize(sheet, 9);
    }

    private void writeTimeoutSheet(Workbook wb, CellStyle headerStyle, TimeoutMapView map) {
        if (map == null) {
            return;
        }
        Sheet sheet = wb.createSheet("Timeout_SC008");
        int row = writeTitle(sheet, 0, "Timeout Map (" + map.chainRuleId() + ")");
        row = writeKeyValues(sheet, row, kvList(
                kv("체인 판정", map.chainValid() ? "OK" : "FAIL"),
                kv("요약", map.chainSummary())
        ));
        row++;
        writeTable(sheet, row, headerStyle,
                new String[]{"순서", "계층", "항목", "값", "설정값", "Property", "파일", "가이드", "노트"},
                timeoutRows(map.nodes()));
        autosize(sheet, 9);
    }

    private void writeConcurrentFlowSheet(Workbook wb, CellStyle headerStyle, ConcurrentFlowMapView map) {
        if (map == null) {
            return;
        }
        Sheet sheet = wb.createSheet("동시요청_SC009");
        int row = writeTitle(sheet, 0, "동시 요청자 Flow (" + map.chainRuleId() + ")");
        row = writeKeyValues(sheet, row, kvList(
                kv("체인 판정", map.chainValid() ? "OK" : "FAIL"),
                kv("요약", map.chainSummary()),
                kv("실요청(전사)", String.valueOf(map.actualRequestUsersTotal())),
                kv("AP당 동시", String.valueOf(map.estimatedConcurrentPerAp())),
                kv("TPS(실요청)", String.valueOf(map.peakTpsFromActualRequest())),
                kv("peak-tps 설정", String.valueOf(map.configuredPeakTps()))
        ));
        row++;
        writeTable(sheet, row, headerStyle,
                new String[]{"순서", "계층", "항목", "값", "설정값", "Property", "파일", "가이드", "노트"},
                flowRows(map.nodes()));
        autosize(sheet, 9);
    }

    private void writeSc007Sheet(Workbook wb, CellStyle headerStyle, IntegratedEnvironmentView settings) {
        Sheet sheet = wb.createSheet("SC007_설정대조");
        int row = writeTitle(sheet, 0, "SC-007 설정 대조 (" + settings.guideVersion() + ")");
        row = writeKeyValues(sheet, row, kvList(
                kv("일치", String.valueOf(settings.matchCount())),
                kv("주의", String.valueOf(settings.warnCount())),
                kv("비교 항목", String.valueOf(settings.totalCompared()))
        ));
        row++;
        List<String[]> rows = new ArrayList<>();
        for (EnvSettingCategoryView cat : settings.categories()) {
            for (EnvSettingItemView item : cat.items()) {
                rows.add(new String[]{
                        cat.title(),
                        item.label(),
                        item.layer(),
                        item.guideValue(),
                        item.actualValue(),
                        item.source(),
                        matchLabel(item.status()),
                        item.note() != null ? item.note() : ""
                });
            }
        }
        writeTable(sheet, row, headerStyle,
                new String[]{"카테고리", "항목", "계층", "가이드", "현재", "출처", "판정", "비고"},
                rows);
        autosize(sheet, 8);
    }

    private List<String[]> plannerDerivationRows(CapacityPlannerResult p) {
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("JVM Heap 산출식", nullToDash(p.jvmHeapDerivationFormula())));
        rows.add(kv("WAS Threads 산출식", nullToDash(p.wasThreadsDerivationFormula())));
        rows.add(kv("DB Pool 산출식", nullToDash(p.hikariPoolDerivationFormula())));
        rows.add(kv("Tomcat maxThreads", nullToDash(p.tomcatMaxThreadsRange())));
        rows.add(kv("Busy Thread", nullToDash(p.tomcatBusyThreadFormula())));
        rows.add(kv("Hikari Pool/VM", String.valueOf(p.hikariPoolPerVm())));
        rows.add(kv("Hikari 범위", nullToDash(p.hikariPoolRangeLabel())));
        return rows;
    }

    private List<String[]> baselineRows(ProjectBaselineView b) {
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("프로젝트 ID", b.projectId()));
        rows.add(kv("프로젝트명", b.projectName()));
        rows.add(kv("환경", b.envCode()));
        rows.add(kv("하드웨어", b.hardwareProfile()));
        rows.add(kv("지점", b.branchCount() + " × " + b.usersPerBranch()));
        rows.add(kv("전체 사용자", String.valueOf(b.totalUsers())));
        rows.add(kv("실요청 사용자", b.actualRequestUsers() + " (" + b.actualRequestPeakPercent() + "%)"));
        rows.add(kv("TPS 피크", String.valueOf(b.peakTps())));
        rows.add(kv("AP VM", b.apVmSpec()));
        rows.add(kv("AP 대수", String.valueOf(b.apCount())));
        if (b.deploymentSummary() != null) {
            b.deploymentSummary().forEach((k, v) -> rows.add(kv(k, v)));
        }
        return rows;
    }

    private List<String[]> jvmRows(JvmSizingRecommendation j) {
        return kvList(
                kv("VM", j.vmProfileId() + " · " + j.vmCores() + "C / " + j.vmMemoryGb() + "GB"),
                kv("Heap 일반", j.heapGeneralMinGb() + "~" + j.heapGeneralMaxGb() + " GB"),
                kv("Heap SV", "≤" + j.heapSingleViewMaxGb() + " GB"),
                kv("GC", j.gcAlgorithm() + " · pause " + j.maxGcPauseMillis() + "ms"),
                kv("Thread Stack", j.threadStackSize()),
                kv("Metaspace", j.metaspaceSizeMb() + "~" + j.maxMetaspaceSizeMb() + " MB"),
                kv("비고", nullToDash(j.sizingNote()))
        );
    }

    private String[] layerGridHeaders() {
        return new String[]{"계층", "설정항목", "Property", "권장값", "현재값", "판정", "판정사유", "설정위치", "예시", "조치"};
    }

    private List<String[]> layerGridRows(List<LayerGridRow> grid) {
        if (grid == null) {
            return List.of();
        }
        List<String[]> rows = new ArrayList<>();
        for (LayerGridRow g : grid) {
            rows.add(new String[]{
                    g.layer(),
                    g.settingLabel(),
                    nullToDash(g.propertyKey()),
                    g.recommendedValue(),
                    g.currentValue(),
                    g.statusLabel() != null ? g.statusLabel() : g.status(),
                    nullToDash(g.reason()),
                    nullToDash(g.configLocation()),
                    nullToDash(g.settingExample()),
                    nullToDash(g.actionGuide())
            });
        }
        return rows;
    }

    private List<String[]> vmResultRows(List<CapacityVmResultRow> vmResults) {
        if (vmResults == null) {
            return List.of();
        }
        List<String[]> rows = new ArrayList<>();
        for (CapacityVmResultRow r : vmResults) {
            rows.add(new String[]{
                    String.valueOf(r.requestRatePercent()),
                    String.valueOf(r.timeoutSec()),
                    String.valueOf(r.realRequesters()),
                    String.valueOf(r.targetTps()),
                    String.valueOf(r.requiredTpmc()),
                    r.vmProfileLabel(),
                    String.valueOf(r.vmTpsAtBase()),
                    String.valueOf(r.requiredVmSingleCenter()),
                    String.valueOf(r.recommendedVmActiveActive()),
                    nullToDash(r.jvmHeapPerVm()),
                    nullToDash(r.jvmHeapSvPerVm()),
                    nullToDash(r.wasThreadsPerVm()),
                    String.valueOf(r.dbPoolPerVm()),
                    String.valueOf(r.dbPoolTotal()),
                    r.status(),
                    nullToDash(r.statusReason())
            });
        }
        return rows;
    }

    private List<String[]> assessmentRows(List<AssessmentResultItem> results) {
        if (results == null) {
            return List.of();
        }
        List<String[]> rows = new ArrayList<>();
        for (AssessmentResultItem r : results) {
            rows.add(new String[]{
                    r.ruleId(),
                    r.ruleType(),
                    r.severity(),
                    r.description(),
                    r.expectedValue(),
                    r.actualValue(),
                    assessLabel(r.status()),
                    nullToDash(r.configFile()),
                    nullToDash(r.propertyKey())
            });
        }
        return rows;
    }

    private List<String[]> timeoutRows(List<TimeoutMapNode> nodes) {
        return flowLikeRows(nodes == null ? List.of() : nodes.stream()
                .map(n -> new FlowLike(
                        n.order(), n.layer(), n.label(), n.displayValue(),
                        n.configValue(), n.propertyKey(), n.sourceFile(), n.guideValue(), n.note()
                ))
                .toList());
    }

    private List<String[]> flowRows(List<ConcurrentFlowMapNode> nodes) {
        return flowLikeRows(nodes == null ? List.of() : nodes.stream()
                .map(n -> new FlowLike(
                        n.order(), n.layer(), n.label(), n.displayValue(),
                        n.configValue(), n.propertyKey(), n.sourceFile(), n.guideValue(), n.note()
                ))
                .toList());
    }

    private List<String[]> flowLikeRows(List<FlowLike> nodes) {
        List<String[]> rows = new ArrayList<>();
        for (FlowLike n : nodes) {
            rows.add(new String[]{
                    String.valueOf(n.order()),
                    n.layer(),
                    n.label(),
                    n.displayValue(),
                    nullToDash(n.configValue()),
                    nullToDash(n.propertyKey()),
                    nullToDash(n.sourceFile()),
                    nullToDash(n.guideValue()),
                    nullToDash(n.note())
            });
        }
        return rows;
    }

    private record FlowLike(
            int order, String layer, String label, String displayValue,
            String configValue, String propertyKey, String sourceFile, String guideValue, String note
    ) {
    }

    private int writeTitle(Sheet sheet, int rowIndex, String title) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setFont(font);
        cell.setCellStyle(style);
        return rowIndex + 1;
    }

    private int writeKeyValues(Sheet sheet, int startRow, List<String[]> pairs) {
        int rowIndex = startRow;
        for (String[] pair : pairs) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(pair[0]);
            row.createCell(1).setCellValue(pair.length > 1 ? pair[1] : "");
        }
        return rowIndex;
    }

    private int writeTable(Sheet sheet, int startRow, CellStyle headerStyle, String[] headers, List<String[]> data) {
        Row header = sheet.createRow(startRow++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (String[] line : data) {
            Row row = sheet.createRow(startRow++);
            for (int i = 0; i < headers.length; i++) {
                row.createCell(i).setCellValue(i < line.length ? nullToDash(line[i]) : "");
            }
        }
        return startRow;
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            try {
                sheet.autoSizeColumn(i);
            } catch (Exception ignored) {
                sheet.setColumnWidth(i, 6000);
            }
        }
    }

    private static String[] kv(String key, String value) {
        return new String[]{key, value != null ? value : ""};
    }

    /** {@link List#of(String[])} 단일 인자 시 String[]가 펼쳐져 List&lt;String&gt;이 되는 함정 방지. */
    @SafeVarargs
    private static List<String[]> kvList(String[]... entries) {
        return Arrays.asList(entries);
    }

    private static String nullToDash(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private static String assessLabel(AssessmentStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case PASS -> "통과";
            case WARN -> "주의";
            case FAIL -> "실패";
            case INFO -> "참고";
            case EXCEPTION -> "오류";
        };
    }

    private static String matchLabel(SettingMatchStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case MATCH -> "일치";
            case WARN -> "주의";
            case INFO -> "참고";
            case NOT_APPLICABLE -> "해당없음";
        };
    }
}
