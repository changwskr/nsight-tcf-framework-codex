package nhnis.mg.co.a.application.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhnis.mg.co.a.dto.mgcoa9000C0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000C0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000D0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000D0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000S0DTOSub0;
import nhnis.mg.co.a.dto.mgcoa9000S0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000S0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000U0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000U0DTOout;
import nhnis.mg.co.a.persistence.dao.mgcoa9000DAO;

/**
 * 거래 파라미터 관리 Service (조회/등록/수정/삭제).
 */
@Service
public class mgcoa9000Service {

    private static final Logger log = LoggerFactory.getLogger(mgcoa9000Service.class);

    @Autowired
    private mgcoa9000DAO mgcoa9000DAO;

    public mgcoa9000S0DTOout mgcoa9000S0(mgcoa9000S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9000S0 Service Start!");

        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            putIfHasText(param, "keyword", input.getKeyword());
            putIfHasText(param, "txId", input.getTxId());
            putIfHasText(param, "txName", input.getTxName());
            putIfHasText(param, "appId", input.getAppId());
            putIfHasText(param, "httpMethod", input.getHttpMethod());
        }

        int pageNo = input == null || input.getPageNo() == null || input.getPageNo() <= 0
                ? 1 : input.getPageNo();
        int pageSize = input == null || input.getPageSize() == null || input.getPageSize() <= 0
                ? 10 : input.getPageSize();
        if (pageSize > 100) {
            pageSize = 100;
        }
        int offset = (pageNo - 1) * pageSize;
        param.put("pageNo", pageNo);
        param.put("pageSize", pageSize);
        param.put("offset", offset);

        int totalCount = mgcoa9000DAO.mgcoa9000S0_S0_count(param);
        List<Map<String, Object>> rows = mgcoa9000DAO.mgcoa9000S0_S0(param);

        mgcoa9000S0DTOout output = new mgcoa9000S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                mgcoa9000S0DTOSub0 sub = new mgcoa9000S0DTOSub0();
                sub.setTxId(asString(row, "TX_ID", "txId"));
                sub.setTxName(asString(row, "TX_NAME", "txName"));
                sub.setAppId(asString(row, "APP_ID", "appId"));
                sub.setPathUrl(asString(row, "PATH_URL", "pathUrl"));
                sub.setHttpMethod(asString(row, "HTTP_METHOD", "httpMethod"));
                sub.setRegUserId(asString(row, "REG_USER_ID", "regUserId"));
                sub.setRegDtm(asString(row, "REG_DTM", "regDtm"));
                sub.setChgUserId(asString(row, "CHG_USER_ID", "chgUserId"));
                sub.setChgDtm(asString(row, "CHG_DTM", "chgDtm"));
                output.addmgcoa9000S0DTOSub0(sub);
            }
        }
        output.setSize(output.sizemgcoa9000S0DTOSub0());
        output.setPageNo(pageNo);
        output.setPageSize(pageSize);
        output.setTotalCount(totalCount);
        output.setTotalPages(pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1L) / pageSize));

        log.info("▶▶▶▶▶▶▶▶ mgcoa9000S0 Service End! - Total: " + totalCount);
        return output;
    }

    public mgcoa9000C0DTOout mgcoa9000C0(mgcoa9000C0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9000C0 Service Start!");
        mgcoa9000C0DTOout output = new mgcoa9000C0DTOout();

        String txId = trimToNull(input == null ? null : input.getTxId());
        String txName = trimToNull(input == null ? null : input.getTxName());
        if (txId == null || txName == null) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("REQUIRED: txId, txName");
            return output;
        }

        Map<String, Object> existsParam = new HashMap<>();
        existsParam.put("txId", txId);
        if (mgcoa9000DAO.mgcoa9000S0_S0_exists(existsParam) > 0) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0002");
            output.setRSLT_MSG("DUPLICATE_TX_ID");
            return output;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("txId", txId);
        param.put("txName", txName);
        param.put("appId", trimToEmpty(input.getAppId()));
        param.put("pathUrl", trimToEmpty(input.getPathUrl()));
        param.put("httpMethod", normalizeMethod(input.getHttpMethod()));
        param.put("regUserId", firstNonBlank(input.getRegUserId(), "LOCAL"));
        param.put("regDtm", nowDtm());

        int cnt = mgcoa9000DAO.mgcoa9000C0_C0(param);
        output.setPROC_CNT(cnt);
        output.setRSLT_CD("0000");
        output.setRSLT_MSG("OK");
        log.info("▶▶▶▶▶▶▶▶ mgcoa9000C0 Service End! - Total: " + cnt);
        return output;
    }

    public mgcoa9000U0DTOout mgcoa9000U0(mgcoa9000U0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9000U0 Service Start!");
        mgcoa9000U0DTOout output = new mgcoa9000U0DTOout();

        String txId = trimToNull(input == null ? null : input.getTxId());
        String txName = trimToNull(input == null ? null : input.getTxName());
        if (txId == null || txName == null) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("REQUIRED: txId, txName");
            return output;
        }

        Map<String, Object> existsParam = new HashMap<>();
        existsParam.put("txId", txId);
        if (mgcoa9000DAO.mgcoa9000S0_S0_exists(existsParam) <= 0) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0003");
            output.setRSLT_MSG("NOT_FOUND");
            return output;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("txId", txId);
        param.put("txName", txName);
        param.put("appId", trimToEmpty(input.getAppId()));
        param.put("pathUrl", trimToEmpty(input.getPathUrl()));
        param.put("httpMethod", normalizeMethod(input.getHttpMethod()));
        param.put("chgUserId", firstNonBlank(input.getChgUserId(), "LOCAL"));
        param.put("chgDtm", nowDtm());

        int cnt = mgcoa9000DAO.mgcoa9000U0_U0(param);
        output.setPROC_CNT(cnt);
        output.setRSLT_CD("0000");
        output.setRSLT_MSG("OK");
        log.info("▶▶▶▶▶▶▶▶ mgcoa9000U0 Service End! - Total: " + cnt);
        return output;
    }

    public mgcoa9000D0DTOout mgcoa9000D0(mgcoa9000D0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9000D0 Service Start!");
        mgcoa9000D0DTOout output = new mgcoa9000D0DTOout();

        if (input == null || input.getTxIdList() == null || input.getTxIdList().isEmpty()) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("NO_DATA");
            return output;
        }

        List<String> txIds = input.getTxIdList().stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (txIds.isEmpty()) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("NO_DATA");
            return output;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("txIdList", txIds);
        int cnt = mgcoa9000DAO.mgcoa9000D0_D0(param);
        output.setPROC_CNT(cnt);
        output.setRSLT_CD("0000");
        output.setRSLT_MSG("OK");
        log.info("▶▶▶▶▶▶▶▶ mgcoa9000D0 Service End! - Total: " + cnt);
        return output;
    }

    private String nowDtm() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
    }

    private String normalizeMethod(String method) {
        String value = trimToNull(method);
        return value == null ? "POST" : value.toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String primary, String fallback) {
        String value = trimToNull(primary);
        return value != null ? value : fallback;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private void putIfHasText(Map<String, Object> param, String key, String value) {
        if (value != null && !value.isBlank()) {
            param.put(key, value.trim());
        }
    }

    private String asString(Map<String, Object> row, String upperKey, String camelKey) {
        if (row == null) {
            return null;
        }
        Object value = row.get(upperKey);
        if (value == null) {
            value = row.get(camelKey);
        }
        if (value == null) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null
                        && (entry.getKey().equalsIgnoreCase(upperKey)
                        || entry.getKey().equalsIgnoreCase(camelKey))
                        && entry.getValue() != null) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value == null ? null : String.valueOf(value);
    }
}
