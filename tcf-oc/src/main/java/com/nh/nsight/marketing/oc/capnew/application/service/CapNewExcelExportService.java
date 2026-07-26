package com.nh.nsight.marketing.oc.capnew.application.service;

import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCompareCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewCompareRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewExcelExportRequest;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewVmCompareCDTO;
import com.nh.nsight.marketing.oc.capnew.application.dto.CapNewScenarioCDTO;
import com.nh.nsight.marketing.oc.capnew.support.CapNewBizException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CapNewExcelExportService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final CapNewWizardService wizardService;
    private final CapNewCompareService compareService;
    private final CapNewVmCompareService vmCompareService;

    public CapNewExcelExportService(
            CapNewWizardService wizardService,
            CapNewCompareService compareService,
            CapNewVmCompareService vmCompareService) {
        this.wizardService = wizardService;
        this.compareService = compareService;
        this.vmCompareService = vmCompareService;
    }

    public byte[] export(CapNewExcelExportRequest request) throws IOException {
        String type = request.getExportType() == null ? "SCENARIO" : request.getExportType().toUpperCase();
        if ("COMPARE".equals(type)) {
            return exportCompare(compareService.compare(buildCompareRequest(request)));
        }
        String scenarioId = request.getScenarioId();
        if (!StringUtils.hasText(scenarioId)) {
            throw new CapNewBizException("scenarioId가 필요합니다.");
        }
        return exportScenario(wizardService.getScenario(scenarioId));
    }

    public String filename(CapNewExcelExportRequest request) {
        String type = request.getExportType() == null ? "SCENARIO" : request.getExportType().toUpperCase();
        if ("COMPARE".equals(type)) {
            int count = request.getScenarioIds() == null ? 0 : request.getScenarioIds().size();
            return "NSIGHT_CAPNEW_비교_" + count + "건_" + FILE_TS.format(LocalDateTime.now()) + ".xlsx";
        }
        String id = request.getScenarioId() == null ? "scenario" : request.getScenarioId();
        return "NSIGHT_CAPNEW_" + id + "_" + FILE_TS.format(LocalDateTime.now()) + ".xlsx";
    }

    private CapNewCompareRequest buildCompareRequest(CapNewExcelExportRequest request) {
        if (request.getScenarioIds() == null || request.getScenarioIds().size() < 2) {
            throw new CapNewBizException("비교 Excel은 시나리오 2개 이상이 필요합니다.");
        }
        CapNewCompareRequest compareRequest = new CapNewCompareRequest();
        compareRequest.setScenarioIds(request.getScenarioIds());
        compareRequest.setBaselineScenarioId(request.getBaselineScenarioId());
        return compareRequest;
    }

    private byte[] exportScenario(CapNewScenarioCDTO scenario) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            writeSummarySheet(workbook, headerStyle, scenario);
            writeStepSheet(workbook, headerStyle, scenario);
            writeApDrSheet(workbook, headerStyle, scenario);
            writeWarPoolSheet(workbook, headerStyle, scenario);
            writeVmCompareSheet(workbook, headerStyle, scenario);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] exportCompare(CapNewCompareCDTO compare) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = headerStyle(workbook);
            writeCompareSummarySheet(workbook, headerStyle, compare);
            writeCompareMatrixSheet(workbook, headerStyle, compare);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeSummarySheet(Workbook wb, CellStyle headerStyle, CapNewScenarioCDTO scenario) {
        Sheet sheet = wb.createSheet("요약");
        Map<String, Object> step8 = stepMap(scenario, "step8");
        Map<String, Object> headline = map(step8.get("headline"));

        List<String[]> rows = new ArrayList<>();
        rows.add(kv("시나리오 ID", scenario.getScenarioId()));
        rows.add(kv("시나리오명", scenario.getScenarioName()));
        rows.add(kv("프로젝트", scenario.getProjectName()));
        rows.add(kv("대상 환경", scenario.getTargetEnv()));
        rows.add(kv("버전", scenario.getVersionNo()));
        rows.add(kv("상태", scenario.getStatus()));
        rows.add(kv("작성자", scenario.getAuthor()));
        rows.add(kv("기준일", scenario.getBaseDate()));
        rows.add(kv("운영 기준", str(headline.get("operatingBaseline"))));
        rows.add(kv("설계 TPS", str(headline.get("designPeakTps"))));
        rows.add(kv("VM Profile", str(headline.get("vmProfile"))));
        rows.add(kv("VM 보정 TPS", str(headline.get("vmAdjustedTps"))));
        rows.add(kv("배포 AP", str(headline.get("totalDeploymentAp"))));
        rows.add(kv("maxThreads", str(headline.get("maxThreads"))));
        rows.add(kv("Pool/VM", str(headline.get("poolPerVm"))));
        rows.add(kv("DB Session", str(headline.get("totalDbSessions"))));
        if (headline.get("warPoolTotalSessions") != null) {
            rows.add(kv("WAR Pool 합계", str(headline.get("warPoolTotalSessions"))
                    + " (" + str(headline.get("warPoolStatus")) + ")"));
        }
        rows.add(kv("종합 판정", str(headline.get("overallJudgment"))));
        rows.add(kv("결론", str(step8.get("conclusion"))));

        writeKvTable(sheet, headerStyle, "항목", "값", rows);
        autosize(sheet, 2);
    }

    private void writeStepSheet(Workbook wb, CellStyle headerStyle, CapNewScenarioCDTO scenario) {
        Sheet sheet = wb.createSheet("STEP별");
        List<String[]> rows = new ArrayList<>();
        for (int step = 1; step <= 8; step++) {
            Map<String, Object> data = stepMap(scenario, "step" + step);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() instanceof Map || entry.getValue() instanceof List) {
                    continue;
                }
                rows.add(new String[] {
                        "STEP " + step,
                        entry.getKey(),
                        str(entry.getValue())
                });
            }
        }
        writeTable(sheet, 0, headerStyle, new String[] { "단계", "항목", "값" }, rows);
        autosize(sheet, 3);
    }

    @SuppressWarnings("unchecked")
    private void writeApDrSheet(Workbook wb, CellStyle headerStyle, CapNewScenarioCDTO scenario) {
        Sheet sheet = wb.createSheet("AP·DR");
        Map<String, Object> step5 = stepMap(scenario, "step5");
        List<Map<String, Object>> results = step5.get("scenarioResults") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : List.of();

        List<String[]> rows = new ArrayList<>();
        for (Map<String, Object> row : results) {
            rows.add(new String[] {
                    str(row.get("code")),
                    str(row.get("label")),
                    str(row.get("targetTps")),
                    str(row.get("apPerCenterNormal")),
                    str(row.get("apPerCenterFailover")),
                    str(row.get("totalDeploymentAp")),
                    str(row.get("judgment"))
            });
        }
        writeTable(sheet, 0, headerStyle,
                new String[] { "코드", "라벨", "목표 TPS", "정상 센터 AP", "장애 센터 AP", "전체 AP", "판정" },
                rows);
        autosize(sheet, 7);
    }

    @SuppressWarnings("unchecked")
    private void writeWarPoolSheet(Workbook wb, CellStyle headerStyle, CapNewScenarioCDTO scenario) {
        Map<String, Object> step7 = stepMap(scenario, "step7");
        if (!Boolean.TRUE.equals(step7.get("warPoolEnabled"))
                && !"true".equalsIgnoreCase(str(step7.get("warPoolEnabled")))) {
            return;
        }
        List<Map<String, Object>> results = step7.get("warPoolResults") instanceof List<?> list
                ? (List<Map<String, Object>>) list
                : List.of();
        if (results.isEmpty()) {
            return;
        }

        Sheet sheet = wb.createSheet("WAR Pool");
        List<String[]> rows = new ArrayList<>();
        for (Map<String, Object> row : results) {
            rows.add(new String[] {
                    str(row.get("warCode")),
                    str(row.get("label")),
                    str(row.get("weightPercent")),
                    str(row.get("poolPerVm")),
                    str(row.get("deploymentAp")),
                    str(row.get("totalPool")),
                    str(row.get("judgment"))
            });
        }
        rows.add(new String[] {
                "합계", "", "", "", "",
                str(step7.get("warPoolTotalSessions")),
                str(step7.get("warPoolStatus"))
        });
        writeTable(sheet, 0, headerStyle,
                new String[] { "WAR", "라벨", "비중(%)", "Pool/VM", "AP 대수", "전체 Pool", "판정" },
                rows);

        int rowIdx = rows.size() + 3;
        Row msgRow = sheet.createRow(rowIdx++);
        msgRow.createCell(0).setCellValue(str(step7.get("warPoolStatusMessage")));
        Object recs = step7.get("warPoolRecommendations");
        if (recs instanceof List<?> list) {
            for (Object item : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue("· " + str(item));
            }
        }
        autosize(sheet, 7);
    }

    private void writeVmCompareSheet(Workbook wb, CellStyle headerStyle, CapNewScenarioCDTO scenario) {
        Map<String, Object> payload = scenario.getStepPayload();
        if (payload == null || !payload.containsKey("step4")) {
            return;
        }
        try {
            CapNewVmCompareCDTO compare = vmCompareService.compare(scenario.getScenarioId(), List.of());
            if (compare.getAlternatives() == null || compare.getAlternatives().isEmpty()) {
                return;
            }
            Sheet sheet = wb.createSheet("VM 대안");
            List<String[]> rows = new ArrayList<>();
            for (CapNewVmCompareCDTO.VmAlternativeRow row : compare.getAlternatives()) {
                rows.add(new String[] {
                        row.getVmProfileLabel(),
                        String.valueOf(row.getVmAdjustedTps()),
                        row.getRequiredApDisplay(),
                        String.valueOf(row.getTotalCores()),
                        row.getFailureBlastLabel(),
                        row.getJudgment(),
                        row.isSelected() ? "Y" : ""
                });
            }
            writeTable(sheet, 0, headerStyle,
                    new String[] { "VM Profile", "VM TPS", "필요 AP", "전체 Core", "장애범위", "판단", "현재" },
                    rows);
            int rowIdx = rows.size() + 3;
            Row rec = sheet.createRow(rowIdx++);
            rec.createCell(0).setCellValue(compare.getRecommendation());
            autosize(sheet, 7);
        } catch (CapNewBizException ex) {
            // VM 비교 불가 시 시트 생략
        }
    }

    private void writeCompareSummarySheet(Workbook wb, CellStyle headerStyle, CapNewCompareCDTO compare) {
        Sheet sheet = wb.createSheet("비교 요약");
        List<String[]> rows = new ArrayList<>();
        rows.add(kv("비교 건수", String.valueOf(compare.getScenarioIds().size())));
        rows.add(kv("기준 시나리오", compare.getBaselineScenarioId()));
        rows.add(kv("요약", compare.getSummary()));
        rows.add(kv("권장안", compare.getRecommendation()));
        writeKvTable(sheet, headerStyle, "항목", "내용", rows);

        int rowIdx = rows.size() + 3;
        Row title = sheet.createRow(rowIdx++);
        Cell cell = title.createCell(0);
        cell.setCellValue("차이 하이라이트");
        cell.setCellStyle(headerStyle);
        for (String highlight : compare.getDiffHighlights()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(highlight);
        }
        autosize(sheet, 2);
    }

    @SuppressWarnings("unchecked")
    private void writeCompareMatrixSheet(Workbook wb, CellStyle headerStyle, CapNewCompareCDTO compare) {
        Sheet sheet = wb.createSheet("지표 매트릭스");
        List<Map<String, Object>> columns = compare.getColumns() == null ? List.of() : compare.getColumns();
        List<String> headers = new ArrayList<>();
        headers.add("지표");
        headers.add("단위");
        for (Map<String, Object> col : columns) {
            headers.add(str(col.get("scenarioName")));
        }

        List<String[]> rows = new ArrayList<>();
        for (Map<String, Object> metric : compare.getMetricRows()) {
            List<Object> values = metric.get("values") instanceof List<?> list
                    ? (List<Object>) list
                    : List.of();
            String[] line = new String[headers.size()];
            line[0] = str(metric.get("label"));
            line[1] = str(metric.get("unit"));
            for (int i = 0; i < values.size(); i++) {
                line[i + 2] = str(values.get(i));
            }
            rows.add(line);
        }
        writeTable(sheet, 0, headerStyle, headers.toArray(String[]::new), rows);
        autosize(sheet, headers.size());
    }

    private void writeKvTable(Sheet sheet, CellStyle headerStyle, String h1, String h2, List<String[]> rows) {
        writeTable(sheet, 0, headerStyle, new String[] { h1, h2 }, rows);
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

    private String[] kv(String key, String value) {
        return new String[] { key, value };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> stepMap(CapNewScenarioCDTO scenario, String stepKey) {
        if (scenario.getStepPayload() == null) {
            return Map.of();
        }
        return map(scenario.getStepPayload().get(stepKey));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return new LinkedHashMap<>();
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
