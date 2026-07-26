package com.nh.nsight.marketing.om.support;

import com.nh.nsight.tcf.util.DateTimeUtil;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class OmDatabaseMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(OmDatabaseMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public OmDatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE OM_USER ADD COLUMN IF NOT EXISTS PASSWORD_HASH VARCHAR(200)");
        ensureUdFileMetaTable();
        jdbcTemplate.update("""
                MERGE INTO OM_BATCH_JOB (JOB_ID, JOB_NAME, BUSINESS_CODE, CRON_EXPR, USE_YN, DESCRIPTION) KEY (JOB_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "BAT-OM-002", "OM 세션 정리", "OM", "*/10 * * * * *", "Y",
                "SPRING_SESSION 만료 세션 10초 주기 정리");
        jdbcTemplate.update("""
                MERGE INTO OM_BATCH_JOB (JOB_ID, JOB_NAME, BUSINESS_CODE, CRON_EXPR, USE_YN, DESCRIPTION) KEY (JOB_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "BAT-BATCH-001", "대시보드 AP 상태 수집", "BATCH", "0 */5 * * * *", "Y",
                "Actuator AP CPU/Heap/Thread → OM_AP_STATUS (tcf-batch)");
        jdbcTemplate.update("""
                MERGE INTO OM_BATCH_JOB (JOB_ID, JOB_NAME, BUSINESS_CODE, CRON_EXPR, USE_YN, DESCRIPTION) KEY (JOB_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "BAT-BATCH-002", "대시보드 DB 상태 수집", "BATCH", "30 */5 * * * *", "Y",
                "Actuator/JDBC DB Pool → OM_DB_STATUS (tcf-batch)");
        jdbcTemplate.update("""
                MERGE INTO OM_BATCH_JOB (JOB_ID, JOB_NAME, BUSINESS_CODE, CRON_EXPR, USE_YN, DESCRIPTION) KEY (JOB_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "BAT-BATCH-003", "대시보드 세션 현황 수집", "BATCH", "45 */5 * * * *", "Y",
                "Spring Session/Actuator → OM_SESSION_STATUS (tcf-batch)");
        jdbcTemplate.update("""
                MERGE INTO OM_BATCH_JOB (JOB_ID, JOB_NAME, BUSINESS_CODE, CRON_EXPR, USE_YN, DESCRIPTION) KEY (JOB_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                "BAT-BATCH-004", "대시보드 배포 현황 수집", "BATCH", "55 */5 * * * *", "Y",
                "Actuator/HTTP 배포 상태 → OM_DEPLOY_STATUS (tcf-batch)");
        ensureSessionStatusTable();
        ensureDeployTables();
        removeLegacyApSeedApStatus();
        removeEmptySessionStatus();
        removeDuplicateOmSessionStatus();
        removeEmptyDbStatus();
        removeEmptyDeployStatus();
        jdbcTemplate.update("""
                MERGE INTO OM_MENU (MENU_ID, MENU_NAME, MENU_URL, PARENT_MENU_ID, SORT_ORDER, USE_YN) KEY (MENU_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """, "OM_FIL", "파일 관리", "/om/admin/file-management.html", "OM_GRP_SYS", 16, "Y");
        ensureRuntimeDiagnosticsMenus();
        jdbcTemplate.update("""
                MERGE INTO OM_MENU (MENU_ID, MENU_NAME, MENU_URL, PARENT_MENU_ID, SORT_ORDER, USE_YN) KEY (MENU_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """, "OM_DPL", "배포 관리", "/om/admin/deploy.html", "OM_GRP_SYS", 16, "Y");
        jdbcTemplate.update("""
                MERGE INTO OM_MENU (MENU_ID, MENU_NAME, MENU_URL, PARENT_MENU_ID, SORT_ORDER, USE_YN) KEY (MENU_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """, "OM_TXC", "거래통제 관리", "/om/admin/transaction-control.html", "OM_GRP_OPS", 3, "Y");
        jdbcTemplate.update("""
                MERGE INTO OM_MENU (MENU_ID, MENU_NAME, MENU_URL, PARENT_MENU_ID, SORT_ORDER, USE_YN) KEY (MENU_ID)
                VALUES (?, ?, ?, ?, ?, ?)
                """, "OM_MSG", "전문구조 관리", "/om/admin/message-structure.html", "OM_GRP_OPS", 5, "Y");
        ServiceCatalogSeedData.mergeAll(jdbcTemplate);
        seedAuthCodeCommonCodes();
        seedCacheNameCommonCodes();
        seedBusinessAuthCodes();
        seedChannelAndBranchCommonCodes();
        repairCorruptedUtf8SeedData();
        ensureMenuHierarchy();
        normalizeMenuNullColumns();
        FunctionAuthSeedData.ensureTable(jdbcTemplate);
        FunctionAuthSeedData.mergeAll(jdbcTemplate);
        ensureTransactionControlTable();
        TransactionControlSeedData.mergeAll(jdbcTemplate);
        TimeoutPolicySeedData.mergeAll(jdbcTemplate);
        repairRuntimeInquiryCatalogAndTimeout();
        ensureMessageStructTables();
        MessageStructureSeedData.mergeAll(jdbcTemplate);
        removeLegacyFunctionAuthIds();
        log.debug("OM schema migration applied.");
    }

    /** CAT-105 중복으로 카탈로그에서 빠졌을 수 있는 런타임 진단 거래·Timeout 정책을 복구한다. */
    private void repairRuntimeInquiryCatalogAndTimeout() {
        jdbcTemplate.update("""
                MERGE INTO OM_SERVICE_CATALOG (CATALOG_ID, BUSINESS_CODE, SERVICE_ID, TRANSACTION_CODE,
                                               PROCESSING_TYPE, HANDLER_CLASS, AUTH_CODE, AUDIT_YN,
                                               TIMEOUT_SEC, USE_YN, DESCRIPTION)
                KEY (CATALOG_ID)
                VALUES ('CAT-111', 'OM', 'OM.Runtime.inquiry', 'OM-RTM-0001', 'INQUIRY',
                        'OmRuntimeHandler', 'ROLE_OM_RTM', 'N', 60, 'Y', '런타임 진단')
                """);
        jdbcTemplate.update("""
                MERGE INTO TCF_SERVICE_TIMEOUT_POLICY (
                    SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE, SERVICE_NAME,
                    ONLINE_TIMEOUT_SEC, TX_TIMEOUT_SEC, DB_QUERY_TIMEOUT_SEC,
                    EXTERNAL_CONNECT_TIMEOUT_MS, EXTERNAL_READ_TIMEOUT_MS,
                    TIMEOUT_ACTION, USE_YN, DESCRIPTION, CREATED_AT
                )
                KEY (SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE)
                VALUES ('OM.Runtime.inquiry', 'OM-RTM-0001', 'OM', '런타임 진단',
                        60, 5, 3, 3000, 5000, 'FAIL', 'Y', '런타임 진단', CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                UPDATE TCF_SERVICE_TIMEOUT_POLICY
                   SET ONLINE_TIMEOUT_SEC = 60,
                       SERVICE_NAME = '런타임 진단',
                       DESCRIPTION = '런타임 진단'
                 WHERE SERVICE_ID = 'OM.Runtime.inquiry'
                   AND TRANSACTION_CODE = 'OM-RTM-0001'
                   AND BUSINESS_CODE = 'OM'
                   AND ONLINE_TIMEOUT_SEC < 60
                """);
    }

    /** data.sql 이 MS949 등으로 적재된 H2 파일의 한글 깨짐을 Java UTF-16 문자열로 복구한다. */
    private void repairCorruptedUtf8SeedData() {
        Map<String, String> catalogDescriptions = Map.ofEntries(
                Map.entry("CAT-001", "SV 샘플 조회"),
                Map.entry("CAT-002", "고객요약 조회"),
                Map.entry("CAT-003", "운영 대시보드"),
                Map.entry("CAT-004", "거래로그 조회"),
                Map.entry("CAT-005", "ServiceId 카탈로그"),
                Map.entry("CAT-006", "사용자 조회"),
                Map.entry("CAT-007", "메뉴 조회"),
                Map.entry("CAT-008", "권한그룹 조회"),
                Map.entry("CAT-009", "감사로그 조회"),
                Map.entry("CAT-010", "오류코드 조회"),
                Map.entry("CAT-011", "배치/스케줄 조회"),
                Map.entry("CAT-012", "Health Check 조회"),
                Map.entry("CAT-013", "환경설정 조회"),
                Map.entry("CAT-014", "파일 다운로드 이력"),
                Map.entry("CAT-015", "공통코드 목록 조회"),
                Map.entry("CAT-016", "공통코드 등록"),
                Map.entry("CAT-017", "공통코드 단건 조회"),
                Map.entry("CAT-018", "공통코드 수정"),
                Map.entry("CAT-019", "공통코드 삭제"),
                Map.entry("CAT-020", "오류코드 등록"),
                Map.entry("CAT-041", "오류코드 상세"),
                Map.entry("CAT-042", "오류코드 수정"),
                Map.entry("CAT-043", "오류코드 삭제"),
                Map.entry("CAT-021", "배치 재실행"),
                Map.entry("CAT-022", "기능권한 조회"),
                Map.entry("CAT-023", "데이터권한 조회"),
                Map.entry("CAT-024", "권한이력 조회"),
                Map.entry("CAT-100", "거래통제 수정"),
                Map.entry("CAT-105", "전문구조 조회"),
                Map.entry("CAT-111", "런타임 진단"),
                Map.entry("CAT-112", "권한이력 전체 삭제"),
                Map.entry("CAT-101", "Timeout 정책 조회"),
                Map.entry("CAT-102", "Timeout 정책 등록"),
                Map.entry("CAT-103", "Timeout 정책 수정"),
                Map.entry("CAT-104", "Timeout 정책 삭제"),
                Map.entry("CAT-025", "Cache 조회"),
                Map.entry("CAT-026", "Cache 삭제"),
                Map.entry("CAT-027", "OM 로그인"),
                Map.entry("CAT-028", "OM 로그아웃"),
                Map.entry("CAT-029", "OM 세션 조회"),
                Map.entry("CAT-030", "세션 목록 조회"),
                Map.entry("CAT-031", "세션 강제 종료"),
                Map.entry("CAT-032", "사용자 상세"),
                Map.entry("CAT-033", "사용자 등록"),
                Map.entry("CAT-034", "사용자 수정"),
                Map.entry("CAT-035", "사용자 삭제"),
                Map.entry("CAT-036", "거래로그 전체 삭제"),
                Map.entry("CAT-037", "ServiceId 등록"),
                Map.entry("CAT-038", "ServiceId 상세"),
                Map.entry("CAT-039", "ServiceId 수정"),
                Map.entry("CAT-040", "ServiceId 삭제"),
                Map.entry("CAT-044", "배치 실행이력 전체 삭제"),
                Map.entry("CAT-045", "OM 샘플 조회"),
                Map.entry("CAT-046", "CC 샘플 조회"),
                Map.entry("CAT-047", "IC 샘플 조회"),
                Map.entry("CAT-048", "PC 샘플 조회"),
                Map.entry("CAT-049", "BC 샘플 조회"),
                Map.entry("CAT-050", "MS 샘플 조회"),
                Map.entry("CAT-051", "PD 샘플 조회"),
                Map.entry("CAT-052", "CM 샘플 조회"),
                Map.entry("CAT-053", "EB 샘플 조회"),
                Map.entry("CAT-113", "EB 시스템 거래 현황(19410)"),
                Map.entry("CAT-054", "EP 샘플 조회"),
                Map.entry("CAT-055", "BP 샘플 조회"),
                Map.entry("CAT-056", "BD 샘플 조회"),
                Map.entry("CAT-057", "SS 샘플 조회"),
                Map.entry("CAT-058", "CS 샘플 조회"),
                Map.entry("CAT-059", "CT 샘플 조회"),
                Map.entry("CAT-060", "MG 샘플 조회"),
                Map.entry("CAT-061", "고객 상세 조회"),
                Map.entry("CAT-062", "고객 등록"),
                Map.entry("CAT-063", "캠페인 목록 조회"),
                Map.entry("CAT-064", "메시지 발송"),
                Map.entry("CAT-065", "파일 목록 조회"),
                Map.entry("CAT-066", "파일 업로드"),
                Map.entry("CAT-067", "파일 다운로드"),
                Map.entry("CAT-068", "파일 상세 조회"),
                Map.entry("CAT-069", "파일 메타 수정"),
                Map.entry("CAT-070", "파일 삭제"),
                Map.entry("CAT-071", "거래 I/O 목록 (레거시)"),
                Map.entry("CAT-072", "거래 I/O 상세 (레거시)"),
                Map.entry("CAT-097", "거래통제 조회"),
                Map.entry("CAT-098", "거래통제 등록"),
                Map.entry("CAT-099", "거래통제 삭제")
        );
        catalogDescriptions.forEach((catalogId, description) ->
                jdbcTemplate.update(
                        "UPDATE OM_SERVICE_CATALOG SET DESCRIPTION = ? WHERE CATALOG_ID = ?",
                        description, catalogId));

        String ts = DateTimeUtil.nowKst();
        mergeCommonCode("BUSINESS_CODE", "SV", "Single View", 1, "통합고객 조회", ts);
        mergeCommonCode("BUSINESS_CODE", "CM", "Campaign", 2, "캠페인", ts);
        mergeCommonCode("BUSINESS_CODE", "OM", "Operation Mgmt", 3, "운영관리", ts);
        mergeCommonCode("BUSINESS_CODE", "BC", "Business Customer", 4, "기업고객", ts);
        mergeCommonCode("BUSINESS_CODE", "BD", "Business Data", 5, "마케팅 데이터", ts);
        mergeCommonCode("BUSINESS_CODE", "CC", "Contact Center", 6, "컨택센터", ts);
        mergeCommonCode("BUSINESS_CODE", "MG", "Message", 7, "메시지 발송", ts);
        mergeCommonCode("BUSINESS_CODE", "ET", "Enterprise", 8, "엔터프라이즈(미사용)", ts);
        mergeCommonCode("BUSINESS_CODE", "IC", "Integration Customer", 9, "통합고객 연계", ts);
        mergeCommonCode("BUSINESS_CODE", "PC", "Private Customer", 10, "개인고객", ts);
        mergeCommonCode("BUSINESS_CODE", "MS", "Mini Single View", 11, "미니 통합고객", ts);
        mergeCommonCode("BUSINESS_CODE", "PD", "Product", 12, "상품", ts);
        mergeCommonCode("BUSINESS_CODE", "EB", "EBM", 13, "EBM", ts);
        mergeCommonCode("BUSINESS_CODE", "EP", "Event Processing", 14, "이벤트 처리", ts);
        mergeCommonCode("BUSINESS_CODE", "BP", "Behavior Processing", 15, "행동 처리", ts);
        mergeCommonCode("BUSINESS_CODE", "SS", "Sales Support", 16, "영업 지원", ts);
        mergeCommonCode("BUSINESS_CODE", "CS", "Common Service", 17, "공통 서비스", ts);
        mergeCommonCode("BUSINESS_CODE", "CT", "Contents", 18, "콘텐츠", ts);
        mergeCommonCode("BUSINESS_CODE", "UD", "UpDownload", 19, "파일 업·다운로드", ts);
    }

    private void seedBusinessAuthCodes() {
        String ts = DateTimeUtil.nowKst();
        mergeCommonCode("AUTH_CODE", "ROLE_CC_INQ", "CC 조회", 14, "Contact Center 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_IC_INQ", "IC 조회", 15, "Integration Customer 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_PC_INQ", "PC 조회", 16, "Private Customer 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_BC_INQ", "BC 조회", 17, "Business Customer 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_MS_INQ", "MS 조회", 18, "Mini Single View 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_PD_INQ", "PD 조회", 19, "Product 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_CM_INQ", "CM 조회", 20, "Campaign 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_EB_INQ", "EB 조회", 21, "EBM 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_EP_INQ", "EP 조회", 22, "Event Processing 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_BP_INQ", "BP 조회", 23, "Behavior Processing 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_BD_INQ", "BD 조회", 24, "Behavior Data 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_SS_INQ", "SS 조회", 25, "Sales Support 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_CS_INQ", "CS 조회", 26, "Common Service 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_CT_INQ", "CT 조회", 27, "Contents 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_MG_INQ", "MG 조회", 28, "Message 조회/발송", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_UD_FIL", "UD 파일", 29, "파일 업·다운로드", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_ET_INQ", "ET 조회", 30, "Enterprise (레거시)", ts);
    }

    private void seedAuthCodeCommonCodes() {
        String ts = DateTimeUtil.nowKst();
        mergeCommonCode("AUTH_CODE", "ROLE_OM_AUTH", "OM 인증/권한", 1, "사용자·메뉴·세션·권한", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_SVC", "ServiceId 관리", 2, "서비스 카탈로그", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_MSG", "전문구조", 32, "표준 전문 Header/Body/Result 필드 정의", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_DSH", "운영 대시보드", 3, "대시보드 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_TXL", "거래로그", 4, "거래로그 조회/삭제", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_TXC", "거래통제", 31, "거래통제 허용 목록", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_AUD", "감사로그", 5, "감사로그 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_ERR", "오류코드", 6, "오류코드 관리", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_BAT", "배치", 7, "배치/스케줄", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_HLT", "Health Check", 8, "AP/DB 헬스", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_RTM", "런타임 진단", 33, "Thread/JVM/DB Pool 진단", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_CFG", "환경설정", 9, "시스템 설정 조회", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_FIL", "파일", 10, "파일 다운로드 이력", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_CDC", "공통코드", 11, "공통코드 CRUD", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_OM_CACHE", "Cache", 12, "캐시 조회/삭제", ts);
        mergeCommonCode("AUTH_CODE", "ROLE_SV_INQ", "SV 조회", 13, "Single View 조회", ts);
    }

    private void seedCacheNameCommonCodes() {
        String ts = DateTimeUtil.nowKst();
        mergeCommonCode("CACHE_NAME", "commonCode", "공통코드", 1, "공통코드 Cache (키=코드그룹)", ts);
        mergeCommonCode("CACHE_NAME", "serviceCatalog", "ServiceId 카탈로그", 2, "ServiceId 카탈로그 Cache", ts);
        mergeCommonCode("CACHE_NAME", "sessionRegion", "세션 영역", 3, "세션 Cache", ts);
    }

    /** 거래통제 화면 콤보 — CHANNEL_CODE, BRANCH_CODE */
    private void seedChannelAndBranchCommonCodes() {
        String ts = DateTimeUtil.nowKst();
        mergeCommonCode("CHANNEL_CODE", "WEBTOP", "웹탑", 1, "웹 채널", ts);
        mergeCommonCode("CHANNEL_CODE", "MOBILE", "모바일", 2, "모바일 채널", ts);
        mergeCommonCode("CHANNEL_CODE", "BRANCH", "창구", 3, "영업점 창구", ts);
        mergeCommonCode("CHANNEL_CODE", "CALL", "콜센터", 4, "전화 상담", ts);
        mergeCommonCode("BRANCH_CODE", "000001", "본부", 1, "본부 영업점", ts);
        mergeCommonCode("BRANCH_CODE", "001234", "강남지점", 2, "강남 영업점", ts);
    }

    private void mergeCommonCode(String codeGroup, String code, String codeName, int sortOrder,
                                 String description, String timestamp) {
        jdbcTemplate.update("""
                MERGE INTO OM_COMMON_CODE (CODE_GROUP, CODE, CODE_NAME, SORT_ORDER, USE_YN,
                                           DESCRIPTION, CREATED_AT, UPDATED_AT)
                KEY (CODE_GROUP, CODE)
                VALUES (?, ?, ?, ?, 'Y', ?, ?, ?)
                """,
                codeGroup, code, codeName, sortOrder, description, timestamp, timestamp);
    }

    private void ensureSessionStatusTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_SESSION_STATUS (
                    SCOPE_ID VARCHAR(50) NOT NULL,
                    SCOPE_NAME VARCHAR(100),
                    ACTIVE_COUNT INT,
                    EXPIRED_COUNT INT,
                    TOTAL_COUNT INT,
                    UNIQUE_USER_COUNT INT,
                    HEALTH_STATUS VARCHAR(20),
                    CHECKED_AT VARCHAR(40),
                    PRIMARY KEY (SCOPE_ID)
                )
                """);
    }

    private void ensureDeployTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_DEPLOY_REQUEST (
                    DEPLOY_REQUEST_ID VARCHAR(40) NOT NULL,
                    REQUEST_TYPE VARCHAR(20) NOT NULL,
                    ENV_CODE VARCHAR(20),
                    BUSINESS_CODE VARCHAR(20),
                    MODULE_NAME VARCHAR(100),
                    BRANCH_NAME VARCHAR(100),
                    COMMIT_ID VARCHAR(100),
                    GRADLE_TASK VARCHAR(200),
                    ARTIFACT_NAME VARCHAR(200),
                    STATUS VARCHAR(30) NOT NULL,
                    REQUEST_USER_ID VARCHAR(50),
                    APPROVE_USER_ID VARCHAR(50),
                    REQUEST_TIME VARCHAR(40) NOT NULL,
                    APPROVE_TIME VARCHAR(40),
                    START_TIME VARCHAR(40),
                    END_TIME VARCHAR(40),
                    ERROR_MESSAGE CLOB,
                    PRIMARY KEY (DEPLOY_REQUEST_ID)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_DEPLOY_HISTORY (
                    DEPLOY_HISTORY_ID VARCHAR(40) NOT NULL,
                    DEPLOY_REQUEST_ID VARCHAR(40),
                    ENV_CODE VARCHAR(20),
                    TARGET_SERVER VARCHAR(100),
                    CONTEXT_PATH VARCHAR(50),
                    BEFORE_VERSION VARCHAR(100),
                    AFTER_VERSION VARCHAR(100),
                    RESULT_CODE VARCHAR(20),
                    RESULT_MESSAGE VARCHAR(1000),
                    HEALTH_CHECK_URL VARCHAR(500),
                    HEALTH_CHECK_RESULT VARCHAR(20),
                    CREATED_TIME VARCHAR(40) NOT NULL,
                    PRIMARY KEY (DEPLOY_HISTORY_ID)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_OM_DEPLOY_REQ_STATUS ON OM_DEPLOY_REQUEST (STATUS, REQUEST_TIME DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_OM_DEPLOY_HIST_REQ ON OM_DEPLOY_HISTORY (DEPLOY_REQUEST_ID, CREATED_TIME DESC)");
    }

    private void ensureUdFileMetaTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS UD_FILE_META (
                    FILE_ID VARCHAR(36) NOT NULL,
                    ORIGINAL_NAME VARCHAR(255) NOT NULL,
                    CONTENT_TYPE VARCHAR(100),
                    FILE_SIZE BIGINT NOT NULL,
                    DESCRIPTION VARCHAR(500),
                    UPLOAD_USER VARCHAR(50),
                    UPLOAD_TIME VARCHAR(40) NOT NULL,
                    BUSINESS_CODE VARCHAR(10) DEFAULT 'UD',
                    USE_YN CHAR(1) DEFAULT 'Y',
                    PRIMARY KEY (FILE_ID)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS UD_FILE_META_IX1 ON UD_FILE_META (UPLOAD_TIME)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS UD_FILE_META_IX2 ON UD_FILE_META (UPLOAD_USER)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS UD_FILE_META_IX3 ON UD_FILE_META (ORIGINAL_NAME)");
    }

    private void removeEmptyDbStatus() {
        int deleted = jdbcTemplate.update("""
                DELETE FROM OM_DB_STATUS
                 WHERE DB_ID IN ('RDW', 'ADW', 'SESSIONDB')
                """);
        if (deleted > 0) {
            log.info("Removed empty or legacy DB status rows: {}", deleted);
        }
    }

    private void removeEmptyDeployStatus() {
        int deleted = jdbcTemplate.update("""
                DELETE FROM OM_DEPLOY_STATUS
                 WHERE BUSINESS_CODE = 'ET'
                """);
        if (deleted > 0) {
            log.info("Removed empty or legacy deploy status rows: {}", deleted);
        }
    }

    private void removeDuplicateOmSessionStatus() {
        int deleted = jdbcTemplate.update("""
                DELETE FROM OM_SESSION_STATUS
                 WHERE SCOPE_ID = 'OM-AP'
                """);
        if (deleted > 0) {
            log.info("Removed duplicate OM HTTP session row (OM-PORTAL Spring Session only): {}", deleted);
        }
    }

    private void removeEmptySessionStatus() {
        int deleted = jdbcTemplate.update("""
                DELETE FROM OM_SESSION_STATUS
                 WHERE SCOPE_ID IN ('legacy-session')
                """);
        if (deleted > 0) {
            log.info("Removed empty session status rows: {}", deleted);
        }
    }

    private void removeLegacyApSeedApStatus() {
        int deleted = jdbcTemplate.update("""
                DELETE FROM OM_AP_STATUS
                 WHERE AP_ID IN ('ap01', 'ap02', 'om-local')
                """);
        if (deleted > 0) {
            log.info("Removed empty or legacy AP status rows: {}", deleted);
        }
    }

    /** OM_MENU 상위 그룹(폴더) — 기능권한·메뉴관리 계층. 기존에 상위가 없는 시드만 자동 연결. */
    private void ensureMenuHierarchy() {
        mergeMenuFolder("OM_GRP_OPS", "운영", 0);
        mergeMenuFolder("OM_GRP_RTM", "런타임·장애진단", 5);
        mergeMenuFolder("OM_GRP_SYS", "시스템·배포", 10);
        mergeMenuFolder("OM_GRP_AUTH", "권한·코드", 20);

        jdbcTemplate.update("""
                MERGE INTO OM_MENU (MENU_ID, MENU_NAME, MENU_URL, PARENT_MENU_ID, SORT_ORDER, USE_YN) KEY (MENU_ID)
                VALUES ('OM_SES', '세션 관리', '/om/admin/session.html', 'OM_GRP_OPS', 6, 'Y')
                """);

        Map<String, String> childToParent = Map.ofEntries(
                Map.entry("OM_DASH", "OM_GRP_OPS"),
                Map.entry("OM_TX", "OM_GRP_OPS"),
                Map.entry("OM_TXC", "OM_GRP_OPS"),
                Map.entry("OM_SVC", "OM_GRP_OPS"),
                Map.entry("OM_AUTH", "OM_GRP_OPS"),
                Map.entry("OM_AUDIT", "OM_GRP_OPS"),
                Map.entry("OM_SES", "OM_GRP_OPS"),
                Map.entry("OM_ERR", "OM_GRP_SYS"),
                Map.entry("OM_BAT", "OM_GRP_SYS"),
                Map.entry("OM_HLT", "OM_GRP_SYS"),
                Map.entry("OM_CFG", "OM_GRP_SYS"),
                Map.entry("OM_FIL", "OM_GRP_SYS"),
                Map.entry("OM_DPL", "OM_GRP_SYS"),
                Map.entry("OM_CDC", "OM_GRP_AUTH"),
                Map.entry("OM_FAU", "OM_GRP_AUTH"),
                Map.entry("OM_DAU", "OM_GRP_AUTH"),
                Map.entry("OM_AHT", "OM_GRP_AUTH"),
                Map.entry("OM_CCH", "OM_GRP_AUTH")
        );
        Map<String, Integer> menuSort = Map.ofEntries(
                Map.entry("OM_DASH", 1),
                Map.entry("OM_TX", 2),
                Map.entry("OM_TXC", 3),
                Map.entry("OM_SVC", 4),
                Map.entry("OM_AUTH", 5),
                Map.entry("OM_AUDIT", 6),
                Map.entry("OM_SES", 7),
                Map.entry("OM_ERR", 11),
                Map.entry("OM_BAT", 12),
                Map.entry("OM_HLT", 13),
                Map.entry("OM_CFG", 14),
                Map.entry("OM_FIL", 15),
                Map.entry("OM_DPL", 16),
                Map.entry("OM_CDC", 21),
                Map.entry("OM_FAU", 22),
                Map.entry("OM_DAU", 23),
                Map.entry("OM_AHT", 24),
                Map.entry("OM_CCH", 25)
        );
        for (Map.Entry<String, String> entry : childToParent.entrySet()) {
            String menuId = entry.getKey();
            jdbcTemplate.update("""
                    UPDATE OM_MENU
                       SET PARENT_MENU_ID = ?,
                           SORT_ORDER = ?
                     WHERE MENU_ID = ?
                    """, entry.getValue(), menuSort.get(menuId), menuId);
        }
        jdbcTemplate.update("""
                UPDATE OM_MENU
                   SET MENU_NAME = '사용자/권한/메뉴/기능·데이터권한'
                 WHERE MENU_ID = 'OM_AUTH'
                """);
        jdbcTemplate.update("""
                UPDATE OM_MENU
                   SET MENU_URL = '/om/admin/user-auth.html',
                       USE_YN = 'N'
                 WHERE MENU_ID = 'OM_FAU'
                """);
        jdbcTemplate.update("""
                UPDATE OM_MENU
                   SET MENU_URL = '/om/admin/user-auth.html',
                       USE_YN = 'N'
                 WHERE MENU_ID = 'OM_DAU'
                """);
    }

    /** 설계서 §2.1 런타임·장애진단 메뉴 그룹 (RTM-010~090) */
    private void ensureRuntimeDiagnosticsMenus() {
        mergeMenuFolder("OM_GRP_RTM", "런타임·장애진단", 5);
        String[][] menus = {
                {"OM_RTM_GUIDE", "진단 순서 가이드", "/om/admin/runtime-diagnosis-guide.html", "1"},
                {"OM_RTM_WS", "런타임·장애진단", "/om/admin/runtime-workspace.html", "2"},
                {"OM_RTM_DASH", "런타임 진단 상세", "/om/admin/runtime-diagnostics.html", "3"},
                {"OM_RTM_CARDS", "핵심 상태 카드", "/om/admin/runtime-status-cards.html", "4"},
                {"OM_RTM_THREAD", "Thread 분석", "/om/admin/runtime-thread-analysis.html", "5"},
                {"OM_RTM_JVM", "JVM 분석", "/om/admin/runtime-jvm-analysis.html", "6"},
                {"OM_RTM_DBPOOL", "DB Pool 분석", "/om/admin/runtime-dbpool-analysis.html", "7"},
                {"OM_RTM_DOM", "WAR·자원 독점", "/om/admin/runtime-dominance-analysis.html", "8"},
                {"OM_RTM_OCC", "업무 점유 현황", "/om/admin/runtime-business-occupancy.html", "9"},
                {"OM_RTM_ACTIVE", "실행 중 거래", "/om/admin/runtime-active-transactions.html", "10"},
                {"OM_RTM_SQL", "Slow SQL·외부연계", "/om/admin/runtime-sql-analysis.html", "11"},
                {"OM_RTM_TXDET", "거래·Thread 상세", "/om/admin/runtime-transaction-detail.html", "12"},
                {"OM_RTM_CAUSE", "장애 진단 및 보고서", "/om/admin/runtime-cause-analysis.html", "13"},
                {"OM_RTM_FLOW", "장애 흐름", "/om/admin/runtime-incident-flow.html", "14"},
                {"OM_RTM_HIST", "장애 이력", "/om/admin/runtime-incident-history.html", "15"},
                {"OM_RTM_THR", "임계치·수집설정", "/om/admin/runtime-threshold-policy.html", "16"}
        };
        for (String[] menu : menus) {
            jdbcTemplate.update("""
                    MERGE INTO OM_MENU (MENU_ID, MENU_NAME, MENU_URL, PARENT_MENU_ID, SORT_ORDER, USE_YN) KEY (MENU_ID)
                    VALUES (?, ?, ?, 'OM_GRP_RTM', ?, 'Y')
                    """, menu[0], menu[1], menu[2], Integer.parseInt(menu[3]));
        }
        jdbcTemplate.update("""
                UPDATE OM_MENU
                   SET PARENT_MENU_ID = 'OM_GRP_RTM',
                       MENU_NAME = '런타임 진단 상세',
                       MENU_URL = '/om/admin/runtime-diagnostics.html',
                       SORT_ORDER = 3,
                       USE_YN = 'Y'
                 WHERE MENU_ID = 'OM_RTM'
                """);
        jdbcTemplate.update("""
                MERGE INTO OM_MENU (MENU_ID, MENU_NAME, MENU_URL, PARENT_MENU_ID, SORT_ORDER, USE_YN) KEY (MENU_ID)
                VALUES ('OM_RTM_WS', '런타임·장애진단', '/om/admin/runtime-workspace.html', 'OM_GRP_RTM', 2, 'Y')
                """);
        jdbcTemplate.update("""
                UPDATE OM_MENU SET USE_YN = 'N' WHERE MENU_ID IN ('OM_RTM_010', 'OM_RTM_020')
                """);
    }

    private void ensureTransactionControlTable() {
        Integer legacyColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_NAME = 'TCF_TRANSACTION_CONTROL'
                   AND COLUMN_NAME = 'TX_ID'
                """, Integer.class);
        if (legacyColumnCount != null && legacyColumnCount > 0) {
            jdbcTemplate.execute("DROP TABLE IF EXISTS TCF_TRANSACTION_CONTROL");
            log.info("Dropped legacy TCF_TRANSACTION_CONTROL table (status-log schema)");
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS TCF_TRANSACTION_CONTROL (
                    SERVICE_ID VARCHAR(100) NOT NULL,
                    TRANSACTION_CODE VARCHAR(50) NOT NULL,
                    BUSINESS_CODE VARCHAR(10) NOT NULL,
                    SERVICE_NAME VARCHAR(200) NOT NULL,
                    USER_ID VARCHAR(50) NOT NULL,
                    CHANNEL_ID VARCHAR(30) NOT NULL,
                    BRANCH_ID VARCHAR(30) NOT NULL,
                    CONTROL_TYPE VARCHAR(20) NOT NULL DEFAULT 'FULL',
                    BLOCK_YN CHAR(1) NOT NULL DEFAULT 'Y',
                    PRIMARY KEY (
                        SERVICE_ID,
                        TRANSACTION_CODE,
                        BUSINESS_CODE,
                        SERVICE_NAME,
                        USER_ID,
                        CHANNEL_ID,
                        BRANCH_ID
                    )
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS IDX_TCF_TX_CTRL_USER
                ON TCF_TRANSACTION_CONTROL (USER_ID, CHANNEL_ID, BRANCH_ID)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS IDX_TCF_TX_CTRL_SVC
                ON TCF_TRANSACTION_CONTROL (BUSINESS_CODE, SERVICE_ID, TRANSACTION_CODE)
                """);
        Integer controlTypeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM INFORMATION_SCHEMA.COLUMNS
                 WHERE TABLE_NAME = 'TCF_TRANSACTION_CONTROL'
                   AND COLUMN_NAME = 'CONTROL_TYPE'
                """, Integer.class);
        if (controlTypeCount == null || controlTypeCount == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE TCF_TRANSACTION_CONTROL
                    ADD COLUMN IF NOT EXISTS CONTROL_TYPE VARCHAR(20) NOT NULL DEFAULT 'FULL'
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE TCF_TRANSACTION_CONTROL
                    ADD COLUMN IF NOT EXISTS BLOCK_YN CHAR(1) NOT NULL DEFAULT 'N'
                    """);
            jdbcTemplate.update("UPDATE TCF_TRANSACTION_CONTROL SET BLOCK_YN = 'N' WHERE BLOCK_YN IS NULL");
            log.info("Migrated TCF_TRANSACTION_CONTROL to blocklist schema (CONTROL_TYPE, BLOCK_YN)");
        }
        seedTxControlTypeCommonCodes();
    }

    private void seedTxControlTypeCommonCodes() {
        String ts = java.time.OffsetDateTime.now().toString();
        mergeCommonCode("TX_CONTROL_TYPE", "GLOBAL", "전체 통제", 1, "모든 거래 통제", ts);
        mergeCommonCode("TX_CONTROL_TYPE", "BUSINESS", "업무코드별 통제", 2, "businessCode 일치", ts);
        mergeCommonCode("TX_CONTROL_TYPE", "SERVICE", "거래서비스ID별 통제", 3, "serviceId 일치", ts);
        mergeCommonCode("TX_CONTROL_TYPE", "CHANNEL", "채널별 통제", 4, "channelId 일치", ts);
        mergeCommonCode("TX_CONTROL_TYPE", "BRANCH", "브랜치별 통제", 5, "branchId 일치", ts);
        mergeCommonCode("TX_CONTROL_TYPE", "USER", "사용자 통제", 6, "userId 일치", ts);
        mergeCommonCode("TX_CONTROL_TYPE", "IP", "IP별 통제", 7, "clientIp 일치", ts);
    }

    private void ensureMessageStructTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_MESSAGE_STRUCT (
                    STRUCT_ID VARCHAR(64) NOT NULL,
                    STRUCT_CODE VARCHAR(80) NOT NULL,
                    BUSINESS_CODE VARCHAR(10),
                    SERVICE_ID VARCHAR(100),
                    TRANSACTION_CODE VARCHAR(50),
                    MESSAGE_TYPE VARCHAR(20) NOT NULL,
                    SEGMENT_TYPE VARCHAR(20) NOT NULL,
                    STRUCT_NAME VARCHAR(200) NOT NULL,
                    DESCRIPTION VARCHAR(500),
                    SAMPLE_JSON CLOB,
                    USE_YN CHAR(1) DEFAULT 'Y',
                    PRIMARY KEY (STRUCT_ID)
                )
                """);
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS IDX_OM_MSG_STRUCT_CODE ON OM_MESSAGE_STRUCT (STRUCT_CODE)
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS OM_MESSAGE_FIELD (
                    FIELD_ID VARCHAR(64) NOT NULL,
                    STRUCT_ID VARCHAR(64) NOT NULL,
                    FIELD_KEY VARCHAR(100) NOT NULL,
                    FIELD_LABEL VARCHAR(200),
                    DATA_TYPE VARCHAR(20) NOT NULL DEFAULT 'STRING',
                    REQUIRED_YN CHAR(1) DEFAULT 'N',
                    MAX_LENGTH INT,
                    DEFAULT_VALUE VARCHAR(500),
                    SAMPLE_VALUE VARCHAR(500),
                    VALIDATION_RULE VARCHAR(200),
                    DESCRIPTION VARCHAR(500),
                    SORT_ORDER INT DEFAULT 0,
                    PRIMARY KEY (FIELD_ID)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS IDX_OM_MSG_FIELD_STRUCT ON OM_MESSAGE_FIELD (STRUCT_ID, SORT_ORDER)
                """);
    }

    private void removeLegacyFunctionAuthIds() {
        int deleted = jdbcTemplate.update("""
                DELETE FROM OM_FUNCTION_AUTH
                 WHERE AUTH_ID IN ('FA-001', 'FA-002', 'FA-003', 'FA-004')
                """);
        if (deleted > 0) {
            log.info("Removed legacy OM_FUNCTION_AUTH seed rows: {}", deleted);
        }
    }

    private void mergeMenuFolder(String menuId, String menuName, int sortOrder) {
        jdbcTemplate.update("""
                MERGE INTO OM_MENU (MENU_ID, MENU_NAME, MENU_URL, PARENT_MENU_ID, SORT_ORDER, USE_YN) KEY (MENU_ID)
                VALUES (?, ?, '', NULL, ?, 'Y')
                """, menuId, menuName, sortOrder);
    }

    /** 그룹 메뉴 URL·미연결 상위 ID의 NULL을 빈 문자열로 정규화 (API/화면 null 표시 방지). */
    private void normalizeMenuNullColumns() {
        jdbcTemplate.update("""
                UPDATE OM_MENU
                   SET MENU_URL = ''
                 WHERE MENU_URL IS NULL
                """);
    }

}
