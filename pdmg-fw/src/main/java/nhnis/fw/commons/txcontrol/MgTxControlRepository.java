package nhnis.fw.commons.txcontrol;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * {@code TB_MG_TX_CONTROL} 조회 (OM JdbcTransactionControlRepository 대응).
 */
@Repository
public class MgTxControlRepository {

    private static final Logger log = LoggerFactory.getLogger(MgTxControlRepository.class);

    private static final String TABLE = "TB_MG_TX_CONTROL";

    private static final String GLOBAL_UNBLOCK_SQL = """
            SELECT BLOCK_YN
              FROM TB_MG_TX_CONTROL
             WHERE CONTROL_TYPE = 'GLOBAL'
               AND BLOCK_YN = 'N'
             FETCH FIRST 1 ROW ONLY
            """;

    private static final String FIND_BLOCKING_RULE_SQL = """
            SELECT CONTROL_TYPE, BLOCK_YN
              FROM TB_MG_TX_CONTROL
             WHERE BLOCK_YN = 'Y'
               AND (
                    CONTROL_TYPE = 'GLOBAL'
                 OR (CONTROL_TYPE = 'BUSINESS' AND UPPER(BUSINESS_CODE) = UPPER(?))
                 OR (CONTROL_TYPE = 'SERVICE' AND SERVICE_ID = ?)
                 OR (CONTROL_TYPE = 'CHANNEL' AND CHANNEL_ID = ?)
                 OR (CONTROL_TYPE = 'BRANCH' AND BRANCH_ID = ?)
                 OR (CONTROL_TYPE = 'USER' AND USER_ID = ?)
                 OR (CONTROL_TYPE = 'IP' AND SERVICE_NAME = ? AND ? <> '')
               )
             FETCH FIRST 1 ROW ONLY
            """;

    private final JdbcTemplate jdbcTemplate;

    public MgTxControlRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        log.info("[MgTxControl] repository ready table={}", TABLE);
    }

    public boolean isGlobalUnblockActive() {
        try {
            List<String> rows = jdbcTemplate.query(
                    GLOBAL_UNBLOCK_SQL,
                    (rs, rowNum) -> rs.getString("BLOCK_YN"));
            return rows != null && !rows.isEmpty();
        } catch (Exception e) {
            log.warn("[MgTxControl] globalUnblock 조회 실패: {}", e.toString());
            return false;
        }
    }

    public Optional<MgTxControlRule> findBlockingRule(MgTxControlRequest req) {
        if (req == null || MgTxControlExemptions.isExempt(req.getServiceId())) {
            return Optional.empty();
        }
        String businessCode = nvl(req.getBusinessCode());
        String serviceId = nvl(req.getServiceId());
        String channelId = nvl(req.getChannelId());
        String branchId = nvl(req.getBranchId());
        String userId = nvl(req.getUserId());
        String clientIp = nvl(req.getClientIp());

        try {
            List<MgTxControlRule> rules = jdbcTemplate.query(
                    FIND_BLOCKING_RULE_SQL,
                    (rs, rowNum) -> new MgTxControlRule(
                            rs.getString("CONTROL_TYPE"),
                            rs.getString("BLOCK_YN")),
                    businessCode,
                    serviceId,
                    channelId,
                    branchId,
                    userId,
                    clientIp, clientIp);
            if (rules == null || rules.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(rules.get(0));
        } catch (Exception e) {
            log.warn("[MgTxControl] blockingRule 조회 실패 serviceId={}: {}", serviceId, e.toString());
            throw e;
        }
    }

    /**
     * serviceId(예: mgcoa5530S0) 앞 2글자를 업무코드(MG)로 본다.
     * PDMG program major group 규칙.
     */
    public static String resolveBusinessCode(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return "";
        }
        String id = serviceId.trim();
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < id.length() && letters.length() < 2; i++) {
            char c = id.charAt(i);
            if (Character.isLetter(c)) {
                letters.append(c);
            } else if (letters.length() > 0) {
                break;
            }
        }
        return letters.toString().toUpperCase(Locale.ROOT);
    }

    private static String nvl(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
