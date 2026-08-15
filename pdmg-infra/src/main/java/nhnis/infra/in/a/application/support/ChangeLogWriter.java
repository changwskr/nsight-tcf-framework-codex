package nhnis.infra.in.a.application.support;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.infra.in.a.persistence.dao.ifinaAuditDAO;

/**
 * change_log 동기 기록 헬퍼.
 */
@Component
public class ChangeLogWriter {
    private final ifinaAuditDAO auditDao;
    private final ObjectMapper objectMapper;

    public ChangeLogWriter(ifinaAuditDAO auditDao, ObjectMapper objectMapper) {
        this.auditDao = auditDao;
        this.objectMapper = objectMapper;
    }

    public void write(String targetTypeCd, String targetId, String actionCd, Object before, Object after, String remark)
            throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("logId", "CL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        p.put("targetTypeCd", targetTypeCd);
        p.put("targetId", targetId);
        p.put("actionCd", actionCd);
        p.put("beforeJson", before == null ? null : objectMapper.writeValueAsString(before));
        p.put("afterJson", after == null ? null : objectMapper.writeValueAsString(after));
        p.put("changedBy", resolveChangedBy());
        p.put("changedAt", new SimpleDateFormat("yyyyMMddHHmmss", Locale.KOREA).format(new Date()));
        p.put("remark", remark == null ? "" : remark);
        auditDao.ifinaChangeLog_insert(p);
    }

    private static String resolveChangedBy() {
        ServiceContext ctx = ServiceContextHolder.getInstance();
        if (ctx != null && ctx.getHeader() != null && ctx.getHeader().getSys_comm() != null) {
            String optr = ctx.getHeader().getSys_comm().getOptr_eno();
            if (optr != null && !optr.isBlank()) {
                return optr.trim();
            }
        }
        return "LOCAL";
    }
}
