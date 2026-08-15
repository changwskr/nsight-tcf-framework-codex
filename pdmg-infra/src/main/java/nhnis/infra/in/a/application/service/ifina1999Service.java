package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.AuthGuard;
import nhnis.infra.in.a.dto.ifina1999C0DTOin;
import nhnis.infra.in.a.dto.ifina1999C0DTOout;
import nhnis.infra.in.a.dto.ifina1999D0DTOin;
import nhnis.infra.in.a.dto.ifina1999D0DTOout;
import nhnis.infra.in.a.dto.ifina1999E0DTOin;
import nhnis.infra.in.a.dto.ifina1999E0DTOout;
import nhnis.infra.in.a.dto.ifina1999S0DTOSub0;
import nhnis.infra.in.a.dto.ifina1999S0DTOin;
import nhnis.infra.in.a.dto.ifina1999S0DTOout;
import nhnis.infra.in.a.dto.ifina1999U0DTOin;
import nhnis.infra.in.a.dto.ifina1999U0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina1100DAO;
import nhnis.infra.in.a.persistence.dao.ifina1999DAO;
import nhnis.infra.in.a.persistence.dao.ifina2100DAO;
import nhnis.infra.in.a.persistence.dao.ifina3100DAO;
import nhnis.infra.in.a.persistence.dao.ifina3110DAO;

/**
 * 서버 인벤토리 파일럿 Service (조회/등록/수정/삭제).
 */
@Service
public class ifina1999Service {

    private static final Logger log = LoggerFactory.getLogger(ifina1999Service.class);

    @Autowired
    private ifina1999DAO ifina1999DAO;
    @Autowired
    private ifina3100DAO ifina3100DAO;
    @Autowired
    private ifina2100DAO ifina2100DAO;
    @Autowired
    private ifina3110DAO ifina3110DAO;
    @Autowired
    private ifina1100DAO ifina1100DAO;
    @Autowired
    private AuthGuard authGuard;

    public ifina1999S0DTOout ifina1999S0(ifina1999S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ ifina1999S0 Service Start!");

        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            putIfHasText(param, "keyword", input.getKeyword());
            putIfHasText(param, "serverId", input.getServerId());
            putIfHasText(param, "serverName", input.getServerName());
            putIfHasText(param, "techRole", input.getTechRole());
            putIfHasText(param, "envCd", input.getEnvCd());
            putIfHasText(param, "statusCd", input.getStatusCd());
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

        int totalCount = ifina1999DAO.ifina1999S0_S0_count(param);
        List<Map<String, Object>> rows = ifina1999DAO.ifina1999S0_S0(param);

        ifina1999S0DTOout output = new ifina1999S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                ifina1999S0DTOSub0 sub = new ifina1999S0DTOSub0();
                sub.setServerId(asString(row, "SERVER_ID", "serverId"));
                sub.setServerName(asString(row, "SERVER_NAME", "serverName"));
                sub.setTechRole(asString(row, "TECH_ROLE", "techRole"));
                sub.setEnvCd(asString(row, "ENV_CD", "envCd"));
                sub.setTierCd(asString(row, "TIER_CD", "tierCd"));
                sub.setStatusCd(asString(row, "STATUS_CD", "statusCd"));
                sub.setRemark(asString(row, "REMARK", "remark"));
                sub.setRegUserId(asString(row, "REG_USER_ID", "regUserId"));
                sub.setRegDtm(asString(row, "REG_DTM", "regDtm"));
                sub.setChgUserId(asString(row, "CHG_USER_ID", "chgUserId"));
                sub.setChgDtm(asString(row, "CHG_DTM", "chgDtm"));
                output.addifina1999S0DTOSub0(sub);
            }
        }
        output.setSize(output.sizeifina1999S0DTOSub0());
        output.setPageNo(pageNo);
        output.setPageSize(pageSize);
        output.setTotalCount(totalCount);
        output.setTotalPages(pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1L) / pageSize));

        log.info("▶▶▶▶▶▶▶▶ ifina1999S0 Service End! - Total: " + totalCount);
        return output;
    }

    public ifina1999C0DTOout ifina1999C0(ifina1999C0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ ifina1999C0 Service Start!");
        ifina1999C0DTOout output = new ifina1999C0DTOout();
        if (authGuard.denyIfHard(output, "ifina1999C0")) return output;

        String serverId = trimToNull(input == null ? null : input.getServerId());
        String serverName = trimToNull(input == null ? null : input.getServerName());
        if (serverId == null || serverName == null) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("REQUIRED: serverId, serverName");
            return output;
        }

        Map<String, Object> existsParam = new HashMap<>();
        existsParam.put("serverId", serverId);
        if (ifina1999DAO.ifina1999S0_S0_exists(existsParam) > 0) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0002");
            output.setRSLT_MSG("DUPLICATE_SERVER_ID");
            return output;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("serverId", serverId);
        param.put("serverName", serverName);
        param.put("techRole", trimToEmpty(input.getTechRole()));
        param.put("envCd", firstNonBlank(input.getEnvCd(), "DEV"));
        param.put("tierCd", firstNonBlank(input.getTierCd(), "TIER3"));
        param.put("statusCd", firstNonBlank(input.getStatusCd(), "DISCOVERED"));
        param.put("remark", trimToEmpty(input.getRemark()));
        param.put("regUserId", firstNonBlank(input.getRegUserId(), "LOCAL"));
        param.put("regDtm", nowDtm());

        int cnt = ifina1999DAO.ifina1999C0_C0(param);
        output.setPROC_CNT(cnt);
        output.setRSLT_CD("0000");
        output.setRSLT_MSG("OK");
        log.info("▶▶▶▶▶▶▶▶ ifina1999C0 Service End! - Total: " + cnt);
        return output;
    }

    public ifina1999U0DTOout ifina1999U0(ifina1999U0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ ifina1999U0 Service Start!");
        ifina1999U0DTOout output = new ifina1999U0DTOout();
        if (authGuard.denyIfHard(output, "ifina1999U0")) return output;

        String serverId = trimToNull(input == null ? null : input.getServerId());
        String serverName = trimToNull(input == null ? null : input.getServerName());
        if (serverId == null || serverName == null) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("REQUIRED: serverId, serverName");
            return output;
        }

        Map<String, Object> existsParam = new HashMap<>();
        existsParam.put("serverId", serverId);
        if (ifina1999DAO.ifina1999S0_S0_exists(existsParam) <= 0) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0003");
            output.setRSLT_MSG("NOT_FOUND");
            return output;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("serverId", serverId);
        param.put("serverName", serverName);
        param.put("techRole", trimToEmpty(input.getTechRole()));
        param.put("envCd", trimToEmpty(input.getEnvCd()));
        param.put("tierCd", trimToEmpty(input.getTierCd()));
        param.put("statusCd", trimToEmpty(input.getStatusCd()));
        param.put("remark", trimToEmpty(input.getRemark()));
        param.put("chgUserId", firstNonBlank(input.getChgUserId(), "LOCAL"));
        param.put("chgDtm", nowDtm());

        int cnt = ifina1999DAO.ifina1999U0_U0(param);
        output.setPROC_CNT(cnt);
        output.setRSLT_CD("0000");
        output.setRSLT_MSG("OK");
        log.info("▶▶▶▶▶▶▶▶ ifina1999U0 Service End! - Total: " + cnt);
        return output;
    }

    public ifina1999D0DTOout ifina1999D0(ifina1999D0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ ifina1999D0 Service Start!");
        ifina1999D0DTOout output = new ifina1999D0DTOout();
        if (authGuard.denyIfHard(output, "ifina1999D0")) return output;

        if (input == null || input.getServerIdList() == null || input.getServerIdList().isEmpty()) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("NO_DATA");
            return output;
        }

        List<String> serverIds = input.getServerIdList().stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (serverIds.isEmpty()) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("NO_DATA");
            return output;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("serverIdList", serverIds);
        int cnt = ifina1999DAO.ifina1999D0_D0(param);
        output.setPROC_CNT(cnt);
        output.setRSLT_CD("0000");
        output.setRSLT_MSG("OK");
        log.info("▶▶▶▶▶▶▶▶ ifina1999D0 Service End! - Total: " + cnt);
        return output;
    }

    /**
     * Pilot({@code TB_IF_SERVER_PILOT}) → 정규({@code TB_IF_SERVER_ASSET_PILOT}) 이관.
     */
    public ifina1999E0DTOout ifina1999E0(ifina1999E0DTOin input) throws Exception {
        ifina1999E0DTOout out = new ifina1999E0DTOout();
        boolean dryRun = "Y".equalsIgnoreCase(trimToNull(input == null ? null : input.getDryRunYn()));
        String defaultSystemId = firstNonBlank(input == null ? null : input.getDefaultSystemId(), "SYS-ONLINE");
        String defaultGroupId = trimToNull(input == null ? null : input.getDefaultGroupId());

        List<String> requested = input == null || input.getServerIdList() == null
                ? List.of()
                : input.getServerIdList().stream()
                        .filter(v -> v != null && !v.isBlank())
                        .map(String::trim)
                        .distinct()
                        .toList();

        Map<String, Object> listParam = new HashMap<>();
        listParam.put("offset", 0);
        listParam.put("pageSize", 500);
        if (requested.size() == 1) {
            listParam.put("serverId", requested.get(0));
        }
        List<Map<String, Object>> pilots = ifina1999DAO.ifina1999S0_S0(listParam);
        if (pilots == null) {
            pilots = List.of();
        }
        if (!requested.isEmpty() && requested.size() != 1) {
            pilots = pilots.stream()
                    .filter(r -> requested.contains(asString(r, "SERVER_ID", "serverId")))
                    .toList();
        }

        int migrated = 0;
        int skipped = 0;
        int errors = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Map<String, Object> row : pilots) {
            String serverId = asString(row, "SERVER_ID", "serverId");
            String serverName = asString(row, "SERVER_NAME", "serverName");
            String techRole = asString(row, "TECH_ROLE", "techRole");
            String envCd = asString(row, "ENV_CD", "envCd");
            String tierCd = asString(row, "TIER_CD", "tierCd");
            String statusCd = asString(row, "STATUS_CD", "statusCd");
            String remark = asString(row, "REMARK", "remark");
            Map<String, Object> detail = new HashMap<>();
            detail.put("serverId", serverId);

            if (serverId == null || serverName == null) {
                errors++;
                detail.put("result", "ERROR");
                detail.put("message", "REQUIRED");
                details.add(detail);
                continue;
            }
            if (ifina3100DAO.ifina3100S0_S0_exists(Map.of("assetId", serverId)) > 0) {
                skipped++;
                detail.put("result", "SKIP");
                detail.put("message", "ALREADY_MIGRATED");
                details.add(detail);
                continue;
            }
            if (techRole != null
                    && ifina1100DAO.ifina1100S0_S0_exists(Map.of("codeSetId", "TECH_ROLE", "codeValue", techRole)) <= 0) {
                errors++;
                detail.put("result", "ERROR");
                detail.put("message", "RL-CD-001 TECH_ROLE: " + techRole);
                details.add(detail);
                continue;
            }
            if (envCd != null
                    && ifina1100DAO.ifina1100S0_S0_exists(Map.of("codeSetId", "ENV", "codeValue", envCd)) <= 0) {
                errors++;
                detail.put("result", "ERROR");
                detail.put("message", "RL-CD-001 ENV: " + envCd);
                details.add(detail);
                continue;
            }

            String[] sg = resolveSystemGroup(serverId, techRole, defaultSystemId, defaultGroupId);
            String systemId = sg[0];
            String groupId = sg[1];
            if (systemId != null && ifina2100DAO.ifina2100S0_S0_exists(Map.of("systemId", systemId)) <= 0) {
                errors++;
                detail.put("result", "ERROR");
                detail.put("message", "시스템 없음: " + systemId);
                details.add(detail);
                continue;
            }
            if (groupId != null && ifina3110DAO.ifina3110S0_S0_exists(Map.of("groupId", groupId)) <= 0) {
                errors++;
                detail.put("result", "ERROR");
                detail.put("message", "서버군 없음: " + groupId);
                details.add(detail);
                continue;
            }

            if (!dryRun) {
                Map<String, Object> p = new HashMap<>();
                p.put("assetId", serverId);
                p.put("assetName", serverName);
                p.put("groupId", groupId == null ? "" : groupId);
                p.put("systemId", systemId == null ? "" : systemId);
                p.put("assetKindCd", "VM");
                p.put("techRoleCd", techRole == null ? "" : techRole);
                p.put("envCd", envCd == null ? "DEV" : envCd);
                p.put("tierCd", tierCd == null ? "TIER3" : tierCd);
                p.put("serviceModelCd", "IAAS");
                p.put("deployModelCd", "ON_PREMISE");
                p.put("statusCd", statusCd == null ? "DISCOVERED" : statusCd);
                p.put("osName", "");
                p.put("osVersion", "");
                p.put("osEolDate", "");
                p.put("remark", remark == null ? "migrated from pilot" : remark);
                p.put("regUserId", "MIGRATE");
                p.put("regDtm", nowDtm());
                p.put("chgUserId", null);
                p.put("chgDtm", null);
                ifina3100DAO.ifina3100C0_C0(p);
            }
            migrated++;
            detail.put("result", dryRun ? "DRY_OK" : "OK");
            detail.put("systemId", systemId);
            detail.put("groupId", groupId);
            details.add(detail);
        }

        out.setMigratedCount(migrated);
        out.setSkippedCount(skipped);
        out.setErrorCount(errors);
        out.setDetails(details);
        out.setPROC_CNT(migrated);
        out.setRSLT_CD(errors > 0 && migrated == 0 ? "0005" : "0000");
        out.setRSLT_MSG(dryRun ? "DRY_RUN" : "OK");
        return out;
    }

    private String[] resolveSystemGroup(String serverId, String techRole, String defaultSystemId, String defaultGroupId) {
        if ("INF-APP-001".equals(serverId)) {
            return new String[] {"SYS-ONLINE", "SG-WAS-A"};
        }
        if ("INF-DB-001".equals(serverId)) {
            return new String[] {"SYS-ONLINE", "SG-DB-CORE"};
        }
        if ("INF-WEB-001".equals(serverId)) {
            return new String[] {"SYS-ONLINE", defaultGroupId};
        }
        String role = techRole == null ? "" : techRole.toUpperCase(Locale.ROOT);
        if ("DATABASE".equals(role)) {
            return new String[] {defaultSystemId, firstNonBlank(defaultGroupId, "SG-DB-CORE")};
        }
        if ("WAS".equals(role)) {
            return new String[] {defaultSystemId, firstNonBlank(defaultGroupId, "SG-WAS-A")};
        }
        if ("BATCH".equals(role)) {
            return new String[] {"SYS-BATCH", firstNonBlank(defaultGroupId, "SG-BATCH-01")};
        }
        return new String[] {defaultSystemId, defaultGroupId};
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
