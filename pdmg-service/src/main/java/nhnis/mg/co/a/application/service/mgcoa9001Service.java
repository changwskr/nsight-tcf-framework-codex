package nhnis.mg.co.a.application.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhnis.fw.exception.BizException;
import nhnis.mg.co.a.dto.mgcoa9001C0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001C0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001D0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001D0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001S0DTOSub0;
import nhnis.mg.co.a.dto.mgcoa9001S0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001S0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001U0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001U0DTOout;
import nhnis.mg.co.a.persistence.dao.mgcoa9001DAO;
import nhnis.mg.co.a.support.MgTxControlRowSupport;

/**
 * 거래통제 Service. OM TransactionControl 을 PDMG naming(mgcoa9001)으로 이식.
 * 통제유형(GLOBAL/BUSINESS/SERVICE/CHANNEL/BRANCH/USER/IP) + 대상값 → 복합키 저장.
 */
@Service
public class mgcoa9001Service {

    private static final Logger log = LoggerFactory.getLogger(mgcoa9001Service.class);
    private static final Pattern YN = Pattern.compile("^[YN]$");
    private static final Set<String> CONTROL_TYPES = Set.of(
            MgTxControlRowSupport.TYPE_GLOBAL,
            MgTxControlRowSupport.TYPE_BUSINESS,
            MgTxControlRowSupport.TYPE_SERVICE,
            MgTxControlRowSupport.TYPE_CHANNEL,
            MgTxControlRowSupport.TYPE_BRANCH,
            MgTxControlRowSupport.TYPE_USER,
            MgTxControlRowSupport.TYPE_IP
    );

    @Autowired
    private mgcoa9001DAO mgcoa9001DAO;

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgcoa9001S0DTOout mgcoa9001S0(mgcoa9001S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9001S0 Service Start!");

        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            putIfHasText(param, "controlType", normalizeControlTypeOptional(input.getControlType()));
            putIfHasText(param, "blockYn", normalizeYnOptional(input.getBlockYn()));
            putIfHasText(param, "targetKeyword", trimToNull(input.getTargetValue()));
        }

        int pageNo = input == null || input.getPageNo() == null || input.getPageNo() <= 0
                ? 1 : input.getPageNo();
        int pageSize = input == null || input.getPageSize() == null || input.getPageSize() <= 0
                ? 20 : input.getPageSize();
        if (pageSize > 100) {
            pageSize = 100;
        }
        int offset = (pageNo - 1) * pageSize;
        param.put("pageNo", pageNo);
        param.put("pageSize", pageSize);
        param.put("offset", offset);

        int totalCount = mgcoa9001DAO.mgcoa9001S0_S0_count(param);
        List<Map<String, Object>> rows = mgcoa9001DAO.mgcoa9001S0_S0(param);

        mgcoa9001S0DTOout output = new mgcoa9001S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                output.addRow(toSub(row));
            }
        }
        output.setSize(output.getRows() == null ? 0 : output.getRows().size());
        output.setPageNo(pageNo);
        output.setPageSize(pageSize);
        output.setTotalCount(totalCount);
        output.setTotalPages(pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1L) / pageSize));

        log.info("▶▶▶▶▶▶▶▶ mgcoa9001S0 Service End! - Total: " + totalCount);
        return output;
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9001C0DTOout mgcoa9001C0(mgcoa9001C0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9001C0 Service Start!");

        String controlType = requireControlType(input == null ? null : input.getControlType());
        String blockYn = requireYn(input == null ? null : input.getBlockYn(), "blockYn");
        String changeReason = requireReason(input == null ? null : input.getChangeReason());
        String targetValue = resolveTarget(controlType, input == null ? null : input.getTargetValue());

        Map<String, String> storage;
        try {
            storage = MgTxControlRowSupport.toStorageRow(controlType, targetValue, blockYn);
        } catch (IllegalArgumentException ex) {
            throw new BizException("MP0403");
        }

        Map<String, Object> param = new HashMap<>(storage);
        if (mgcoa9001DAO.mgcoa9001S0_S0_exists(param) > 0) {
            throw new BizException("MP0409");
        }

        param.put("changeReason", changeReason);
        param.put("regUserId", firstNonBlank(input == null ? null : input.getRegUserId(), "LOCAL"));
        param.put("regDtm", nowDtm());

        int cnt = mgcoa9001DAO.mgcoa9001C0_C0(param);
        mgcoa9001C0DTOout output = new mgcoa9001C0DTOout();
        output.setProcCnt(cnt);
        log.info("▶▶▶▶▶▶▶▶ mgcoa9001C0 Service End! - Total: " + cnt);
        return output;
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9001U0DTOout mgcoa9001U0(mgcoa9001U0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9001U0 Service Start!");

        Map<String, Object> keyRow = requireKeyRow(input);
        String controlType = requireControlType(input.getControlType());
        String blockYn = requireYn(input.getBlockYn(), "blockYn");
        String changeReason = requireReason(input.getChangeReason());

        if (mgcoa9001DAO.mgcoa9001S0_S0_exists(keyRow) <= 0) {
            throw new BizException("MP0404");
        }

        Map<String, Object> param = new HashMap<>(keyRow);
        param.put("controlType", controlType);
        param.put("blockYn", blockYn);
        param.put("changeReason", changeReason);
        param.put("chgUserId", firstNonBlank(input.getChgUserId(), "LOCAL"));
        param.put("chgDtm", nowDtm());

        int cnt = mgcoa9001DAO.mgcoa9001U0_U0(param);
        if (cnt <= 0) {
            throw new BizException("MP0404");
        }
        mgcoa9001U0DTOout output = new mgcoa9001U0DTOout();
        output.setProcCnt(cnt);
        log.info("▶▶▶▶▶▶▶▶ mgcoa9001U0 Service End! - Total: " + cnt);
        return output;
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9001D0DTOout mgcoa9001D0(mgcoa9001D0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9001D0 Service Start!");

        Map<String, Object> keyRow = requireKeyRow(input);
        requireReason(input == null ? null : input.getChangeReason());

        if (mgcoa9001DAO.mgcoa9001S0_S0_exists(keyRow) <= 0) {
            throw new BizException("MP0404");
        }

        int cnt = mgcoa9001DAO.mgcoa9001D0_D0(keyRow);
        if (cnt <= 0) {
            throw new BizException("MP0404");
        }
        mgcoa9001D0DTOout output = new mgcoa9001D0DTOout();
        output.setProcCnt(cnt);
        log.info("▶▶▶▶▶▶▶▶ mgcoa9001D0 Service End! - Total: " + cnt);
        return output;
    }

    private mgcoa9001S0DTOSub0 toSub(Map<String, Object> row) {
        mgcoa9001S0DTOSub0 sub = new mgcoa9001S0DTOSub0();
        sub.setServiceId(asString(row, "serviceId"));
        sub.setTransactionCode(asString(row, "transactionCode"));
        sub.setBusinessCode(asString(row, "businessCode"));
        sub.setServiceName(asString(row, "serviceName"));
        sub.setUserId(asString(row, "userId"));
        sub.setChannelId(asString(row, "channelId"));
        sub.setBranchId(asString(row, "branchId"));
        String controlType = asString(row, "controlType");
        sub.setControlType(controlType);
        sub.setBlockYn(asString(row, "blockYn"));
        sub.setTargetValue(MgTxControlRowSupport.extractTarget(controlType, row));
        sub.setChangeReason(asString(row, "changeReason"));
        sub.setRegUserId(asString(row, "regUserId"));
        sub.setRegDtm(asString(row, "regDtm"));
        sub.setChgUserId(asString(row, "chgUserId"));
        sub.setChgDtm(asString(row, "chgDtm"));
        return sub;
    }

    private Map<String, Object> requireKeyRow(mgcoa9001U0DTOin input) {
        if (input == null) {
            throw new BizException("MP0401");
        }
        Map<String, Object> key = new HashMap<>();
        key.put("serviceId", requireText(input.getServiceId(), "serviceId"));
        key.put("transactionCode", requireText(input.getTransactionCode(), "transactionCode"));
        key.put("businessCode", requireText(input.getBusinessCode(), "businessCode"));
        key.put("serviceName", requireText(input.getServiceName(), "serviceName"));
        key.put("userId", requireText(input.getUserId(), "userId"));
        key.put("channelId", requireText(input.getChannelId(), "channelId"));
        key.put("branchId", requireText(input.getBranchId(), "branchId"));
        return key;
    }

    private Map<String, Object> requireKeyRow(mgcoa9001D0DTOin input) {
        if (input == null) {
            throw new BizException("MP0401");
        }
        Map<String, Object> key = new HashMap<>();
        key.put("serviceId", requireText(input.getServiceId(), "serviceId"));
        key.put("transactionCode", requireText(input.getTransactionCode(), "transactionCode"));
        key.put("businessCode", requireText(input.getBusinessCode(), "businessCode"));
        key.put("serviceName", requireText(input.getServiceName(), "serviceName"));
        key.put("userId", requireText(input.getUserId(), "userId"));
        key.put("channelId", requireText(input.getChannelId(), "channelId"));
        key.put("branchId", requireText(input.getBranchId(), "branchId"));
        return key;
    }

    private String resolveTarget(String controlType, String targetValue) {
        if (MgTxControlRowSupport.TYPE_GLOBAL.equals(controlType)) {
            return MgTxControlRowSupport.WILDCARD;
        }
        return requireText(targetValue, "targetValue");
    }

    private String requireControlType(String value) {
        String type = requireText(value, "controlType").toUpperCase(Locale.ROOT);
        if (!CONTROL_TYPES.contains(type)) {
            throw new BizException("MP0403");
        }
        return type;
    }

    private String normalizeControlTypeOptional(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String type = trimmed.toUpperCase(Locale.ROOT);
        if (!CONTROL_TYPES.contains(type)) {
            throw new BizException("MP0403");
        }
        return type;
    }

    private String requireReason(String value) {
        String trimmed = requireText(value, "changeReason");
        if (trimmed.length() < 5) {
            throw new BizException("MP0403");
        }
        return trimmed;
    }

    private String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BizException("MP0401");
        }
        return trimmed;
    }

    private String requireYn(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BizException("MP0401");
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!YN.matcher(upper).matches()) {
            throw new BizException("MP0403");
        }
        return upper;
    }

    private String normalizeYnOptional(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (!YN.matcher(upper).matches()) {
            throw new BizException("MP0403");
        }
        return upper;
    }

    private String nowDtm() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
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

    private void putIfHasText(Map<String, Object> param, String key, String value) {
        if (value != null && !value.isBlank()) {
            param.put(key, value.trim());
        }
    }

    private String asString(Map<String, Object> row, String camelKey) {
        if (row == null) {
            return null;
        }
        Object value = row.get(camelKey);
        if (value == null) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null
                        && entry.getKey().equalsIgnoreCase(camelKey)
                        && entry.getValue() != null) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value == null ? null : String.valueOf(value);
    }
}
