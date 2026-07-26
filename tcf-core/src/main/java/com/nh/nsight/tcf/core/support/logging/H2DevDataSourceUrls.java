package com.nh.nsight.tcf.core.support.logging;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * ztomcat dev — H2 TCP(9092) 공유 DB URL 규칙.
 * TCP URL은 반드시 {@code ./nsight_om} 상대 경로를 사용해야 한다 (절대 경로·file URL 금지).
 */
public final class H2DevDataSourceUrls {

    public static final String DEV_NSIGHT_OM_TCP =
            "jdbc:h2:tcp://127.0.0.1:9092/./nsight_om;MODE=Oracle;DATABASE_TO_UPPER=false";

    private H2DevDataSourceUrls() {
    }

    public static String resolveNsightOmUrl(Environment environment, String configuredUrl) {
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            String resolved = environment.resolveRequiredPlaceholders(configuredUrl.trim());
            if (isInvalidTcpDatabasePath(resolved)) {
                return DEV_NSIGHT_OM_TCP;
            }
            if (resolved.startsWith("jdbc:h2:file:") && environment.acceptsProfiles(Profiles.of("dev"))) {
                return DEV_NSIGHT_OM_TCP;
            }
            return resolved;
        }
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            return DEV_NSIGHT_OM_TCP;
        }
        return environment.resolveRequiredPlaceholders(TcfTransactionLogConstants.DEFAULT_DATASOURCE_URL_TEMPLATE);
    }

    public static boolean isInvalidTcpDatabasePath(String url) {
        if (url == null || !url.startsWith("jdbc:h2:tcp://")) {
            return false;
        }
        int marker = url.indexOf("9092/");
        if (marker < 0) {
            return false;
        }
        String dbSegment = url.substring(marker + "9092/".length());
        return !dbSegment.startsWith("./") && !dbSegment.startsWith("~/");
    }
}
