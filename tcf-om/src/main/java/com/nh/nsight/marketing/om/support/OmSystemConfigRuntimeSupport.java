package com.nh.nsight.marketing.om.support;

import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tomcat WAR / Spring bootRun 배포에 따라 환경설정 화면에 표시할 런타임 값을 구성한다.
 */
@Component
public class OmSystemConfigRuntimeSupport {
    private static final String MODE_TOMCAT = "tomcat";
    private static final String MODE_SPRING = "spring";

    private final Environment environment;
    private final DataSource dataSource;

    public OmSystemConfigRuntimeSupport(Environment environment, DataSource dataSource) {
        this.environment = environment;
        this.dataSource = dataSource;
    }

    public String resolveDeploymentMode() {
        for (String profile : environment.getActiveProfiles()) {
            if ("dev".equals(profile) || "prod".equals(profile)) {
                return MODE_TOMCAT;
            }
        }
        return MODE_SPRING;
    }

    public List<Map<String, Object>> buildRuntimeRows(String deploymentMode) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (MODE_TOMCAT.equals(deploymentMode)) {
            rows.addAll(buildTomcatDeploymentRows());
        } else {
            rows.addAll(buildSpringDeploymentRows());
        }
        rows.addAll(buildSharedRuntimeRows());
        return rows;
    }

    public boolean includeSeedRow(String configKey, String configCategory, String deploymentMode) {
        if (MODE_TOMCAT.equals(deploymentMode)) {
            if ("server.port".equals(configKey)) {
                return false;
            }
            if ("tomcat".equals(configCategory)) {
                return false;
            }
            return true;
        }
        if ("tomcat".equals(configCategory)) {
            return false;
        }
        return true;
    }

    private List<Map<String, Object>> buildTomcatDeploymentRows() {
        String gateway = prop("nsight.gateway.base-url", "http://localhost:8080");
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("deployment", "deployment.mode", "tomcat", "Tomcat WAR 배포 (외부 Tomcat 컨테이너)"));
        rows.add(row("deployment", "spring.profiles.active", activeProfiles(), "활성 Spring 프로파일"));
        rows.add(row("gateway", "gateway.base-url", gateway, "ztomcat 게이트웨이 URL"));
        rows.add(row("gateway", "om.context-path", "/om", "tcf-om WAR context path"));
        rows.add(row("gateway", "om.access-url", gateway + "/om", "tcf-om 접근 URL"));
        rows.add(row("gateway", "ui.access-url", gateway + "/ui", "tcf-ui 접근 URL"));
        rows.add(row("gateway", "batch.access-url", gateway + "/batch", "tcf-batch 접근 URL"));
        rows.add(row("deployment", "war.name", "om.war", "배포 WAR 파일명"));
        rows.add(row("deployment", "catalina.home", System.getenv().getOrDefault("CATALINA_HOME", "-"),
                "Tomcat CATALINA_HOME (setenv.bat 적용)"));
        rows.add(row("deployment", "nsight.txlog.path", prop("nsight.txlog.path", "-"),
                "공유 H2 경로 (tcf-om·tcf-batch·거래로그)"));
        return rows;
    }

    private List<Map<String, Object>> buildSpringDeploymentRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        String port = prop("server.port", "8097");
        String contextPath = prop("server.servlet.context-path", "/");
        rows.add(row("deployment", "deployment.mode", "spring", "Spring bootRun (내장 Tomcat)"));
        rows.add(row("deployment", "spring.profiles.active", activeProfiles(), "활성 Spring 프로파일"));
        rows.add(row("local", "server.port", port, "tcf-om bootRun 포트"));
        rows.add(row("local", "server.servlet.context-path", contextPath, "Servlet context path"));
        rows.add(row("local", "om.access-url", "http://127.0.0.1:" + port + normalizeContext(contextPath),
                "tcf-om 접근 URL"));
        rows.add(row("local", "ui.access-url", "http://127.0.0.1:8099", "tcf-ui bootRun 기본 URL"));
        rows.add(row("local", "batch.access-url", prop("nsight.om.batch-service-url", "http://127.0.0.1:8098/batch"),
                "tcf-batch bootRun URL"));
        rows.add(row("deployment", "nsight.txlog.path", prop("nsight.txlog.path", "-"),
                "공유 H2 경로 (tcf-om·tcf-batch·거래로그)"));
        return rows;
    }

    private List<Map<String, Object>> buildSharedRuntimeRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("application", "spring.application.name", prop("spring.application.name", "nsight-tcf-om"),
                "Spring 애플리케이션명"));
        rows.add(row("application", "nsight.om.batch-service-url",
                prop("nsight.om.batch-service-url", "-"), "OM → tcf-batch 연동 URL"));
        rows.add(row("application", "nsight.timeout.online-transaction-seconds",
                prop("nsight.timeout.online-transaction-seconds", "-"), "온라인 거래 Timeout(초)"));
        rows.add(row("application", "nsight.timeout.db-query-seconds",
                prop("nsight.timeout.db-query-seconds", "-"), "DB 조회 Timeout(초)"));
        rows.add(row("application", "nsight.tcf.transaction-log-enabled",
                prop("nsight.tcf.transaction-log-enabled", "-"), "TCF 거래로그 적재 여부"));
        rows.add(row("hikari", "spring.datasource.hikari.pool-name",
                prop("spring.datasource.hikari.pool-name", "-"), "Hikari Pool 이름"));
        rows.add(row("hikari", "spring.datasource.hikari.maximum-pool-size",
                String.valueOf(resolveHikariMaxPool()), "Hikari 최대 Pool 크기 (런타임)"));
        rows.add(row("hikari", "spring.datasource.hikari.minimum-idle",
                prop("spring.datasource.hikari.minimum-idle", "-"), "Hikari 최소 유휴 연결"));
        rows.add(row("hikari", "spring.datasource.url", maskJdbcUrl(prop("spring.datasource.url", "-")),
                "DataSource JDBC URL (비밀번호 마스킹)"));
        rows.add(row("mybatis", "mybatis.configuration.default-statement-timeout",
                prop("mybatis.configuration.default-statement-timeout", "-"), "MyBatis 기본 Timeout(초)"));
        rows.add(row("mybatis", "mybatis.configuration.default-fetch-size",
                prop("mybatis.configuration.default-fetch-size", "-"), "MyBatis 기본 Fetch Size"));
        return rows;
    }

    private int resolveHikariMaxPool() {
        if (dataSource instanceof HikariDataSource hikari) {
            return hikari.getMaximumPoolSize();
        }
        String configured = prop("spring.datasource.hikari.maximum-pool-size", "10");
        try {
            return Integer.parseInt(configured);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "(default)";
        }
        return String.join(", ", profiles);
    }

    private String prop(String key, String defaultValue) {
        String value = environment.getProperty(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String normalizeContext(String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    }

    private String maskJdbcUrl(String url) {
        if (!StringUtils.hasText(url) || "-".equals(url)) {
            return url;
        }
        return url.replaceAll("(?i)(password=)[^;&]*", "$1****");
    }

    private Map<String, Object> row(String category, String key, String value, String description) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("configCategory", category);
        row.put("configKey", key);
        row.put("configValue", value);
        row.put("editableYn", "N");
        row.put("description", description);
        row.put("runtime", true);
        return row;
    }
}
