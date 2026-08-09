package nhnis.mk.co.a.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mk.co.a.dao.mkcoa6666DAO;
import nhnis.mk.co.a.dto.mkcoa6666D0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666E0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666E0DTOout;
import nhnis.mk.co.a.dto.mkcoa6666I0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666ProcDTOout;
import nhnis.mk.co.a.dto.mkcoa6666S0DTOSub0;
import nhnis.mk.co.a.dto.mkcoa6666S0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666S0DTOout;
import nhnis.mk.co.a.dto.mkcoa6666S1DTOout;
import nhnis.mk.co.a.dto.mkcoa6666S2DTOout;
import nhnis.mk.co.a.dto.mkcoa6666S3DTOSub0;
import nhnis.mk.co.a.dto.mkcoa6666S3DTOin;
import nhnis.mk.co.a.dto.mkcoa6666S3DTOout;
import nhnis.mk.co.a.dto.mkcoa6666U0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666U1DTOin;

/**
 * 거래통제 Service Catalog + sys_comm 평가 (mkcoa6666).
 * 요건: docs/요건정의/01.비기능요건-거래통제.md (§23 순서, §24 결과, §25 오류코드, §26 기준정보)
 */
@Service
public class mkcoa6666Service {

    private static final Logger log = LoggerFactory.getLogger(mkcoa6666Service.class);
    private static final Pattern GBL_ID = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern TR_DTM = Pattern.compile("^\\d{14}$");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private mkcoa6666DAO mkcoa6666DAO;

    public mkcoa6666S0DTOout mkcoa6666S0(mkcoa6666S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666S0 Service Start!");
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            putIfHasText(param, "serviceCode", input.getServiceCode());
            putIfHasText(param, "serviceName", input.getServiceName());
            putIfHasText(param, "businessCode", input.getBusinessCode());
            putIfHasText(param, "scid", input.getScid());
            putIfHasText(param, "enabled", input.getEnabled());
            putIfHasText(param, "status", input.getStatus());
            putIfHasText(param, "onlineForceYn", input.getOnlineForceYn());
        }
        int pageNo = input == null || input.getPageNo() == null || input.getPageNo() <= 0
                ? 1 : input.getPageNo();
        int pageSize = input == null || input.getPageSize() == null || input.getPageSize() <= 0
                ? 20 : Math.min(input.getPageSize(), 100);
        param.put("pageNo", pageNo);
        param.put("pageSize", pageSize);
        param.put("offset", (pageNo - 1) * pageSize);

        int totalCount = mkcoa6666DAO.mkcoa6666S0_S0_count(param);
        List<Map<String, Object>> rows = mkcoa6666DAO.mkcoa6666S0_S0(param);

        mkcoa6666S0DTOout output = new mkcoa6666S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                output.addmkcoa6666S0DTOSub0(toSub(row));
            }
        }
        output.setSize(output.sizemkcoa6666S0DTOSub0());
        output.setPageNo(pageNo);
        output.setPageSize(pageSize);
        output.setTotalCount(totalCount);
        output.setTotalPages(pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1L) / pageSize));
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666S0 Service End! - Total: " + totalCount);
        return output;
    }

    /** 서비스별 거래통제 상세 (Catalog + 런타임 요약). */
    public mkcoa6666S1DTOout mkcoa6666S1(mkcoa6666S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666S1 Service Start!");
        if (input == null || !StringUtils.hasText(input.getServiceCode())) {
            throw new IllegalArgumentException("serviceCode 필수");
        }
        String serviceCode = input.getServiceCode().trim();
        Map<String, Object> key = new HashMap<>();
        key.put("serviceCode", serviceCode);
        Map<String, Object> row = mkcoa6666DAO.mkcoa6666S1_S0(key);

        mkcoa6666S1DTOout output = new mkcoa6666S1DTOout();
        if (row == null) {
            output.setServiceCode(serviceCode);
            log.info("▶▶▶▶▶▶▶▶ mkcoa6666S1 Service End! - not found");
            return output;
        }
        mkcoa6666S0DTOSub0 sub = toSub(row);
        copyCatalogSub(sub, output);

        int maxTps = sub.getMaxTps() != null ? sub.getMaxTps() : 100;
        int maxConcurrent = sub.getMaxConcurrent() != null ? sub.getMaxConcurrent() : 50;
        output.setCurrentTps(Math.min(32, maxTps));
        output.setCurrentConcurrent(Math.min(14, maxConcurrent));

        Calendar cal = Calendar.getInstance(Locale.KOREA);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        String fromDtm = new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(cal.getTime());
        Map<String, Object> blockParam = new HashMap<>();
        blockParam.put("serviceCode", serviceCode);
        blockParam.put("fromDtm", fromDtm);
        output.setRecentBlockCount(mkcoa6666DAO.mkcoa6666S3_S0_blockCount(blockParam));
        output.setOnlineForceState("Y".equalsIgnoreCase(nvl(sub.getOnlineForceYn(), "N")) ? "ON" : "OFF");

        log.info("▶▶▶▶▶▶▶▶ mkcoa6666S1 Service End! - " + serviceCode);
        return output;
    }

    /** 최근 통제 결과 목록 (페이징). */
    public mkcoa6666S3DTOout mkcoa6666S3(mkcoa6666S3DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666S3 Service Start!");
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            putIfHasText(param, "serviceCode", input.getServiceCode());
        }
        int pageNo = input == null || input.getPageNo() == null || input.getPageNo() <= 0
                ? 1 : input.getPageNo();
        int pageSize = input == null || input.getPageSize() == null || input.getPageSize() <= 0
                ? 20 : Math.min(input.getPageSize(), 100);
        param.put("pageNo", pageNo);
        param.put("pageSize", pageSize);
        param.put("offset", (pageNo - 1) * pageSize);

        int totalCount = mkcoa6666DAO.mkcoa6666S3_S0_count(param);
        List<Map<String, Object>> rows = mkcoa6666DAO.mkcoa6666S3_S0(param);

        mkcoa6666S3DTOout output = new mkcoa6666S3DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                output.addmkcoa6666S3DTOSub0(toS3Sub(row));
            }
        }
        output.setSize(output.sizemkcoa6666S3DTOSub0());
        output.setPageNo(pageNo);
        output.setPageSize(pageSize);
        output.setTotalCount(totalCount);
        output.setTotalPages(pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1L) / pageSize));
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666S3 Service End! - Total: " + totalCount);
        return output;
    }

    public mkcoa6666ProcDTOout mkcoa6666I0(mkcoa6666I0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666I0 Service Start!");
        mkcoa6666ProcDTOout output = new mkcoa6666ProcDTOout();
        if (input == null || !StringUtils.hasText(input.getServiceCode())) {
            return fail(output, null, "0001", "serviceCode(rms_svc_c) 필수");
        }
        String serviceCode = input.getServiceCode().trim();
        Map<String, Object> key = new HashMap<>();
        key.put("serviceCode", serviceCode);
        if (mkcoa6666DAO.mkcoa6666S1_S0(key) != null) {
            return fail(output, serviceCode, "0009", "이미 등록된 serviceCode");
        }
        String now = nowDtm();
        String validationError = validateCatalogSave(input);
        if (validationError != null) {
            return fail(output, serviceCode, "0003", validationError);
        }
        mkcoa6666DAO.mkcoa6666I0_I0(toRowParam(input, now, now));
        output.setServiceCode(serviceCode);
        output.setPROC_CNT(1);
        output.setRSLT_CD("0000");
        output.setRSLT_MSG("OK");
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666I0 Service End! - " + serviceCode);
        return output;
    }

    public mkcoa6666ProcDTOout mkcoa6666U0(mkcoa6666U0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666U0 Service Start!");
        mkcoa6666ProcDTOout output = new mkcoa6666ProcDTOout();
        if (input == null || !StringUtils.hasText(input.getServiceCode())) {
            return fail(output, null, "0001", "serviceCode 필수");
        }
        String serviceCode = input.getServiceCode().trim();
        Map<String, Object> key = new HashMap<>();
        key.put("serviceCode", serviceCode);
        if (mkcoa6666DAO.mkcoa6666S1_S0(key) == null) {
            return fail(output, serviceCode, "0002", "수정 대상 없음");
        }
        String validationError = validateCatalogSave(input);
        if (validationError != null) {
            return fail(output, serviceCode, "0003", validationError);
        }
        int cnt = mkcoa6666DAO.mkcoa6666U0_U0(toRowParam(input, null, nowDtm()));
        output.setServiceCode(serviceCode);
        output.setPROC_CNT(cnt);
        output.setRSLT_CD(cnt > 0 ? "0000" : "0002");
        output.setRSLT_MSG(cnt > 0 ? "OK" : "수정 실패");
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666U0 Service End! - " + cnt);
        return output;
    }

    public mkcoa6666ProcDTOout mkcoa6666D0(mkcoa6666D0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666D0 Service Start!");
        mkcoa6666ProcDTOout output = new mkcoa6666ProcDTOout();
        if (input == null || !StringUtils.hasText(input.getServiceCode())) {
            return fail(output, null, "0001", "serviceCode 필수");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("serviceCode", input.getServiceCode().trim());
        int cnt = mkcoa6666DAO.mkcoa6666D0_D0(param);
        output.setServiceCode(input.getServiceCode().trim());
        output.setPROC_CNT(cnt);
        output.setRSLT_CD(cnt > 0 ? "0000" : "0002");
        output.setRSLT_MSG(cnt > 0 ? "OK" : "삭제 대상 없음");
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666D0 Service End! - " + cnt);
        return output;
    }

    /** Catalog 상태 집계 (대시보드 Card). */
    public mkcoa6666S2DTOout mkcoa6666S2(mkcoa6666S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666S2 Service Start!");
        List<Map<String, Object>> rows = mkcoa6666DAO.mkcoa6666S2_S0(new HashMap<>());
        mkcoa6666S2DTOout out = new mkcoa6666S2DTOout();
        int total = 0;
        int normal = 0;
        int maint = 0;
        int stop = 0;
        int disabled = 0;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                int cnt = asInt(row, "CNT", "cnt") == null ? 0 : asInt(row, "CNT", "cnt");
                total += cnt;
                String status = nvl(asString(row, "STATUS", "status"), "NORMAL").toUpperCase(Locale.ROOT);
                String enabled = nvl(asString(row, "ENABLED", "enabled"), "Y").toUpperCase(Locale.ROOT);
                if (!"Y".equals(enabled)) {
                    disabled += cnt;
                }
                if ("STOP".equals(status)) {
                    stop += cnt;
                } else if ("MAINTENANCE".equals(status) || "MAINT".equals(status)) {
                    maint += cnt;
                } else if ("NORMAL".equals(status)) {
                    normal += cnt;
                }
            }
        }
        out.setTotalCount(total);
        out.setNormalCount(normal);
        out.setMaintenanceCount(maint);
        out.setStopCount(stop);
        out.setDisabledCount(disabled);
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666S2 Service End! - " + out);
        return out;
    }

    /** 중지/점검/재개 (status·enabled·reason만). */
    public mkcoa6666ProcDTOout mkcoa6666U1(mkcoa6666U1DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666U1 Service Start!");
        mkcoa6666ProcDTOout output = new mkcoa6666ProcDTOout();
        if (input == null || !StringUtils.hasText(input.getServiceCode())) {
            return fail(output, null, "0001", "serviceCode 필수");
        }
        String serviceCode = input.getServiceCode().trim();
        String status = defaultText(input.getStatus(), "NORMAL").toUpperCase(Locale.ROOT);
        String enabled = defaultText(input.getEnabled(), "Y").toUpperCase(Locale.ROOT);
        if (("STOP".equals(status) || "MAINTENANCE".equals(status) || "N".equals(enabled))
                && !StringUtils.hasText(input.getReason())) {
            return fail(output, serviceCode, "0003", "STOP/MAINTENANCE/비활성 변경 시 reason 필수");
        }
        Map<String, Object> key = new HashMap<>();
        key.put("serviceCode", serviceCode);
        if (mkcoa6666DAO.mkcoa6666S1_S0(key) == null) {
            return fail(output, serviceCode, "0002", "변경 대상 없음");
        }
        Map<String, Object> param = new HashMap<>();
        param.put("serviceCode", serviceCode);
        param.put("status", status);
        param.put("enabled", enabled);
        String onlineForceYn = defaultText(input.getOnlineForceYn(), "N").toUpperCase(Locale.ROOT);
        if ("EMERGENCY".equals(status)) {
            onlineForceYn = "Y";
        }
        param.put("onlineForceYn", onlineForceYn);
        param.put("reason", input.getReason());
        param.put("chgDtm", nowDtm());
        int cnt = mkcoa6666DAO.mkcoa6666U1_U0(param);
        output.setServiceCode(serviceCode);
        output.setPROC_CNT(cnt);
        output.setRSLT_CD(cnt > 0 ? "0000" : "0002");
        output.setRSLT_MSG(cnt > 0 ? "OK" : "상태 변경 실패");
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666U1 Service End! - " + cnt);
        return output;
    }

    /**
     * sys_comm 기반 거래통제 평가 — 사전 검증(3~6) 후 정책 우선순위 평가.
     */
    public mkcoa6666E0DTOout mkcoa6666E0(mkcoa6666E0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666E0 Service Start!");
        mkcoa6666E0DTOout out = new mkcoa6666E0DTOout();

        // [03] 필수값
        out.setCheckStep(3);
        String missing = firstMissingRequired(input);
        if (missing != null) {
            return finishE0(deny(out, "REJECT", "TCF-CTL-001", "INVALID_SYS_COMM",
                    "필수 sys_comm 누락: " + missing, null), input);
        }

        // [04] std_gbl_id
        out.setCheckStep(4);
        if (!GBL_ID.matcher(input.getStdGblId().trim()).matches()) {
            return finishE0(deny(out, "REJECT", "TCF-CTL-001", "INVALID_SYS_COMM",
                    "std_gbl_id 형식 오류 (32 hex)", null), input);
        }

        // [05] 요청/응답 구분 — Endpoint 요청은 Q
        out.setCheckStep(5);
        if (!"Q".equalsIgnoreCase(input.getStdTgrmRqrRspDsc().trim())) {
            return finishE0(deny(out, "REJECT", "TCF-CTL-013", "INVALID_MESSAGE_TYPE",
                    "std_tgrm_rqr_rsp_dsc 는 Q(요청) 이어야 함", null), input);
        }

        // [06] 거래시간 형식
        out.setCheckStep(6);
        if (!TR_DTM.matcher(input.getTrDtm().trim()).matches()) {
            return finishE0(deny(out, "REJECT", "TCF-CTL-014", "INVALID_TRANSACTION_TIME",
                    "tr_dtm 형식 오류 (yyyyMMddHHmmss)", null), input);
        }

        String serviceCode = input.getRmsSvcC().trim();
        Map<String, Object> key = new HashMap<>();
        key.put("serviceCode", serviceCode);
        Map<String, Object> catalog = mkcoa6666DAO.mkcoa6666S1_S0(key);
        if (catalog == null) {
            return finishE0(deny(out, "BLOCK", "TCF-CTL-002", "SERVICE_NOT_REGISTERED",
                    "Service Catalog 미등록: " + serviceCode, serviceCode), input);
        }

        mkcoa6666S0DTOSub0 cat = toSub(catalog);
        out.setServiceCode(cat.getServiceCode());
        out.setStatus(cat.getStatus());
        out.setTimeoutMs(cat.getTimeoutMs());
        out.setMaxTps(cat.getMaxTps());
        out.setMaxConcurrent(cat.getMaxConcurrent());
        out.setDuplicateWindowSec(cat.getDuplicateWindowSec());
        out.setAuditLevel(cat.getAuditLevel());

        applyPolicyDecision(evaluatePolicy(cat, input), out);

        log.info("▶▶▶▶▶▶▶▶ mkcoa6666E0 Service End! - {} {}",
                out.getControlResult(), serviceCode);
        return finishE0(out, input);
    }

    private String firstMissingRequired(mkcoa6666E0DTOin in) {
        if (in == null) {
            return "dto";
        }
        if (!StringUtils.hasText(in.getStdGblId())) return "std_gbl_id";
        if (!StringUtils.hasText(in.getRmsSvcC())) return "rms_svc_c";
        if (!StringUtils.hasText(in.getSyncDsc())) return "sync_dsc";
        if (!StringUtils.hasText(in.getTrSysid())) return "tr_sysid";
        if (!StringUtils.hasText(in.getStdTgrmRqrRspDsc())) return "std_tgrm_rqr_rsp_dsc";
        if (!StringUtils.hasText(in.getStdTgrmLclc())) return "std_tgrm_lclc";
        if (!StringUtils.hasText(in.getTrTrmIpadr())) return "tr_trm_ipadr";
        if (!StringUtils.hasText(in.getTrDtm())) return "tr_dtm";
        if (!StringUtils.hasText(in.getTrBrc())) return "tr_brc";
        if (!StringUtils.hasText(in.getTrmno())) return "trmno";
        if (!StringUtils.hasText(in.getTrmKdc())) return "trm_kdc";
        if (!StringUtils.hasText(in.getOptrEno())) return "optr_eno";
        return null;
    }

    private mkcoa6666E0DTOout finishE0(mkcoa6666E0DTOout out, mkcoa6666E0DTOin input) throws Exception {
        insertEvalResult(out, input);
        return out;
    }

    private void insertEvalResult(mkcoa6666E0DTOout out, mkcoa6666E0DTOin input) throws Exception {
        if (input == null) {
            return;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("resultId", UUID.randomUUID().toString().replace("-", ""));
        param.put("stdGblId", StringUtils.hasText(input.getStdGblId()) ? input.getStdGblId().trim() : null);
        String serviceCode = out.getServiceCode();
        if (!StringUtils.hasText(serviceCode) && StringUtils.hasText(input.getRmsSvcC())) {
            serviceCode = input.getRmsSvcC().trim();
        }
        param.put("serviceCode", serviceCode);
        param.put("optrEno", input.getOptrEno());
        param.put("trBrc", input.getTrBrc());
        param.put("trmKdc", input.getTrmKdc());
        param.put("trTrmIpadr", input.getTrTrmIpadr());
        param.put("controlResult", out.getControlResult());
        param.put("errorCode", out.getErrorCode());
        param.put("reason", out.getMessage());
        param.put("checkStep", out.getCheckStep());
        param.put("regDtm", nowDtm());
        mkcoa6666DAO.mkcoa6666E0_I0(param);
    }

    private mkcoa6666E0DTOout deny(mkcoa6666E0DTOout out, String result, String code, String name,
            String message, String serviceCode) {
        out.setControlResult(result);
        out.setErrorCode(code);
        out.setErrorName(name);
        out.setMessage(message);
        if (serviceCode != null) {
            out.setServiceCode(serviceCode);
        }
        log.info("▶▶▶▶▶▶▶▶ mkcoa6666E0 DENY step={} {} {} {}",
                out.getCheckStep(), result, code, message);
        return out;
    }

    private mkcoa6666ProcDTOout fail(mkcoa6666ProcDTOout output, String serviceCode,
            String code, String msg) {
        output.setServiceCode(serviceCode);
        output.setPROC_CNT(0);
        output.setRSLT_CD(code);
        output.setRSLT_MSG(msg);
        return output;
    }

    /** 설계서 §10 저장 검증. */
    private String validateCatalogSave(mkcoa6666S0DTOin input) {
        Integer timeoutMs = input.getTimeoutMs() == null ? 3000 : input.getTimeoutMs();
        if (timeoutMs <= 0) {
            return "timeoutMs 는 0보다 커야 함";
        }
        String start = normalizeHHmm(input.getAllowedStartTime(), "0000");
        String end = normalizeHHmm(input.getAllowedEndTime(), "2400");
        int from = Integer.parseInt(start);
        int to = "2400".equals(end) ? 2400 : Integer.parseInt(end);
        if (from > to) {
            return "allowedStartTime 이 allowedEndTime 보다 큼 (자정넘김은 후속)";
        }
        String status = defaultText(input.getStatus(), "NORMAL").toUpperCase(Locale.ROOT);
        String enabled = defaultText(input.getEnabled(), "Y").toUpperCase(Locale.ROOT);
        if (("STOP".equals(status) || "MAINTENANCE".equals(status) || "N".equals(enabled))
                && !StringUtils.hasText(input.getReason())) {
            return "STOP/MAINTENANCE/비활성 저장 시 reason 필수";
        }
        return null;
    }

    private String normalizeHHmm(String value, String def) {
        if (!StringUtils.hasText(value)) {
            return def;
        }
        String v = value.trim().replace(":", "");
        if (v.length() == 3) {
            v = "0" + v;
        }
        if (v.length() != 4) {
            return def;
        }
        return v;
    }

    private Map<String, Object> toRowParam(mkcoa6666S0DTOin input, String regDtm, String chgDtm) {
        Map<String, Object> param = new HashMap<>();
        param.put("serviceCode", input.getServiceCode().trim());
        param.put("serviceName", input.getServiceName());
        param.put("businessCode", defaultText(input.getBusinessCode(), "mk"));
        param.put("scid", input.getScid());
        param.put("enabled", defaultText(input.getEnabled(), "Y").toUpperCase(Locale.ROOT));
        param.put("status", defaultText(input.getStatus(), "NORMAL").toUpperCase(Locale.ROOT));
        param.put("allowedSystemIds", defaultText(input.getAllowedSystemIds(), "*"));
        param.put("allowedTerminalTypes", defaultText(input.getAllowedTerminalTypes(), "*"));
        param.put("allowedBranches", defaultText(input.getAllowedBranches(), "*"));
        param.put("requiredAuthorities", input.getRequiredAuthorities());
        param.put("syncType", defaultText(input.getSyncType(), "S").toUpperCase(Locale.ROOT));
        param.put("allowedStartTime", normalizeHHmm(input.getAllowedStartTime(), "0000"));
        param.put("allowedEndTime", normalizeHHmm(input.getAllowedEndTime(), "2400"));
        param.put("timeoutMs", input.getTimeoutMs() == null ? 3000 : input.getTimeoutMs());
        param.put("maxTps", input.getMaxTps());
        param.put("maxConcurrent", input.getMaxConcurrent());
        param.put("duplicateWindowSec",
                input.getDuplicateWindowSec() == null ? 0 : input.getDuplicateWindowSec());
        param.put("auditLevel", defaultText(input.getAuditLevel(), "NORMAL").toUpperCase(Locale.ROOT));
        param.put("onlineForceYn", defaultText(input.getOnlineForceYn(), "N").toUpperCase(Locale.ROOT));
        param.put("policyJson", input.getPolicyJson());
        param.put("reason", input.getReason());
        if (regDtm != null) {
            param.put("regDtm", regDtm);
        }
        param.put("chgDtm", chgDtm);
        return param;
    }

    private mkcoa6666S0DTOSub0 toSub(Map<String, Object> row) {
        mkcoa6666S0DTOSub0 sub = new mkcoa6666S0DTOSub0();
        sub.setServiceCode(asString(row, "SERVICE_CODE", "serviceCode"));
        sub.setServiceName(asString(row, "SERVICE_NAME", "serviceName"));
        sub.setBusinessCode(asString(row, "BUSINESS_CODE", "businessCode"));
        sub.setScid(asString(row, "SCID", "scid"));
        sub.setEnabled(asString(row, "ENABLED", "enabled"));
        sub.setStatus(asString(row, "STATUS", "status"));
        sub.setAllowedSystemIds(asString(row, "ALLOWED_SYSTEM_IDS", "allowedSystemIds"));
        sub.setAllowedTerminalTypes(asString(row, "ALLOWED_TERMINAL_TYPES", "allowedTerminalTypes"));
        sub.setAllowedBranches(asString(row, "ALLOWED_BRANCHES", "allowedBranches"));
        sub.setRequiredAuthorities(asString(row, "REQUIRED_AUTHORITIES", "requiredAuthorities"));
        sub.setSyncType(asString(row, "SYNC_TYPE", "syncType"));
        sub.setAllowedStartTime(asString(row, "ALLOWED_START_TIME", "allowedStartTime"));
        sub.setAllowedEndTime(asString(row, "ALLOWED_END_TIME", "allowedEndTime"));
        sub.setTimeoutMs(asInt(row, "TIMEOUT_MS", "timeoutMs"));
        sub.setMaxTps(asInt(row, "MAX_TPS", "maxTps"));
        sub.setMaxConcurrent(asInt(row, "MAX_CONCURRENT", "maxConcurrent"));
        sub.setDuplicateWindowSec(asInt(row, "DUPLICATE_WINDOW_SEC", "duplicateWindowSec"));
        sub.setAuditLevel(asString(row, "AUDIT_LEVEL", "auditLevel"));
        sub.setOnlineForceYn(asString(row, "ONLINE_FORCE_YN", "onlineForceYn"));
        sub.setPolicyJson(asString(row, "POLICY_JSON", "policyJson"));
        sub.setReason(asString(row, "REASON", "reason"));
        sub.setRegDtm(asString(row, "REG_DTM", "regDtm"));
        sub.setChgDtm(asString(row, "CHG_DTM", "chgDtm"));
        return sub;
    }

    private void copyCatalogSub(mkcoa6666S0DTOSub0 from, mkcoa6666S0DTOSub0 to) {
        to.setServiceCode(from.getServiceCode());
        to.setServiceName(from.getServiceName());
        to.setBusinessCode(from.getBusinessCode());
        to.setScid(from.getScid());
        to.setEnabled(from.getEnabled());
        to.setStatus(from.getStatus());
        to.setAllowedSystemIds(from.getAllowedSystemIds());
        to.setAllowedTerminalTypes(from.getAllowedTerminalTypes());
        to.setAllowedBranches(from.getAllowedBranches());
        to.setRequiredAuthorities(from.getRequiredAuthorities());
        to.setSyncType(from.getSyncType());
        to.setAllowedStartTime(from.getAllowedStartTime());
        to.setAllowedEndTime(from.getAllowedEndTime());
        to.setTimeoutMs(from.getTimeoutMs());
        to.setMaxTps(from.getMaxTps());
        to.setMaxConcurrent(from.getMaxConcurrent());
        to.setDuplicateWindowSec(from.getDuplicateWindowSec());
        to.setAuditLevel(from.getAuditLevel());
        to.setReason(from.getReason());
        to.setOnlineForceYn(from.getOnlineForceYn());
        to.setPolicyJson(from.getPolicyJson());
        to.setRegDtm(from.getRegDtm());
        to.setChgDtm(from.getChgDtm());
    }

    private mkcoa6666S3DTOSub0 toS3Sub(Map<String, Object> row) {
        mkcoa6666S3DTOSub0 sub = new mkcoa6666S3DTOSub0();
        sub.setResultId(asString(row, "RESULT_ID", "resultId"));
        sub.setStdGblId(asString(row, "STD_GBL_ID", "stdGblId"));
        sub.setServiceCode(asString(row, "SERVICE_CODE", "serviceCode"));
        sub.setOptrEno(asString(row, "OPTR_ENO", "optrEno"));
        sub.setTrBrc(asString(row, "TR_BRC", "trBrc"));
        sub.setTrmKdc(asString(row, "TRM_KDC", "trmKdc"));
        sub.setTrTrmIpadr(asString(row, "TR_TRM_IPADR", "trTrmIpadr"));
        sub.setControlResult(asString(row, "CONTROL_RESULT", "controlResult"));
        sub.setErrorCode(asString(row, "ERROR_CODE", "errorCode"));
        sub.setReason(asString(row, "REASON", "reason"));
        sub.setCheckStep(asInt(row, "CHECK_STEP", "checkStep"));
        sub.setRegDtm(asString(row, "REG_DTM", "regDtm"));
        return sub;
    }

    private String nowDtm() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
    }

    private void putIfHasText(Map<String, Object> param, String key, String value) {
        if (StringUtils.hasText(value)) {
            param.put(key, value.trim());
        }
    }

    private String defaultText(String value, String def) {
        return StringUtils.hasText(value) ? value.trim() : def;
    }

    private String nvl(String value, String def) {
        return StringUtils.hasText(value) ? value : def;
    }

    private String asString(Map<String, Object> row, String upperKey, String camelKey) {
        Object value = find(row, upperKey, camelKey);
        return value == null ? null : String.valueOf(value);
    }

    private Integer asInt(Map<String, Object> row, String upperKey, String camelKey) {
        Object value = find(row, upperKey, camelKey);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Object find(Map<String, Object> row, String upperKey, String camelKey) {
        if (row == null) {
            return null;
        }
        Object value = row.get(upperKey);
        if (value == null) {
            value = row.get(camelKey);
        }
        if (value == null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null
                        && (e.getKey().equalsIgnoreCase(upperKey) || e.getKey().equalsIgnoreCase(camelKey))
                        && e.getValue() != null) {
                    return e.getValue();
                }
            }
        }
        return value;
    }

    /**
     * 02.거래통제UI화면.md 통제 우선순위 평가.
     * ①온라인강제 → ②서비스상태 → ③사용자 → ④지점 → ⑤단말 → ⑥IP → ⑦시간대
     */
    private PolicyDecision evaluatePolicy(mkcoa6666S0DTOSub0 cat, mkcoa6666E0DTOin req) {
        JsonNode policy = parsePolicyJson(cat.getPolicyJson());

        if ("Y".equalsIgnoreCase(nvl(cat.getOnlineForceYn(), "N"))
                || isForceActive(policy)) {
            return PolicyDecision.block("TCF-CTL-003", "SERVICE_DISABLED",
                    "온라인 강제통제 ON", 1);
        }

        if (!"Y".equalsIgnoreCase(nvl(cat.getEnabled(), "N"))) {
            return PolicyDecision.block("TCF-CTL-003", "SERVICE_DISABLED",
                    "서비스 사용중지", 2);
        }
        String status = nvl(cat.getStatus(), "NORMAL").toUpperCase(Locale.ROOT);
        if ("EMERGENCY".equals(status) || "EMERGENCY_STOP".equals(status)) {
            return PolicyDecision.block("TCF-CTL-003", "SERVICE_DISABLED",
                    "EMERGENCY 강제통제", 2);
        }
        if ("STOP".equals(status)) {
            return PolicyDecision.block("TCF-CTL-003", "SERVICE_DISABLED",
                    "서비스 STOP", 2);
        }
        if ("MAINTENANCE".equals(status) || "MAINT".equals(status)) {
            return PolicyDecision.block("TCF-CTL-004", "SERVICE_MAINTENANCE",
                    "서비스 점검중", 2);
        }

        if (!csvAllows(cat.getAllowedSystemIds(), req.getTrSysid())) {
            return PolicyDecision.block("TCF-CTL-005", "SYSTEM_NOT_ALLOWED",
                    "호출 시스템 미허용: " + req.getTrSysid(), 2);
        }

        PolicyDecision user = checkUser(policy, req);
        if (user != null) {
            return user;
        }
        PolicyDecision branch = checkBranch(policy, cat, req);
        if (branch != null) {
            return branch;
        }
        PolicyDecision terminal = checkTerminal(policy, cat, req);
        if (terminal != null) {
            return terminal;
        }
        PolicyDecision ip = checkIp(policy, req);
        if (ip != null) {
            return ip;
        }
        PolicyDecision time = checkTime(policy, cat, req);
        if (time != null) {
            return time;
        }

        String syncType = nvl(cat.getSyncType(), "S");
        if (StringUtils.hasText(req.getSyncDsc())
                && !syncType.equalsIgnoreCase(req.getSyncDsc().trim())) {
            return PolicyDecision.reject("TCF-CTL-016", "TRANSACTION_MODE_MISMATCH",
                    "sync_dsc 불일치", 7);
        }

        return PolicyDecision.allow(8);
    }

    private void applyPolicyDecision(PolicyDecision d, mkcoa6666E0DTOout out) {
        out.setControlResult(d.result);
        out.setErrorCode(d.code);
        out.setErrorName(d.name);
        out.setMessage(d.message);
        out.setCheckStep(d.step);
    }

    private PolicyDecision checkUser(JsonNode policy, mkcoa6666E0DTOin req) {
        if (policy == null || !"Y".equalsIgnoreCase(text(policy, "userCtrlUse"))) {
            if (StringUtils.hasText(req.getAuthUserId())
                    && !req.getAuthUserId().trim().equals(req.getOptrEno().trim())) {
                return PolicyDecision.reject("TCF-CTL-008", "OPERATOR_MISMATCH",
                        "인증 사용자와 optr_eno 불일치", 3);
            }
            return null;
        }
        if ("Y".equalsIgnoreCase(text(policy, "authMatchRequired"))
                && StringUtils.hasText(req.getAuthUserId())
                && !req.getAuthUserId().trim().equals(req.getOptrEno().trim())) {
            return PolicyDecision.reject("TCF-CTL-008", "OPERATOR_MISMATCH",
                    "인증 사용자와 optr_eno 불일치", 3);
        }
        String optr = req.getOptrEno();
        if (containsUser(policy.path("denyUsers"), optr)) {
            return PolicyDecision.block("TCF-CTL-008", "OPERATOR_MISMATCH",
                    "사용자 차단: " + optr, 3);
        }
        if (policy.path("allowUsers").isArray() && policy.path("allowUsers").size() > 0
                && !containsUser(policy.path("allowUsers"), optr)) {
            String def = text(policy, "userDefaultPolicy");
            if (!"ALLOW_AUTH".equalsIgnoreCase(def) && !"ALLOW_ALL".equalsIgnoreCase(def)) {
                return PolicyDecision.block("TCF-CTL-008", "OPERATOR_MISMATCH",
                        "허용 사용자 목록에 없음: " + optr, 3);
            }
        }
        return null;
    }

    private PolicyDecision checkBranch(JsonNode policy, mkcoa6666S0DTOSub0 cat, mkcoa6666E0DTOin req) {
        String brc = req.getTrBrc();
        if (policy != null && containsCode(policy.path("denyBranches"), brc)) {
            return PolicyDecision.block("TCF-CTL-007", "BRANCH_NOT_ALLOWED",
                    "차단지점: " + brc, 4);
        }
        String mode = policy == null ? "ALL" : text(policy, "branchMode");
        if ("LIST".equalsIgnoreCase(mode) || "OWN_AND_ALLOW".equalsIgnoreCase(mode)) {
            if (policy != null && policy.path("allowBranches").isArray()
                    && policy.path("allowBranches").size() > 0
                    && !containsCode(policy.path("allowBranches"), brc)) {
                return PolicyDecision.block("TCF-CTL-007", "BRANCH_NOT_ALLOWED",
                        "허용지점 아님: " + brc, 4);
            }
        }
        if (!csvAllows(cat.getAllowedBranches(), brc)) {
            return PolicyDecision.block("TCF-CTL-007", "BRANCH_NOT_ALLOWED",
                    "지점 미허용: " + brc, 4);
        }
        return null;
    }

    private PolicyDecision checkTerminal(JsonNode policy, mkcoa6666S0DTOSub0 cat, mkcoa6666E0DTOin req) {
        String trm = req.getTrmKdc();
        if (policy != null && policy.has("terminalTypes")) {
            JsonNode types = policy.path("terminalTypes");
            if (types.has(trm)) {
                if (!"Y".equalsIgnoreCase(types.path(trm).asText("N"))) {
                    return PolicyDecision.block("TCF-CTL-006", "TERMINAL_NOT_ALLOWED",
                            "단말종류 차단: " + trm, 5);
                }
                return null;
            }
            String unknown = text(policy, "unknownTerminalAction");
            if ("BLOCK".equalsIgnoreCase(unknown)) {
                return PolicyDecision.block("TCF-CTL-006", "TERMINAL_NOT_ALLOWED",
                        "미등록 단말종류: " + trm, 5);
            }
        }
        if (!csvAllows(cat.getAllowedTerminalTypes(), trm)) {
            return PolicyDecision.block("TCF-CTL-006", "TERMINAL_NOT_ALLOWED",
                    "단말종류 미허용: " + trm, 5);
        }
        return null;
    }

    private PolicyDecision checkIp(JsonNode policy, mkcoa6666E0DTOin req) {
        String ip = req.getTrTrmIpadr();
        if (!StringUtils.hasText(ip)) {
            return PolicyDecision.reject("TCF-CTL-015", "INVALID_TERMINAL_IP", "IP 누락", 6);
        }
        if (isLoopback(ip)) {
            String loop = policy == null ? "DEV_ONLY" : text(policy, "loopbackPolicy");
            if ("DENY".equalsIgnoreCase(loop)) {
                return PolicyDecision.block("TCF-CTL-015", "INVALID_TERMINAL_IP",
                        "루프백 IP 차단", 6);
            }
            return null;
        }
        if (policy == null || !"Y".equalsIgnoreCase(text(policy, "ipCtrlUse"))) {
            return null;
        }
        if (inIpList(policy.path("denyIps"), ip)) {
            return PolicyDecision.block("TCF-CTL-015", "INVALID_TERMINAL_IP",
                    "차단 IP: " + ip, 6);
        }
        JsonNode allow = policy.path("allowIps");
        if (allow.isArray() && allow.size() > 0 && !inIpList(allow, ip)) {
            String unknown = text(policy, "unknownIpAction");
            if (!"ALLOW".equalsIgnoreCase(unknown)) {
                return PolicyDecision.block("TCF-CTL-015", "INVALID_TERMINAL_IP",
                        "미등록 IP: " + ip, 6);
            }
        }
        return null;
    }

    private PolicyDecision checkTime(JsonNode policy, mkcoa6666S0DTOSub0 cat, mkcoa6666E0DTOin req) {
        String trDtm = req.getTrDtm();
        if (!StringUtils.hasText(trDtm) || trDtm.length() < 12) {
            return PolicyDecision.reject("TCF-CTL-014", "INVALID_TRANSACTION_TIME",
                    "tr_dtm 형식 오류", 7);
        }
        String hhmm = trDtm.substring(8, 12);
        if (policy != null && policy.has("blockWindows")) {
            for (JsonNode w : policy.path("blockWindows")) {
                if (inWindow(hhmm, text(w, "start"), text(w, "end"))) {
                    return PolicyDecision.block("TCF-CTL-014", "INVALID_TRANSACTION_TIME",
                            "특정 차단시간: " + text(w, "reason"), 7);
                }
            }
        }
        if (policy != null && policy.has("timeWindows")) {
            JsonNode wd = policy.path("timeWindows").path("weekday");
            if (wd.isObject() && "N".equalsIgnoreCase(text(wd, "allow"))) {
                return PolicyDecision.block("TCF-CTL-014", "INVALID_TRANSACTION_TIME",
                        "평일 거래 비허용", 7);
            }
            if (wd.isObject() && "Y".equalsIgnoreCase(text(wd, "allow"))
                    && !inWindow(hhmm, text(wd, "start"), text(wd, "end"))) {
                String action = text(policy, "outOfHoursAction");
                if (!"ALLOW".equalsIgnoreCase(action)) {
                    return PolicyDecision.block("TCF-CTL-014", "INVALID_TRANSACTION_TIME",
                            "거래 가능시간 외: " + hhmm, 7);
                }
            }
            return null;
        }
        if (!withinSimple(hhmm, cat.getAllowedStartTime(), cat.getAllowedEndTime())) {
            return PolicyDecision.block("TCF-CTL-014", "INVALID_TRANSACTION_TIME",
                    "거래 가능시간 외: " + hhmm, 7);
        }
        return null;
    }

    private boolean isForceActive(JsonNode policy) {
        if (policy == null) {
            return false;
        }
        return "Y".equalsIgnoreCase(text(policy.path("onlineForce"), "active"));
    }

    private boolean containsUser(JsonNode arr, String userId) {
        if (arr == null || !arr.isArray() || !StringUtils.hasText(userId)) {
            return false;
        }
        for (JsonNode n : arr) {
            if (userId.equalsIgnoreCase(text(n, "userId"))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCode(JsonNode arr, String code) {
        if (arr == null || !arr.isArray() || !StringUtils.hasText(code)) {
            return false;
        }
        for (JsonNode n : arr) {
            if (code.equalsIgnoreCase(text(n, "code"))) {
                return true;
            }
        }
        return false;
    }

    private boolean inIpList(JsonNode arr, String ip) {
        if (arr == null || !arr.isArray()) {
            return false;
        }
        for (JsonNode n : arr) {
            String item = n.isTextual() ? n.asText() : text(n, "ip");
            if (!StringUtils.hasText(item)) {
                continue;
            }
            if (item.contains("/")) {
                if (matchCidr(ip, item)) {
                    return true;
                }
            } else if (ip.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            int prefix = Integer.parseInt(parts[1]);
            long ipNum = ipv4(ip);
            long net = ipv4(parts[0]);
            long mask = prefix == 0 ? 0 : 0xFFFFFFFFL << (32 - prefix);
            return (ipNum & mask) == (net & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipv4(String ip) {
        String[] p = ip.split("\\.");
        return (Long.parseLong(p[0]) << 24)
                + (Long.parseLong(p[1]) << 16)
                + (Long.parseLong(p[2]) << 8)
                + Long.parseLong(p[3]);
    }

    private boolean inWindow(String hhmm, String start, String end) {
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            return false;
        }
        int cur = Integer.parseInt(hhmm);
        int from = Integer.parseInt(normalizeHHmm(start));
        int to = "2400".equals(normalizeHHmm(end)) ? 2400 : Integer.parseInt(normalizeHHmm(end));
        if (from <= to) {
            return cur >= from && (to == 2400 || cur <= to);
        }
        return cur >= from || cur <= to;
    }

    private boolean withinSimple(String hhmm, String start, String end) {
        String s = normalizeHHmm(StringUtils.hasText(start) ? start : "0000");
        String e = normalizeHHmm(StringUtils.hasText(end) ? end : "2400");
        int cur = Integer.parseInt(hhmm);
        int from = Integer.parseInt(s);
        int to = "2400".equals(e) ? 2400 : Integer.parseInt(e);
        if (from <= to) {
            return cur >= from && (to == 2400 || cur < to);
        }
        return cur >= from || cur < to;
    }

    private String normalizeHHmm(String value) {
        String v = value.replace(":", "").trim();
        if (v.length() == 3) {
            v = "0" + v;
        }
        return v.length() == 4 ? v : "0000";
    }

    private boolean csvAllows(String csv, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String list = nvl(csv, "*").trim();
        if ("*".equals(list)) {
            return true;
        }
        for (String token : list.split(",")) {
            if (value.trim().equalsIgnoreCase(token.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }

    private JsonNode parsePolicyJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? "" : v.asText("");
    }

    private static final class PolicyDecision {
        private final String result;
        private final String code;
        private final String name;
        private final String message;
        private final int step;

        private PolicyDecision(String result, String code, String name, String message, int step) {
            this.result = result;
            this.code = code;
            this.name = name;
            this.message = message;
            this.step = step;
        }

        private static PolicyDecision allow(int step) {
            return new PolicyDecision("ALLOW", null, null, "ALLOW", step);
        }

        private static PolicyDecision block(String code, String name, String message, int step) {
            return new PolicyDecision("BLOCK", code, name, message, step);
        }

        private static PolicyDecision reject(String code, String name, String message, int step) {
            return new PolicyDecision("REJECT", code, name, message, step);
        }
    }
}
