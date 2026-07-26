package com.nh.nsight.aimethodology.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nh.nsight.aimethodology.model.BusinessModel;
import com.nh.nsight.aimethodology.model.FieldModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * DDL / OM / HTTP / 화면·거래 정의서 / 추적성 / Quality Gate / Manifest / Rule Test.
 */
public final class DocArtifactGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private DocArtifactGenerator() {
    }

    public static Map.Entry<String, String> generateRuleTest(BusinessModel model) {
        Map<String, String> layout = PackageLayouts.packageLayout(model);
        Map<String, String> names = PackageLayouts.classNames(model);
        String packageName = layout.get("rule");
        String className = names.get("rule") + "Test";
        String code = """
                package %s;

                import org.junit.jupiter.api.DisplayName;
                import org.junit.jupiter.api.Test;

                import static org.junit.jupiter.api.Assertions.assertNotNull;

                /**
                 * 자동생성 테스트 골격. 실제 필드별 정상/오류 시나리오를 보완한다.
                 */
                class %s {

                    private final %s rule = new %s();

                    @Test
                    @DisplayName("Rule 인스턴스 생성")
                    void createRule() {
                        assertNotNull(rule);
                    }
                }
                """.formatted(packageName, className, names.get("rule"), names.get("rule"));
        return Map.entry(PackageLayouts.testJavaPath(packageName, className), code);
    }

    public static Map.Entry<String, String> generateDdl(BusinessModel model) {
        String table = model.getTableName();
        List<FieldModel> fields = model.getFields();
        List<String> columnLines = new ArrayList<>();
        List<String> pkColumns = new ArrayList<>();
        for (FieldModel f : fields) {
            String nullable = f.isNullable() ? "" : " NOT NULL";
            String comment = PackageLayouts.blankToDefault(f.getLabel(), f.getName());
            columnLines.add("    " + f.getColumn() + " " + f.getDbType() + nullable + " /* " + comment + " */");
            if (f.isPk()) {
                pkColumns.add(f.getColumn());
            }
        }
        if (!pkColumns.isEmpty()) {
            columnLines.add("    CONSTRAINT PK_" + table + " PRIMARY KEY (" + String.join(", ", pkColumns) + ")");
        }
        String columnLinesText = String.join(",\n", columnLines);
        String tableComment = PackageLayouts.blankToDefault(
                PackageLayouts.blankToDefault(model.getTableComment(), model.getScreenName()), table);
        StringBuilder ddl = new StringBuilder();
        ddl.append("-- 자동생성 DDL 초안\n");
        ddl.append("-- 실제 스키마·테이블스페이스·파티션·인덱스 기준은 DA/DBA 검토 후 확정한다.\n");
        ddl.append("CREATE TABLE ").append(table).append(" (\n");
        ddl.append(columnLinesText).append("\n");
        ddl.append(");\n\n");
        ddl.append("COMMENT ON TABLE ").append(table).append(" IS '").append(tableComment).append("';\n");
        for (FieldModel f : fields) {
            ddl.append("COMMENT ON COLUMN ").append(table).append(".").append(f.getColumn())
                    .append(" IS '").append(PackageLayouts.blankToDefault(f.getLabel(), f.getName()))
                    .append("';\n");
        }
        return Map.entry("db/ddl/" + table + ".sql", ddl.toString());
    }

    public static Map.Entry<String, String> generateOmCatalog(BusinessModel model) {
        String audit = model.isAuditRequired() ? "Y" : "N";
        String serviceName = PackageLayouts.blankToDefault(model.getServiceName(), model.getEventName());
        String sql = """
                -- OM Service Catalog 등록 초안
                INSERT INTO OM_SERVICE_CATALOG (
                    SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE, SERVICE_NAME,
                    TIMEOUT_SECONDS, AUDIT_YN, USE_YN, SCREEN_ID, EVENT_ID
                ) VALUES (
                    '%s', '%s', '%s',
                    '%s', %d,
                    '%s', 'Y', '%s', '%s'
                );
                """.formatted(
                model.getServiceId(), model.getTransactionCode(), model.getBusinessCode(),
                serviceName, model.timeoutOrDefault(),
                audit, model.getScreenId(), model.getEventId()
        );
        String safeId = model.getServiceId().replace('.', '_');
        return Map.entry("db/om/" + safeId + "_OM_SERVICE_CATALOG.sql", sql);
    }

    public static Map.Entry<String, String> generateHttpRequest(BusinessModel model) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (FieldModel f : PackageLayouts.requestFields(model)) {
            Object sample = f.getSampleValue();
            if (sample == null || "".equals(sample)) {
                sample = defaultSample(f.getJavaType());
            }
            body.put(f.getName(), sample);
        }
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("businessCode", model.getBusinessCode());
        header.put("serviceId", model.getServiceId());
        header.put("transactionCode", model.getTransactionCode());
        header.put("screenId", model.getScreenId());
        header.put("channelId", "WEBTOP");
        header.put("userId", "TEST_USER");
        header.put("branchId", "000001");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("header", header);
        request.put("body", body);

        String contextPath = PackageLayouts.blankToDefault(model.getContextPath(), "/");
        String title = PackageLayouts.blankToDefault(model.getServiceName(), model.getServiceId());
        String json = MAPPER.writeValueAsString(request);
        String text = "### " + title + "\n"
                + "POST http://localhost:8080" + contextPath + "/online\n"
                + "Content-Type: application/json\n"
                + "Authorization: Bearer {accessToken}\n\n"
                + json + "\n";
        return Map.entry("requests/" + model.getServiceId().replace('.', '_') + ".http", text);
    }

    public static Map.Entry<String, String> generateScreenDefinition(BusinessModel model) {
        List<String> rows = new ArrayList<>();
        rows.add("| Java 필드 | 화면/업무명 | DB 컬럼 | Java 타입 | DB 타입 | 요청 | 조건 | 응답 | 필수 | 민감 |");
        rows.add("|---|---|---|---|---|---:|---:|---:|---:|---:|");
        for (FieldModel f : model.getFields()) {
            rows.add("| `" + f.getName() + "` | " + PackageLayouts.nullToEmpty(f.getLabel())
                    + " | `" + f.getColumn() + "` | `" + f.getJavaType() + "` | `" + f.getDbType() + "` | "
                    + (f.isRequest() ? "Y" : "N") + " | " + (f.isCondition() ? "Y" : "N") + " | "
                    + (f.isResponse() ? "Y" : "N") + " | " + (f.isNullable() ? "N" : "Y") + " | "
                    + (f.isSensitive() ? "Y" : "N") + " |");
        }
        String eventSuffix = model.getEventId() == null ? ""
                : model.getEventId().substring(model.getEventId().lastIndexOf('-') + 1);
        String text = """
                # 화면·이벤트 정의서

                ## 1. 기본정보

                | 항목 | 값 |
                |---|---|
                | 화면 ID | `%s` |
                | 화면명 | %s |
                | 이벤트 ID | `%s` |
                | 이벤트명 | %s |
                | UI 객체 ID | `%s` |
                | 호출 ServiceId | `%s` |
                | 거래코드 | `%s` |
                | 성공 처리 | %s |
                | 실패 처리 | %s |
                | 중복 요청 방지 | %s |

                ## 2. 필드 정의

                %s

                ## 3. 처리 흐름

                ```text
                화면 %s
                  → 이벤트 %s
                  → StandardRequest.header.serviceId = %s
                  → OnlineTransactionController
                  → TCF / STF / TransactionDispatcher
                  → 도메인 Handler / Facade / Service / Rule / DAO / Mapper
                  → %s
                  → ETF 표준 응답
                ```
                """.formatted(
                model.getScreenId(),
                model.getScreenName(),
                model.getEventId(),
                model.getEventName(),
                PackageLayouts.nullToEmpty(model.getUiObjectId()),
                model.getServiceId(),
                model.getTransactionCode(),
                PackageLayouts.blankToDefault(model.getSuccessAction(), "처리 결과 표시 및 필요 시 재조회"),
                PackageLayouts.blankToDefault(model.getFailureAction(), "표준 오류 메시지 표시, 입력 상태 유지"),
                model.isIdempotencyRequired() ? "적용" : "해당 없음/검토",
                String.join("\n", rows),
                model.getScreenId(),
                model.getEventId(),
                model.getServiceId(),
                model.getTableName()
        );
        return Map.entry("docs/screens/" + model.getScreenId() + "_" + eventSuffix + ".md", text);
    }

    public static Map.Entry<String, String> generateTransactionDefinition(BusinessModel model) {
        Map<String, String> names = PackageLayouts.classNames(model);
        Map<String, String> layout = PackageLayouts.packageLayout(model);
        String text = """
                # 거래설계서 — %s

                ## 거래 식별

                | 항목 | 값 |
                |---|---|
                | 업무코드 | `%s` |
                | 도메인 | `%s` |
                | ServiceId | `%s` |
                | 거래코드 | `%s` |
                | 처리유형 | `%s` |
                | Timeout | `%d초` |
                | 감사대상 | `%s` |
                | 권한코드 | `%s` |

                ## 실행 프로그램

                | 계층 | 클래스/메서드 |
                |---|---|
                | Controller | `OnlineTransactionController.online()` |
                | Handler | `%s.%s` |
                | Facade | `%s.%s.%s()` |
                | Service | `%s.%s.%s()` |
                | Rule | `%s.%s` |
                | DAO | `%s.%s.%s()` |
                | Mapper | `%s.%s.%s` |
                | Table | `%s` |

                ## 정상 흐름

                ```text
                화면 이벤트
                → ServiceId 포함 표준전문
                → STF: Header·인증·권한·거래통제·Timeout 검증
                → Handler: ServiceId 분기
                → Facade: Transaction 경계
                → Service: 유스케이스 조립
                → Rule: 필수값·업무규칙 검증
                → DAO/Mapper: SQL 실행
                → ETF: 표준 성공 응답 및 거래로그 종료
                ```

                ## 오류·Timeout 흐름

                - 필수값·업무규칙 위반: `BusinessException`으로 업무 오류 표준화
                - Mapper/DB 오류: 시스템 오류로 변환하고 Rollback
                - Timeout: TCF 전체 Timeout과 MyBatis Statement Timeout을 모두 적용
                - 미등록 ServiceId: Dispatcher/Handler에서 실행 차단
                - 변경 거래 감사로그: 사용자·지점·화면·ServiceId·변경대상·결과를 기록
                """.formatted(
                model.getServiceId(),
                model.getBusinessCode(),
                model.getDomainCode(),
                model.getServiceId(),
                model.getTransactionCode(),
                model.getOperation(),
                model.timeoutOrDefault(),
                model.isAuditRequired() ? "Y" : "N",
                PackageLayouts.nullToEmpty(model.getPermissionCode()),
                layout.get("handler"), names.get("handler"),
                layout.get("facade"), names.get("facade"), model.getMethodName(),
                layout.get("service"), names.get("service"), model.getMethodName(),
                layout.get("rule"), names.get("rule"),
                layout.get("dao"), names.get("dao"), model.getMethodName(),
                layout.get("mapper"), names.get("mapper"), model.getMethodName(),
                model.getTableName()
        );
        return Map.entry("docs/transactions/" + model.getServiceId().replace('.', '_') + ".md", text);
    }

    public static Map.Entry<String, String> generateTraceabilityCsv(List<BusinessModel> models) {
        StringBuilder output = new StringBuilder();
        output.append(csvRow(List.of(
                "화면ID", "화면명", "이벤트ID", "이벤트명", "ServiceId", "거래코드", "Handler", "Facade",
                "Service", "Rule", "DAO", "Mapper", "SQL_ID", "Table", "Operation", "Timeout", "Audit"
        )));
        for (BusinessModel model : models) {
            Map<String, String> n = PackageLayouts.classNames(model);
            output.append(csvRow(List.of(
                    str(model.getScreenId()), str(model.getScreenName()),
                    str(model.getEventId()), str(model.getEventName()),
                    str(model.getServiceId()), str(model.getTransactionCode()),
                    n.get("handler"), n.get("facade"), n.get("service"), n.get("rule"),
                    n.get("dao"), n.get("mapper"),
                    str(model.getMethodName()), str(model.getTableName()),
                    str(model.getOperation()), String.valueOf(model.timeoutOrDefault()),
                    model.isAuditRequired() ? "Y" : "N"
            )));
        }
        return Map.entry("docs/TRACEABILITY_MATRIX.csv", output.toString());
    }

    public static Map.Entry<String, String> generateQualityGate(List<BusinessModel> models) {
        String text = """
                # 자동검증 및 품질 Gate

                ## 생성 전 Gate

                - [ ] 화면 ID·이벤트 ID·ServiceId·거래코드 형식 검증
                - [ ] ServiceId·거래코드 중복 검증
                - [ ] 업무코드·도메인·패키지 정합성 검증
                - [ ] 조회/변경 유형과 거래코드 유형 정합성 검증
                - [ ] 필수 요청·조건·응답 필드 검증
                - [ ] 변경 거래 감사대상 검증
                - [ ] 민감정보 마스킹 규칙 검증

                ## 코드 Gate

                - [ ] Handler는 ServiceId 분기와 Facade 호출만 수행
                - [ ] Transaction 경계는 Facade에 위치
                - [ ] Service는 Mapper를 직접 호출하지 않음
                - [ ] Rule은 DB/외부 시스템을 호출하지 않음
                - [ ] DAO Method와 Mapper Statement ID는 1:1
                - [ ] Mapper namespace와 Java Interface FQCN 일치
                - [ ] SQL에 안전한 WHERE 조건 존재
                - [ ] TCF Timeout ≥ DB/외부 호출 Timeout 합계 검토

                ## CI/CD Gate 예시

                ```text
                1. model-validate
                2. code-generate
                3. compileJava
                4. unitTest
                5. ArchUnit 계층검사
                6. ServiceId 중복검사
                7. Mapper namespace/SQL ID 검사
                8. DDL/SQL 정적검사
                9. OM Catalog 등록정보 비교
                10. 산출물 추적성 누락검사
                ```
                """;
        return Map.entry("QUALITY_GATE.md", text);
    }

    public static Map.Entry<String, String> generateManifest(List<BusinessModel> models,
                                                            Map<String, String> artifacts) throws Exception {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("generator", "NSIGHT Model Studio");
        manifest.put("version", "0.1.0");
        manifest.put("modelCount", models.size());
        manifest.put("serviceIds", models.stream().map(BusinessModel::getServiceId).collect(Collectors.toList()));
        Set<String> businessCodes = models.stream()
                .map(BusinessModel::getBusinessCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        manifest.put("businessCodes", new ArrayList<>(businessCodes));
        manifest.put("files", artifacts.keySet().stream().sorted().collect(Collectors.toList()));
        return Map.entry("manifest.json", MAPPER.writeValueAsString(manifest));
    }

    private static Object defaultSample(String javaType) {
        return switch (javaType == null ? "String" : javaType) {
            case "Integer", "Long" -> 1;
            case "BigDecimal" -> 1000.0;
            case "Boolean" -> true;
            case "LocalDate" -> "2026-07-25";
            case "LocalDateTime" -> "2026-07-25T09:00:00";
            default -> "SAMPLE";
        };
    }

    private static String csvRow(List<String> values) {
        return values.stream().map(DocArtifactGenerator::csvEscape).collect(Collectors.joining(",")) + "\n";
    }

    private static String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String str(String value) {
        return value == null ? "" : value;
    }
}
