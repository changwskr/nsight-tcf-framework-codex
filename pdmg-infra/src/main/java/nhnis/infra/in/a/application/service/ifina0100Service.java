package nhnis.infra.in.a.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.dto.ifina0100S0DTOin;
import nhnis.infra.in.a.dto.ifina0100S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifina1999DAO;
import nhnis.infra.in.a.persistence.dao.ifina2100DAO;
import nhnis.infra.in.a.persistence.dao.ifina2200DAO;
import nhnis.infra.in.a.persistence.dao.ifina3100DAO;
import nhnis.infra.in.a.persistence.dao.ifina3110DAO;
import nhnis.infra.in.a.persistence.dao.ifina4100DAO;
import nhnis.infra.in.a.persistence.dao.ifina4200DAO;
import nhnis.infra.in.a.persistence.dao.ifina5100DAO;
import nhnis.infra.in.a.persistence.dao.ifinaAuditDAO;

@Service
public class ifina0100Service {
    private final ifina3100DAO assetDao;
    private final ifina1999DAO pilotDao;
    private final ifina2100DAO systemDao;
    private final ifina3110DAO groupDao;
    private final ifina2200DAO appDao;
    private final ifina4100DAO mwDao;
    private final ifina4200DAO dbDao;
    private final ifina5100DAO epDao;
    private final ifinaAuditDAO auditDao;

    public ifina0100Service(
            ifina3100DAO assetDao,
            ifina1999DAO pilotDao,
            ifina2100DAO systemDao,
            ifina3110DAO groupDao,
            ifina2200DAO appDao,
            ifina4100DAO mwDao,
            ifina4200DAO dbDao,
            ifina5100DAO epDao,
            ifinaAuditDAO auditDao) {
        this.assetDao = assetDao;
        this.pilotDao = pilotDao;
        this.systemDao = systemDao;
        this.groupDao = groupDao;
        this.appDao = appDao;
        this.mwDao = mwDao;
        this.dbDao = dbDao;
        this.epDao = epDao;
        this.auditDao = auditDao;
    }

    public ifina0100S0DTOout ifina0100S0(ifina0100S0DTOin input) throws Exception {
        ifina0100S0DTOout out = new ifina0100S0DTOout();
        Map<String, Object> empty = Map.of("offset", 0, "pageSize", 1);
        out.setAssetCount(assetDao.ifina3100S0_S0_count(new HashMap<>(empty)));
        out.setPilotAssetCount(pilotDao.ifina1999S0_S0_count(new HashMap<>(empty)));
        out.setSystemCount(systemDao.ifina2100S0_S0_count(new HashMap<>(empty)));
        out.setGroupCount(groupDao.ifina3110S0_S0_count(new HashMap<>(empty)));
        out.setAppCount(appDao.ifina2200S0_S0_count(new HashMap<>(empty)));
        out.setMwCount(mwDao.ifina4100S0_S0_count(new HashMap<>(empty)));
        out.setDbCount(dbDao.ifina4200S0_S0_count(new HashMap<>(empty)));
        out.setEndpointCount(epDao.ifina5100S0_S0_count(new HashMap<>(empty)));
        out.setChangeLogCount(auditDao.ifina1600S0_S0_count(new HashMap<>(empty)));

        Map<String, Object> confirmed = new HashMap<>(empty);
        confirmed.put("statusCd", "CONFIRMED");
        out.setConfirmedCount(assetDao.ifina3100S0_S0_count(confirmed));
        Map<String, Object> retired = new HashMap<>(empty);
        retired.put("statusCd", "RETIRED");
        out.setRetiredCount(assetDao.ifina3100S0_S0_count(retired));

        int limit = input != null && input.getRecentLimit() != null && input.getRecentLimit() > 0
                ? Math.min(input.getRecentLimit(), 20) : 8;
        Map<String, Object> recentQ = new HashMap<>();
        recentQ.put("offset", 0);
        recentQ.put("pageSize", limit);
        List<Map<String, Object>> rows = assetDao.ifina3100S0_S0(recentQ);
        List<Map<String, Object>> recent = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("assetId", as(row, "ASSET_ID", "assetId"));
                m.put("assetName", as(row, "ASSET_NAME", "assetName"));
                m.put("techRoleCd", as(row, "TECH_ROLE_CD", "techRoleCd"));
                m.put("envCd", as(row, "ENV_CD", "envCd"));
                m.put("statusCd", as(row, "STATUS_CD", "statusCd"));
                m.put("systemId", as(row, "SYSTEM_ID", "systemId"));
                recent.add(m);
            }
        }
        out.setRecentAssets(recent);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG("OK");
        return out;
    }

    private static String as(Map<String, Object> row, String u, String c) {
        if (row == null) return null;
        Object v = row.get(u);
        if (v == null) v = row.get(c);
        if (v == null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && (e.getKey().equalsIgnoreCase(u) || e.getKey().equalsIgnoreCase(c))) {
                    v = e.getValue(); break;
                }
            }
        }
        return v == null ? null : String.valueOf(v);
    }
}
