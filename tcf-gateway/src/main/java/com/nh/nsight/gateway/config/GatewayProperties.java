package com.nh.nsight.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nsight.gateway")
public class GatewayProperties {
    /** LOCAL / DEV / PRD — TCF_GATEWAY_ROUTE.ENV_CODE 조회 기준 */
    private String envCode = "LOCAL";
    private Auth auth = new Auth();
    private Routing routing = new Routing();
    private RouteTable routeTable = new RouteTable();
    private SessionDatasource sessionDatasource = new SessionDatasource();
    private UserSessionSync userSessionSync = new UserSessionSync();

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String envCode) {
        this.envCode = envCode;
    }

    public RouteTable getRouteTable() {
        return routeTable;
    }

    public void setRouteTable(RouteTable routeTable) {
        this.routeTable = routeTable;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Routing getRouting() {
        return routing;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
    }

    public SessionDatasource getSessionDatasource() {
        return sessionDatasource;
    }

    public void setSessionDatasource(SessionDatasource sessionDatasource) {
        this.sessionDatasource = sessionDatasource;
    }

    public UserSessionSync getUserSessionSync() {
        return userSessionSync;
    }

    public void setUserSessionSync(UserSessionSync userSessionSync) {
        this.userSessionSync = userSessionSync;
    }

    /** TCF_USER_SESSION ↔ SPRING_SESSION 주기 동기화 */
    public static class UserSessionSync {
        private boolean enabled = true;
        /** OM session-cleanup 과 동일 기본 10초 */
        private long fixedRateMs = 10_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getFixedRateMs() {
            return fixedRateMs;
        }

        public void setFixedRateMs(long fixedRateMs) {
            this.fixedRateMs = fixedRateMs;
        }
    }

    public static class Auth {
        /** true 시 JSESSIONID/NSIGHTSID 쿠키(로그인) 필수 */
        private boolean loginRequired = true;
        private SessionValidation sessionValidation = new SessionValidation();
        private Jwt jwt = new Jwt();

        public boolean isLoginRequired() {
            return loginRequired;
        }

        public void setLoginRequired(boolean loginRequired) {
            this.loginRequired = loginRequired;
        }

        public SessionValidation getSessionValidation() {
            return sessionValidation;
        }

        public void setSessionValidation(SessionValidation sessionValidation) {
            this.sessionValidation = sessionValidation;
        }

        public Jwt getJwt() {
            return jwt;
        }

        public void setJwt(Jwt jwt) {
            this.jwt = jwt;
        }
    }

    /** Bearer JWT 검증 (tcf-jwt JWKS). Authorization 헤더가 있으면 쿠키 세션 검증 대신 사용 */
    public static class Jwt {
        private boolean enabled = false;
        private String jwkSetUri;
        private String issuer = "NSIGHT-AUTH";
        private String audience = "NSIGHT-MP";
        private String headerName = "Authorization";
        private String tokenPrefix = "Bearer";
        /** true 시 header.userId vs JWT claim 불일치 시 401 */
        private boolean headerUserStrict = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public String getTokenPrefix() {
            return tokenPrefix;
        }

        public void setTokenPrefix(String tokenPrefix) {
            this.tokenPrefix = tokenPrefix;
        }

        public boolean isHeaderUserStrict() {
            return headerUserStrict;
        }

        public void setHeaderUserStrict(boolean headerUserStrict) {
            this.headerUserStrict = headerUserStrict;
        }
    }

    /** Gateway 4단계 세션 검증 (2~4단계 on/off) */
    public static class SessionValidation {
        /** 2단계: SPRING_SESSION 존재·만료 확인 */
        private boolean springSessionCheck = true;
        /** 3단계: TCF_USER_SESSION STATUS 확인 */
        private boolean userSessionCheck = true;
        /** 4단계: header.userId vs 세션 userId 비교 */
        private boolean headerUserCheck = true;
        /** true 시 불일치 즉시 401, false(기본) 시 SESSIONDB 값으로 header 보정 */
        private boolean headerUserStrict = false;

        public boolean isSpringSessionCheck() {
            return springSessionCheck;
        }

        public void setSpringSessionCheck(boolean springSessionCheck) {
            this.springSessionCheck = springSessionCheck;
        }

        public boolean isUserSessionCheck() {
            return userSessionCheck;
        }

        public void setUserSessionCheck(boolean userSessionCheck) {
            this.userSessionCheck = userSessionCheck;
        }

        public boolean isHeaderUserCheck() {
            return headerUserCheck;
        }

        public void setHeaderUserCheck(boolean headerUserCheck) {
            this.headerUserCheck = headerUserCheck;
        }

        public boolean isHeaderUserStrict() {
            return headerUserStrict;
        }

        public void setHeaderUserStrict(boolean headerUserStrict) {
            this.headerUserStrict = headerUserStrict;
        }
    }

    /** SPRING_SESSION 조회용 별도 DB (tcf-om SESSIONDB 등) */
    public static class SessionDatasource {
        private String url;
        private String username = "sa";
        private String password = "";
        private String driverClassName = "org.h2.Driver";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriverClassName() {
            return driverClassName;
        }

        public void setDriverClassName(String driverClassName) {
            this.driverClassName = driverClassName;
        }
    }

    public static class Routing {
        /** 요청 query 미지정 시 downstream 라우팅 모드 */
        private DeploymentMode deploymentMode = DeploymentMode.bootrun;
        /** tomcat 모드 downstream 베이스 URL (예: http://localhost:8080) */
        private String tomcatBaseUrl = "http://localhost:8080";
        /** bootrun 모드 downstream 호스트 (예: http://127.0.0.1) */
        private String bootrunHost = "http://127.0.0.1";
        /** true 시 query 파라미터 무시, profile routing 설정만 사용 (dev) */
        private boolean ignoreRequestParams = false;

        public DeploymentMode getDeploymentMode() {
            return deploymentMode;
        }

        public void setDeploymentMode(DeploymentMode deploymentMode) {
            this.deploymentMode = deploymentMode;
        }

        public String getTomcatBaseUrl() {
            return tomcatBaseUrl;
        }

        public void setTomcatBaseUrl(String tomcatBaseUrl) {
            this.tomcatBaseUrl = tomcatBaseUrl;
        }

        public String getBootrunHost() {
            return bootrunHost;
        }

        public void setBootrunHost(String bootrunHost) {
            this.bootrunHost = bootrunHost;
        }

        public boolean isIgnoreRequestParams() {
            return ignoreRequestParams;
        }

        public void setIgnoreRequestParams(boolean ignoreRequestParams) {
            this.ignoreRequestParams = ignoreRequestParams;
        }
    }

    public enum DeploymentMode {
        bootrun, tomcat
    }

    public static class RouteTable {
        private boolean cacheEnabled = false;
        private long cacheTtlSeconds = 30;

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public long getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(long cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
    }
}
