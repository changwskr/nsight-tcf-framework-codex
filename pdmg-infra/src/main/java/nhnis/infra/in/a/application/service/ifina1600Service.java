package nhnis.infra.in.a.application.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import nhnis.infra.in.a.application.support.ChangeLogWriter;
import nhnis.infra.in.a.application.support.EvidenceStorage;
import nhnis.infra.in.a.dto.ifina1600C0DTOin;
import nhnis.infra.in.a.dto.ifina1600C0DTOout;
import nhnis.infra.in.a.dto.ifina1600S0DTOin;
import nhnis.infra.in.a.dto.ifina1600S0DTOout;
import nhnis.infra.in.a.persistence.dao.ifinaAuditDAO;

@Service
public class ifina1600Service {
    private final ifinaAuditDAO dao;
    private final ChangeLogWriter changeLogWriter;
    private final EvidenceStorage evidenceStorage;

    public ifina1600Service(ifinaAuditDAO dao, ChangeLogWriter changeLogWriter, EvidenceStorage evidenceStorage) {
        this.dao = dao;
        this.changeLogWriter = changeLogWriter;
        this.evidenceStorage = evidenceStorage;
    }

    public ifina1600S0DTOout ifina1600S0(ifina1600S0DTOin input) throws Exception {
        String entityType = blank(input == null ? null : input.getEntityType(), "CHANGE").toUpperCase(Locale.ROOT);
        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            put(param, "keyword", input.getKeyword());
            put(param, "targetTypeCd", input.getTargetTypeCd());
            put(param, "targetId", input.getTargetId());
            put(param, "actionCd", input.getActionCd());
        }
        int pageNo = pageNo(input == null ? null : input.getPageNo());
        int pageSize = pageSize(input == null ? null : input.getPageSize());
        param.put("offset", (pageNo - 1) * pageSize);
        param.put("pageSize", pageSize);

        ifina1600S0DTOout out = new ifina1600S0DTOout();
        out.setEntityType(entityType);
        int total;
        List<Map<String, Object>> raw;
        List<Map<String, Object>> rows = new ArrayList<>();
        if ("EVIDENCE".equals(entityType)) {
            total = dao.ifina1600S0_evidence_count(param);
            raw = dao.ifina1600S0_evidence(param);
            if (raw != null) {
                for (Map<String, Object> row : raw) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("evidenceId", as(row, "EVIDENCE_ID", "evidenceId"));
                    m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
                    m.put("targetId", as(row, "TARGET_ID", "targetId"));
                    m.put("gateId", as(row, "GATE_ID", "gateId"));
                    m.put("fileName", as(row, "FILE_NAME", "fileName"));
                    m.put("fileUri", as(row, "FILE_URI", "fileUri"));
                    m.put("uploadedBy", as(row, "UPLOADED_BY", "uploadedBy"));
                    m.put("uploadedAt", as(row, "UPLOADED_AT", "uploadedAt"));
                    m.put("remark", as(row, "REMARK", "remark"));
                    rows.add(m);
                }
            }
        } else {
            total = dao.ifina1600S0_S0_count(param);
            raw = dao.ifina1600S0_S0(param);
            if (raw != null) {
                for (Map<String, Object> row : raw) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("logId", as(row, "LOG_ID", "logId"));
                    m.put("targetTypeCd", as(row, "TARGET_TYPE_CD", "targetTypeCd"));
                    m.put("targetId", as(row, "TARGET_ID", "targetId"));
                    m.put("actionCd", as(row, "ACTION_CD", "actionCd"));
                    m.put("beforeJson", as(row, "BEFORE_JSON", "beforeJson"));
                    m.put("afterJson", as(row, "AFTER_JSON", "afterJson"));
                    m.put("changedBy", as(row, "CHANGED_BY", "changedBy"));
                    m.put("changedAt", as(row, "CHANGED_AT", "changedAt"));
                    m.put("remark", as(row, "REMARK", "remark"));
                    rows.add(m);
                }
            }
        }
        out.setRows(rows);
        out.setSize(rows.size());
        out.setPageNo(pageNo);
        out.setPageSize(pageSize);
        out.setTotalCount(total);
        out.setTotalPages(pageSize <= 0 ? 0 : (int) ((total + pageSize - 1L) / pageSize));
        return out;
    }

    public ifina1600C0DTOout ifina1600C0(ifina1600C0DTOin input) throws Exception {
        ifina1600C0DTOout out = new ifina1600C0DTOout();
        String targetId = trim(input == null ? null : input.getTargetId());
        String fileName = trim(input == null ? null : input.getFileName());
        if (targetId == null || fileName == null) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0001");
            out.setRSLT_MSG("REQUIRED: targetId, fileName");
            return out;
        }
        String evidenceId = trim(input.getEvidenceId());
        if (evidenceId == null) {
            evidenceId = "EV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        }
        if (dao.ifinaEvidence_exists(Map.of("evidenceId", evidenceId)) > 0) {
            out.setPROC_CNT(0);
            out.setRSLT_CD("0002");
            out.setRSLT_MSG("DUPLICATE_EVIDENCE_ID");
            return out;
        }

        String fileUri = trim(input.getFileUri());
        Long sizeBytes = null;
        String storedName = fileName;
        String b64 = trim(input.getFileContentBase64());
        if (b64 != null) {
            try {
                byte[] bytes = decodeBase64(b64);
                EvidenceStorage.Stored stored = evidenceStorage.store(evidenceId, fileName, bytes);
                storedName = stored.fileName();
                fileUri = stored.fileUri();
                sizeBytes = stored.sizeBytes();
            } catch (IllegalArgumentException ex) {
                out.setPROC_CNT(0);
                out.setRSLT_CD("0001");
                out.setRSLT_MSG(ex.getMessage());
                return out;
            }
        }
        if (fileUri == null) {
            fileUri = "/evidence/" + evidenceId + "/" + storedName;
        }

        String targetType = blank(input.getTargetTypeCd(), "ASSET");
        Map<String, Object> p = new HashMap<>();
        p.put("evidenceId", evidenceId);
        p.put("targetTypeCd", targetType);
        p.put("targetId", targetId);
        p.put("gateId", empty(input.getGateId()));
        p.put("fileName", storedName);
        p.put("fileUri", fileUri);
        p.put("url", empty(input.getUrl()));
        p.put("uploadedBy", "LOCAL");
        p.put("uploadedAt", now());
        p.put("remark", empty(input.getRemark()));
        out.setPROC_CNT(dao.ifinaEvidence_insert(p));
        changeLogWriter.write("EVIDENCE", evidenceId, "CREATE", null, p, "ifina1600C0");
        out.setEvidenceId(evidenceId);
        out.setFileName(storedName);
        out.setFileUri(fileUri);
        out.setDownloadUri(fileUri);
        out.setSizeBytes(sizeBytes);
        out.setRSLT_CD("0000");
        out.setRSLT_MSG(sizeBytes != null ? "OK_FILE_STORED" : "OK");
        return out;
    }

    private static byte[] decodeBase64(String raw) {
        String s = raw;
        int comma = s.indexOf(',');
        if (s.startsWith("data:") && comma > 0) {
            s = s.substring(comma + 1);
        }
        s = s.replaceAll("\\s", "");
        return Base64.getDecoder().decode(s);
    }

    private static String now() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date());
    }

    private static int pageNo(Integer v) {
        return v == null || v <= 0 ? 1 : v;
    }

    private static int pageSize(Integer v) {
        int s = v == null || v <= 0 ? 20 : v;
        return Math.min(s, 100);
    }

    private static String trim(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String empty(String v) {
        return v == null ? "" : v.trim();
    }

    private static String blank(String v, String d) {
        if (v == null || v.isBlank()) {
            return d;
        }
        return v.trim();
    }

    private static void put(Map<String, Object> m, String k, String v) {
        if (v != null && !v.isBlank()) {
            m.put(k, v.trim());
        }
    }

    private static String as(Map<String, Object> row, String u, String c) {
        if (row == null) {
            return null;
        }
        Object v = row.get(u);
        if (v == null) {
            v = row.get(c);
        }
        if (v == null) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (e.getKey() != null && (e.getKey().equalsIgnoreCase(u) || e.getKey().equalsIgnoreCase(c))) {
                    v = e.getValue();
                    break;
                }
            }
        }
        return v == null ? null : String.valueOf(v);
    }
}
