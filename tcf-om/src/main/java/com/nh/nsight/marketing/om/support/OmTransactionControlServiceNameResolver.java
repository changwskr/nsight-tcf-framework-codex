package com.nh.nsight.marketing.om.support;

import com.nh.nsight.tcf.core.support.control.TransactionControlServiceNameResolver;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OmTransactionControlServiceNameResolver implements TransactionControlServiceNameResolver {

    private final JdbcTemplate jdbcTemplate;

    public OmTransactionControlServiceNameResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> resolve(StandardHeader header) {
        if (header == null || !"OM".equalsIgnoreCase(header.getBusinessCode())) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(header.getServiceId()) || !StringUtils.hasText(header.getTransactionCode())) {
            return Optional.empty();
        }
        List<String> descriptions = jdbcTemplate.query("""
                SELECT DESCRIPTION
                  FROM OM_SERVICE_CATALOG
                 WHERE SERVICE_ID = ?
                   AND TRANSACTION_CODE = ?
                   AND USE_YN = 'Y'
                """,
                (rs, rowNum) -> rs.getString(1),
                header.getServiceId(),
                header.getTransactionCode());
        if (descriptions.isEmpty() || !StringUtils.hasText(descriptions.get(0))) {
            return Optional.empty();
        }
        return Optional.of(descriptions.get(0).trim());
    }
}
