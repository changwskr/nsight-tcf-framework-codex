package com.nh.nsight.tcf.web.support;

import com.nh.nsight.tcf.core.config.TcfProperties;
import com.nh.nsight.tcf.core.support.logging.H2DevDataSourceUrls;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/** 거래통제·거래로그 등 보조 DataSource가 primary와 동일 DB인지 판별 */
public final class TcfDataSourceUrlSupport {

    private TcfDataSourceUrlSupport() {}

    public static String resolvePrimaryUrl(Environment environment) {
        return H2DevDataSourceUrls.resolveNsightOmUrl(
                environment, environment.getProperty("spring.datasource.url"));
    }

    public static boolean isSameUrl(Environment environment, String candidateUrl) {
        if (!StringUtils.hasText(candidateUrl)) {
            return false;
        }
        return normalize(resolvePrimaryUrl(environment)).equals(normalize(candidateUrl));
    }

    public static boolean transactionControlReusesPrimary(TcfProperties properties, Environment environment) {
        return isSameUrl(environment, resolveTransactionControlUrl(properties, environment));
    }

    public static String resolveTransactionControlUrl(TcfProperties properties, Environment environment) {
        String explicit = environment.getProperty("nsight.tcf.transaction-control-datasource.url");
        if (StringUtils.hasText(explicit)) {
            return H2DevDataSourceUrls.resolveNsightOmUrl(environment, explicit.trim());
        }
        String txLog = environment.getProperty("nsight.tcf.transaction-log-datasource.url");
        if (StringUtils.hasText(txLog)) {
            return H2DevDataSourceUrls.resolveNsightOmUrl(environment, txLog.trim());
        }
        if (!properties.getTransactionLogDatasource().isSeparate()) {
            return resolvePrimaryUrl(environment);
        }
        return H2DevDataSourceUrls.resolveNsightOmUrl(environment, null);
    }

    private static String normalize(String url) {
        return url == null ? "" : url.trim().toLowerCase();
    }
}
