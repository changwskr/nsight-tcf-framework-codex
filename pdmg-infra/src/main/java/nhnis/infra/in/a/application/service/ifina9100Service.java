package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;

import nhnis.infra.in.a.dto.ifina9100S0DTOSub0;
import nhnis.infra.in.a.dto.ifina9100S0DTOin;
import nhnis.infra.in.a.dto.ifina9100S0DTOout;
import nhnis.infra.in.a.dto.ifina9100U0DTOin;
import nhnis.infra.in.a.dto.ifina9100U0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina9100DAO;

@Service
public class ifina9100Service {

    private final ifina9100DAO ifina9100DAO;
    private final AuthGuard authGuard;

    public ifina9100Service(ifina9100DAO ifina9100DAO, AuthGuard authGuard) {
        this.authGuard = authGuard;
        this.ifina9100DAO = ifina9100DAO;
    }

    public ifina9100S0DTOout ifina9100S0(ifina9100S0DTOin input) throws Exception {
        String targetType = blank(input == null ? null : input.getTargetType(), "ASSET");
        String targetId = blank(input == null ? null : input.getTargetId(), "INF-APP-001");
        Map<String, Object> param = Map.of("targetType", targetType, "targetId", targetId);
        List<Map<String, Object>> rows = ifina9100DAO.ifina9100S0_S0(param);

        ifina9100S0DTOout out = new ifina9100S0DTOout();
        out.setTargetType(targetType);
        out.setTargetId(targetId);
        int checked = 0;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                ifina9100S0DTOSub0 sub = new ifina9100S0DTOSub0();
                sub.setChecklistId(as(row, "CHECKLIST_ID", "checklistId"));
                sub.setItemName(as(row, "ITEM_NAME", "itemName"));
                sub.setSeverityCd(as(row, "SEVERITY_CD", "severityCd"));
                sub.setSortNo(asInt(row, "SORT_NO", "sortNo"));
                String yn = blank(as(row, "CHECKED_YN", "checkedYn"), "N");
                sub.setCheckedYn(yn);
                sub.setRemark(as(row, "REMARK", "remark"));
                if ("Y".equalsIgnoreCase(yn)) {
                    checked++;
                }
                out.addifina9100S0DTOSub0(sub);
            }
        }
        int total = out.sizeifina9100S0DTOSub0();
        out.setSize(total);
        out.setCheckedCount(checked);
        out.setTotalItems(total);
        out.setProgressPct(total == 0 ? 0 : (int) Math.round(checked * 100.0 / total));
        return out;
    }

    public ifina9100U0DTOout ifina9100U0(ifina9100U0DTOin input) throws Exception {
        ifina9100U0DTOout out = new ifina9100U0DTOout();
        if (authGuard.denyIfHard(out, "ifina9100U0")) return out;
        String targetType = trim(input == null ? null : input.getTargetType());
        String targetId = trim(input == null ? null : input.getTargetId());
        if (targetType == null || targetId == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: targetType, targetId");
            return out;
        }
        if (input.getItems() == null || input.getItems().isEmpty()) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("NO_ITEMS");
            return out;
        }
        int cnt = 0;
        String dtm = now();
        for (Map<String, Object> item : input.getItems()) {
            if (item == null) continue;
            String checklistId = str(item, "checklistId", "CHECKLIST_ID");
            if (checklistId == null) continue;
            Map<String, Object> param = new HashMap<>();
            param.put("checklistId", checklistId);
            param.put("targetType", targetType);
            param.put("targetId", targetId);
            param.put("checkedYn", blank(str(item, "checkedYn", "CHECKED_YN"), "N"));
            param.put("remark", blank(str(item, "remark", "REMARK"), ""));
            param.put("chgUserId", "LOCAL");
            param.put("chgDtm", dtm);
            if (ifina9100DAO.ifina9100U0_U0_exists(param) > 0) {
                cnt += ifina9100DAO.ifina9100U0_U0_update(param);
            } else {
                cnt += ifina9100DAO.ifina9100U0_U0_insert(param);
            }
        }
        ifina9100S0DTOin probe = new ifina9100S0DTOin();
        probe.setTargetType(targetType);
        probe.setTargetId(targetId);
        ifina9100S0DTOout refreshed = ifina9100S0(probe);
        out.setPROC_CNT(cnt);
        out.setProgressPct(refreshed.getProgressPct());
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static String now() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
    }
    private static String trim(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
    private static String blank(String v, String d) {
        String t = trim(v);
        return t != null ? t : d;
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
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }
    private static String as(Map<String, Object> row, String u, String c) { return str(row, u, c); }
    private static Integer asInt(Map<String, Object> row, String u, String c) {
        String s = str(row, u, c);
        if (s == null) return null;
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }
}
