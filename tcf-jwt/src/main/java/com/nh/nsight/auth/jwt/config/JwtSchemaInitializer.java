package com.nh.nsight.auth.jwt.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/** dev(ztomcat) 등 sql.init 미사용 환경에서 JWT 테이블을 보장한다. */
public class JwtSchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(JwtSchemaInitializer.class);
    private final JdbcTemplate jdbcTemplate;
    private final JwtSecurityProperties properties;

    public JwtSchemaInitializer(JdbcTemplate jdbcTemplate, JwtSecurityProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public void init() {
        if (!properties.isSchemaAutoInit()) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS TCF_JWT_TOKEN (
                    TOKEN_ID VARCHAR(64) NOT NULL,
                    JTI VARCHAR(100) NOT NULL,
                    ISSUER VARCHAR(100) NOT NULL,
                    USER_ID VARCHAR(50) NOT NULL,
                    BRANCH_ID VARCHAR(30),
                    CHANNEL_ID VARCHAR(30),
                    AUTH_GROUP_ID VARCHAR(50),
                    TOKEN_TYPE VARCHAR(20) NOT NULL,
                    AUDIENCE VARCHAR(100),
                    ISSUED_AT TIMESTAMP NOT NULL,
                    EXPIRES_AT TIMESTAMP NOT NULL,
                    REVOKED_YN CHAR(1) DEFAULT 'N',
                    REVOKED_AT TIMESTAMP,
                    REVOKE_REASON VARCHAR(200),
                    CLIENT_IP VARCHAR(50),
                    USER_AGENT VARCHAR(500),
                    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (TOKEN_ID),
                    CONSTRAINT TCF_JWT_TOKEN_UK1 UNIQUE (ISSUER, JTI)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_TCF_JWT_TOKEN_USER ON TCF_JWT_TOKEN (USER_ID, EXPIRES_AT)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_TCF_JWT_TOKEN_REVOKED ON TCF_JWT_TOKEN (JTI, ISSUER, REVOKED_YN)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS TCF_REFRESH_TOKEN (
                    REFRESH_TOKEN_ID VARCHAR(64) NOT NULL,
                    USER_ID VARCHAR(50) NOT NULL,
                    TOKEN_HASH VARCHAR(256) NOT NULL,
                    TOKEN_FAMILY_ID VARCHAR(64),
                    ISSUED_AT TIMESTAMP NOT NULL,
                    EXPIRES_AT TIMESTAMP NOT NULL,
                    LAST_USED_AT TIMESTAMP,
                    ROTATED_YN CHAR(1) DEFAULT 'N',
                    REVOKED_YN CHAR(1) DEFAULT 'N',
                    REVOKED_AT TIMESTAMP,
                    CLIENT_IP VARCHAR(50),
                    USER_AGENT VARCHAR(500),
                    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (REFRESH_TOKEN_ID)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_TCF_REFRESH_TOKEN_USER ON TCF_REFRESH_TOKEN (USER_ID, EXPIRES_AT)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS TCF_TOKEN_DENYLIST (
                    ISSUER VARCHAR(100) NOT NULL,
                    JTI VARCHAR(100) NOT NULL,
                    USER_ID VARCHAR(50),
                    EXPIRES_AT TIMESTAMP NOT NULL,
                    REVOKED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    REVOKE_REASON VARCHAR(200),
                    PRIMARY KEY (ISSUER, JTI)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_TCF_TOKEN_DENYLIST_EXP ON TCF_TOKEN_DENYLIST (EXPIRES_AT)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS TCF_JWT_LOGIN_HISTORY (
                    LOG_ID VARCHAR(64) NOT NULL,
                    USER_ID VARCHAR(50) NOT NULL,
                    LOGIN_RESULT VARCHAR(20) NOT NULL,
                    FAIL_REASON VARCHAR(500),
                    CHANNEL_ID VARCHAR(30),
                    CLIENT_IP VARCHAR(50),
                    LOGIN_TIME TIMESTAMP NOT NULL,
                    PRIMARY KEY (LOG_ID)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS IDX_TCF_JWT_LOGIN_HIST_USER ON TCF_JWT_LOGIN_HISTORY (USER_ID, LOGIN_TIME)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS TCF_JWT_SECURITY_POLICY (
                    POLICY_ID VARCHAR(20) NOT NULL,
                    ISSUER VARCHAR(100) NOT NULL,
                    AUDIENCE VARCHAR(100) NOT NULL,
                    ACCESS_TOKEN_VALID_MINUTES INT NOT NULL,
                    REFRESH_TOKEN_VALID_HOURS INT NOT NULL,
                    ALGORITHM VARCHAR(20) NOT NULL,
                    CLOCK_SKEW_SECONDS INT NOT NULL,
                    DENYLIST_CHECK_ENABLED CHAR(1) DEFAULT 'Y',
                    REFRESH_TOKEN_ROTATION_ENABLED CHAR(1) DEFAULT 'Y',
                    UPDATED_AT TIMESTAMP,
                    UPDATED_BY VARCHAR(50),
                    PRIMARY KEY (POLICY_ID)
                )
                """);
        log.info("JWT schema initialized (TCF_JWT_TOKEN, TCF_REFRESH_TOKEN, TCF_TOKEN_DENYLIST, TCF_JWT_LOGIN_HISTORY, TCF_JWT_SECURITY_POLICY).");
    }
}
