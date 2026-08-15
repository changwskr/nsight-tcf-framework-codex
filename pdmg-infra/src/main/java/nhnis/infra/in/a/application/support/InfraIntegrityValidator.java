package nhnis.infra.in.a.application.support;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import nhnis.infra.in.a.persistence.dao.ifina1100DAO;
import nhnis.infra.in.a.persistence.dao.ifina1999DAO;
import nhnis.infra.in.a.persistence.dao.ifina2100DAO;
import nhnis.infra.in.a.persistence.dao.ifina3100DAO;
import nhnis.infra.in.a.persistence.dao.ifina3110DAO;
import nhnis.infra.in.a.persistence.dao.ifina5100DAO;

/**
 * 공통 Hard/Soft 정합성 검증 (RL-AS/CD/FK/NT 등).
 */
@Component
public class InfraIntegrityValidator {

    private final ifina3100DAO assetDao;
    private final ifina1999DAO pilotDao;
    private final ifina2100DAO systemDao;
    private final ifina3110DAO groupDao;
    private final ifina1100DAO codeDao;
    private final ifina5100DAO networkDao;

    public InfraIntegrityValidator(
            ifina3100DAO assetDao,
            ifina1999DAO pilotDao,
            ifina2100DAO systemDao,
            ifina3110DAO groupDao,
            ifina1100DAO codeDao,
            ifina5100DAO networkDao) {
        this.assetDao = assetDao;
        this.pilotDao = pilotDao;
        this.systemDao = systemDao;
        this.groupDao = groupDao;
        this.codeDao = codeDao;
        this.networkDao = networkDao;
    }

    public ValidationResult validateAssetCreate(
            String assetId, String assetName, String assetKindCd, String envCd, String techRoleCd,
            String systemId, String groupId) throws Exception {
        ValidationResult r = new ValidationResult();
        requireAssetRequired(r, assetId, assetName, assetKindCd, envCd, techRoleCd);
        if (r.hasHard()) {
            return r;
        }
        if (assetDao.ifina3100S0_S0_exists(Map.of("assetId", assetId.trim())) > 0) {
            r.add(RuleViolation.hard("RL-AS-001", "0002", "Asset ID 중복: " + assetId));
        }
        validateCodesAndFk(r, assetKindCd, envCd, techRoleCd, systemId, groupId);
        softBareMetalHint(r, assetKindCd);
        return r;
    }

    public ValidationResult validateAssetUpdate(
            String assetId, String assetName, String assetKindCd, String envCd, String techRoleCd,
            String systemId, String groupId) throws Exception {
        ValidationResult r = new ValidationResult();
        if (isBlank(assetId) || isBlank(assetName)) {
            r.add(RuleViolation.hard("RL-AS-002", "0001", "필수값 누락: assetId, assetName"));
            return r;
        }
        if (assetDao.ifina3100S0_S0_exists(Map.of("assetId", assetId.trim())) <= 0) {
            r.add(RuleViolation.hard("RL-AS-001", "0003", "NOT_FOUND: " + assetId));
            return r;
        }
        validateCodesAndFk(r, assetKindCd, envCd, techRoleCd, systemId, groupId);
        softBareMetalHint(r, assetKindCd);
        if (isSaasOrCloud(assetKindCd)) {
            r.add(RuleViolation.soft("RL-AS-003", "SaaS/Cloud — Compute 필드 미적용"));
        }
        return r;
    }

    public ValidationResult validateActiveCode(String codeSetId, String codeValue, String fieldLabel) throws Exception {
        ValidationResult r = new ValidationResult();
        if (isBlank(codeValue)) {
            return r;
        }
        Map<String, Object> p = new HashMap<>();
        p.put("codeSetId", codeSetId);
        p.put("codeValue", codeValue.trim());
        p.put("activeYn", "Y");
        // exists ignores activeYn; check via count with filters
        int exists = codeDao.ifina1100S0_S0_exists(Map.of("codeSetId", codeSetId, "codeValue", codeValue.trim()));
        if (exists <= 0) {
            r.add(RuleViolation.hard("RL-CD-001", "0004",
                    "유효하지 않은 코드(" + fieldLabel + "): " + codeSetId + "." + codeValue));
            return r;
        }
        Map<String, Object> q = new HashMap<>();
        q.put("codeSetId", codeSetId);
        q.put("codeValue", codeValue.trim());
        q.put("activeYn", "Y");
        q.put("offset", 0);
        q.put("pageSize", 1);
        if (codeDao.ifina1100S0_S0_count(q) <= 0) {
            r.add(RuleViolation.hard("RL-CD-001", "0004",
                    "비활성 코드(" + fieldLabel + "): " + codeSetId + "." + codeValue));
        }
        return r;
    }

    public ValidationResult validateNetworkEndpoint(
            String endpointId, String assetId, String address, String portNo, boolean create) throws Exception {
        ValidationResult r = new ValidationResult();
        if (isBlank(endpointId) || isBlank(assetId) || isBlank(address)) {
            r.add(RuleViolation.hard("RL-NT-004", "0001", "REQUIRED: endpointId, assetId, address"));
            return r;
        }
        boolean assetOk = assetDao.ifina3100S0_S0_exists(Map.of("assetId", assetId.trim())) > 0
                || pilotDao.ifina1999S0_S0_exists(Map.of("serverId", assetId.trim())) > 0;
        if (!assetOk) {
            r.add(RuleViolation.hard("RL-NT-004", "0004", "자산 없음: " + assetId));
        }
        Map<String, Object> dup = new HashMap<>();
        dup.put("address", address.trim());
        dup.put("portNo", portNo == null ? "" : portNo.trim());
        if (!create) {
            dup.put("endpointId", endpointId.trim());
        }
        if (networkDao.ifina5100S0_dupAddress(dup) > 0) {
            r.add(RuleViolation.hard("RL-NT-001", "0005", "address+port 중복"));
        }
        return r;
    }

    private void requireAssetRequired(
            ValidationResult r, String assetId, String assetName, String assetKindCd, String envCd, String techRoleCd) {
        if (isBlank(assetId) || isBlank(assetName) || isBlank(assetKindCd) || isBlank(envCd) || isBlank(techRoleCd)) {
            r.add(RuleViolation.hard("RL-AS-002", "0001",
                    "필수값 누락: assetId, assetName, assetKindCd, envCd, techRoleCd"));
        }
    }

    private void validateCodesAndFk(
            ValidationResult r, String assetKindCd, String envCd, String techRoleCd, String systemId, String groupId)
            throws Exception {
        r.addAll(validateActiveCode("ASSET_KIND", assetKindCd, "assetKindCd"));
        r.addAll(validateActiveCode("ENV", envCd, "envCd"));
        r.addAll(validateActiveCode("TECH_ROLE", techRoleCd, "techRoleCd"));
        if (!isBlank(systemId) && systemDao.ifina2100S0_S0_exists(Map.of("systemId", systemId.trim())) <= 0) {
            r.add(RuleViolation.hard("RL-AS-005", "0004", "시스템 없음: " + systemId));
        }
        if (!isBlank(groupId) && groupDao.ifina3110S0_S0_exists(Map.of("groupId", groupId.trim())) <= 0) {
            r.add(RuleViolation.hard("RL-AS-004", "0004", "서버군 없음: " + groupId));
        }
    }

    private void softBareMetalHint(ValidationResult r, String assetKindCd) {
        if ("BARE_METAL".equalsIgnoreCase(safe(assetKindCd))) {
            r.add(RuleViolation.soft("RL-BM-001", "Bare Metal — SERVICE_MODEL/RUNTIME_BASE 축 확인"));
        }
    }

    private static boolean isSaasOrCloud(String assetKindCd) {
        String v = safe(assetKindCd).toUpperCase(Locale.ROOT);
        return v.equals("SAAS") || v.startsWith("CLOUD");
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
