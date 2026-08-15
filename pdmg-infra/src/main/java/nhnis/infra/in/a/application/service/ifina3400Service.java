package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;

import nhnis.infra.in.a.dto.ifina3400V0DTOin;
import nhnis.infra.in.a.dto.ifina3400V0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina1999DAO;
import nhnis.infra.in.a.persistence.dao.ifina3110DAO;

/**
 * INF-340 일괄등록 검증(V0) / 반영(C0).
 * 파일럿 자산 테이블(TB_IF_SERVER_PILOT)에 적재.
 */
@Service
public class ifina3400Service {

    private final ifina1999DAO ifina1999DAO;
    private final ifina3110DAO ifina3110DAO;
    private final AuthGuard authGuard;

    public ifina3400Service(ifina1999DAO ifina1999DAO, ifina3110DAO ifina3110DAO, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.ifina1999DAO = ifina1999DAO;
        this.ifina3110DAO = ifina3110DAO;
    }

    public ifina3400V0DTOout ifina3400V0(ifina3400V0DTOin input) throws Exception {
        return validate(input == null ? null : input.getRows());
    }

    public ifina3400V0DTOout ifina3400C0(ifina3400V0DTOin input) throws Exception {
        ifina3400V0DTOout __raciOut = new ifina3400V0DTOout();
        if (authGuard.denyIfHard(__raciOut, "ifina3400C0")) return __raciOut;
        ifina3400V0DTOout validated = validate(input == null ? null : input.getRows());
        String mode = input == null || input.getApplyMode() == null ? "okOnly" : input.getApplyMode().trim();
        boolean allOrNothing = "allOrNothing".equalsIgnoreCase(mode);

        if (allOrNothing && validated.getErrorCount() != null && validated.getErrorCount() > 0) {
            validated.setPROC_CNT(0);
            validated.setRSLT_CD("0005");
            validated.setRSLT_MSG("allOrNothing: HARD 오류 " + validated.getErrorCount() + "건");
            return validated;
        }

        int inserted = 0;
        for (Map<String, Object> row : validated.getOkRows()) {
            Map<String, Object> param = new HashMap<>();
            param.put("serverId", str(row, "serverId"));
            param.put("serverName", str(row, "serverName"));
            param.put("techRole", blank(str(row, "techRole"), "OTHER"));
            param.put("envCd", blank(str(row, "envCd"), "DEV"));
            param.put("tierCd", blank(str(row, "tierCd"), "TIER3"));
            param.put("statusCd", blank(str(row, "statusCd"), "DISCOVERED"));
            param.put("remark", blank(str(row, "remark"), "bulk"));
            param.put("regUserId", "BULK");
            param.put("regDtm", now());
            inserted += ifina1999DAO.ifina1999C0_C0(param);
        }
        validated.setPROC_CNT(inserted);
        validated.setRSLT_CD("0000");
        validated.setRSLT_MSG("OK inserted=" + inserted);
        return validated;
    }

    private ifina3400V0DTOout validate(List<Map<String, Object>> rows) throws Exception {
        ifina3400V0DTOout out = new ifina3400V0DTOout();
        List<Map<String, Object>> ok = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        if (rows == null || rows.isEmpty()) {
            out.setOkCount(0);
            out.setErrorCount(0);
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_ROWS");
            return out;
        }

        int idx = 0;
        for (Map<String, Object> raw : rows) {
            idx++;
            Map<String, Object> row = raw == null ? Map.of() : raw;
            String serverId = str(row, "serverId", "SERVER_ID");
            String serverName = str(row, "serverName", "SERVER_NAME");
            String groupId = str(row, "groupId", "GROUP_ID");

            if (serverId == null || serverName == null) {
                errors.add(err(idx, "E_REQ", "serverId/serverName 필수"));
                continue;
            }
            if (!seenIds.add(serverId)) {
                errors.add(err(idx, "E_DUP_ID", "파일内 중복 Asset ID: " + serverId));
                continue;
            }
            if (ifina1999DAO.ifina1999S0_S0_exists(Map.of("serverId", serverId)) > 0) {
                errors.add(err(idx, "E_DUP_ID", "DB 중복 Asset ID: " + serverId));
                continue;
            }
            if (groupId != null && !groupId.isBlank()
                    && ifina3110DAO.ifina3110S0_S0_exists(Map.of("groupId", groupId)) <= 0) {
                errors.add(err(idx, "E_FK_GROUP", "서버군 없음: " + groupId));
                continue;
            }

            Map<String, Object> okRow = new HashMap<>();
            okRow.put("serverId", serverId);
            okRow.put("serverName", serverName);
            okRow.put("techRole", str(row, "techRole", "TECH_ROLE"));
            okRow.put("envCd", str(row, "envCd", "ENV_CD"));
            okRow.put("tierCd", str(row, "tierCd", "TIER_CD"));
            okRow.put("statusCd", str(row, "statusCd", "STATUS_CD"));
            okRow.put("remark", str(row, "remark", "REMARK"));
            okRow.put("groupId", groupId);
            okRow.put("row", idx);
            ok.add(okRow);
        }

        out.setOkRows(ok);
        out.setErrors(errors);
        out.setOkCount(ok.size());
        out.setErrorCount(errors.size());
        out.setPROC_CNT(0);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("validated ok=" + ok.size() + " err=" + errors.size());
        return out;
    }

    private static Map<String, Object> err(int row, String code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("row", row);
        m.put("code", code);
        m.put("ruleId", "RL-UP-" + code);
        m.put("message", message);
        return m;
    }

    private static String str(Map<String, Object> row, String... keys) {
        for (String k : keys) {
            Object v = row.get(k);
            if (v == null) {
                for (Map.Entry<String, Object> e : row.entrySet()) {
                    if (e.getKey() != null && e.getKey().equalsIgnoreCase(k)) {
                        v = e.getValue();
                        break;
                    }
                }
            }
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    private static String blank(String v, String d) { return v == null || v.isBlank() ? d : v; }
    private static String now() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
    }
}
