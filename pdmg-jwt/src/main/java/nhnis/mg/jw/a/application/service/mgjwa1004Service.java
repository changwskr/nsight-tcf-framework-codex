package nhnis.mg.jw.a.application.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import nhnis.mg.jw.a.config.JwtRuntimePolicy;
import nhnis.mg.jw.a.dto.mgjwa1004S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1004S0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1004U0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1004U0DTOout;
import nhnis.mg.jw.a.persistence.dao.mgjwa1004DAO;
import nhnis.fw.exception.BizException;
import nhnis.mg.jw.a.support.JwtClientContext;

/** 보안정책 조회·수정. */
@Service
@DependsOn("jwtSchemaInitializer")
public class mgjwa1004Service {

    private static final String POLICY_ID = "DEFAULT";

    private final mgjwa1004DAO dao;
    private final JwtRuntimePolicy runtimePolicy;

    public mgjwa1004Service(mgjwa1004DAO dao, JwtRuntimePolicy runtimePolicy) {
        this.dao = dao;
        this.runtimePolicy = runtimePolicy;
    }

    @PostConstruct
    void loadPolicyFromDatabase() {
        Map<String, Object> row = dao.selectSecurityPolicy(POLICY_ID);
        if (row != null && !row.isEmpty()) {
            runtimePolicy.applyFromRow(row);
        } else {
            seedDefaultPolicy("SYSTEM");
        }
    }

    public mgjwa1004S0DTOout mgjwa1004S0(mgjwa1004S0DTOin input) {
        Map<String, Object> row = dao.selectSecurityPolicy(POLICY_ID);
        if (row == null || row.isEmpty()) {
            row = new LinkedHashMap<>(runtimePolicy.toMap());
        }
        mgjwa1004S0DTOout output = new mgjwa1004S0DTOout();
        output.setBusinessCode("JW");
        output.setScreen("보안정책 관리");
        output.setPolicy(row);
        return output;
    }

    public mgjwa1004U0DTOout mgjwa1004U0(mgjwa1004U0DTOin input) {
        if (input == null) {
            throw new BizException("E-JWT-VAL-0002", "요청 본문이 필요합니다.");
        }
        int accessMinutes = input.getAccessTokenValidMinutes() != null
                ? input.getAccessTokenValidMinutes() : runtimePolicy.getAccessTokenValidMinutes();
        int refreshHours = input.getRefreshTokenValidHours() != null
                ? input.getRefreshTokenValidHours() : runtimePolicy.getRefreshTokenValidHours();
        if (accessMinutes < 1 || accessMinutes > 1440) {
            throw new BizException("E-JWT-VAL-0002", "Access Token 유효시간은 1~1440분이어야 합니다.");
        }
        if (refreshHours < 1 || refreshHours > 720) {
            throw new BizException("E-JWT-VAL-0002", "Refresh Token 유효시간은 1~720시간이어야 합니다.");
        }
        String issuer = StringUtils.hasText(input.getIssuer()) ? input.getIssuer() : runtimePolicy.getIssuer();
        String audience = StringUtils.hasText(input.getAudience()) ? input.getAudience() : runtimePolicy.getAudience();
        String algorithm = StringUtils.hasText(input.getAlgorithm()) ? input.getAlgorithm() : runtimePolicy.getAlgorithm();
        int clockSkew = input.getClockSkewSeconds() != null
                ? input.getClockSkewSeconds() : runtimePolicy.getClockSkewSeconds();
        boolean denylist = ynValue(input.getDenylistCheckEnabled(), runtimePolicy.isDenylistCheckEnabled());
        boolean rotation = ynValue(input.getRefreshTokenRotationEnabled(),
                runtimePolicy.isRefreshTokenRotationEnabled());

        String updatedBy = "SYSTEM";
        String operator = JwtClientContext.userId();
        if (StringUtils.hasText(operator)) {
            updatedBy = operator;
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("policyId", POLICY_ID);
        row.put("issuer", issuer);
        row.put("audience", audience);
        row.put("accessTokenValidMinutes", accessMinutes);
        row.put("refreshTokenValidHours", refreshHours);
        row.put("algorithm", algorithm);
        row.put("clockSkewSeconds", clockSkew);
        row.put("denylistCheckEnabled", denylist ? "Y" : "N");
        row.put("refreshTokenRotationEnabled", rotation ? "Y" : "N");
        row.put("updatedAt", Timestamp.from(Instant.now()));
        row.put("updatedBy", updatedBy);
        dao.mergeSecurityPolicy(row);
        runtimePolicy.apply(issuer, audience, accessMinutes, refreshHours, algorithm, clockSkew, denylist, rotation);

        mgjwa1004U0DTOout output = new mgjwa1004U0DTOout();
        output.setBusinessCode("JW");
        output.setScreen("보안정책 관리");
        output.setUpdated(true);
        output.setPolicy(dao.selectSecurityPolicy(POLICY_ID));
        return output;
    }

    private void seedDefaultPolicy(String updatedBy) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("policyId", POLICY_ID);
        row.put("issuer", runtimePolicy.getIssuer());
        row.put("audience", runtimePolicy.getAudience());
        row.put("accessTokenValidMinutes", runtimePolicy.getAccessTokenValidMinutes());
        row.put("refreshTokenValidHours", runtimePolicy.getRefreshTokenValidHours());
        row.put("algorithm", runtimePolicy.getAlgorithm());
        row.put("clockSkewSeconds", runtimePolicy.getClockSkewSeconds());
        row.put("denylistCheckEnabled", runtimePolicy.isDenylistCheckEnabled() ? "Y" : "N");
        row.put("refreshTokenRotationEnabled", runtimePolicy.isRefreshTokenRotationEnabled() ? "Y" : "N");
        row.put("updatedAt", Timestamp.from(Instant.now()));
        row.put("updatedBy", updatedBy);
        dao.mergeSecurityPolicy(row);
    }

    private static boolean ynValue(String value, boolean fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }
}
