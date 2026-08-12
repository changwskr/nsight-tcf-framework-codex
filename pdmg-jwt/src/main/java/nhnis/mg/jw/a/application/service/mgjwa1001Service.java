package nhnis.mg.jw.a.application.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import nhnis.mg.jw.a.config.JwtRuntimePolicy;
import nhnis.mg.jw.a.dto.mgjwa1001D0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1001D0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1001S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1001S0DTOout;
import nhnis.mg.jw.a.persistence.dao.mgjwa1001DAO;
import nhnis.fw.exception.BizException;
import nhnis.mg.jw.a.support.JwtSupport;
import nhnis.mg.jw.a.support.JwtTokenStore;

/** 토큰 현황 조회·강제폐기. */
@Service
public class mgjwa1001Service {

    private final mgjwa1001DAO dao;
    private final JwtTokenStore tokenStore;
    private final JwtRuntimePolicy runtimePolicy;

    public mgjwa1001Service(mgjwa1001DAO dao, JwtTokenStore tokenStore, JwtRuntimePolicy runtimePolicy) {
        this.dao = dao;
        this.tokenStore = tokenStore;
        this.runtimePolicy = runtimePolicy;
    }

    public mgjwa1001S0DTOout mgjwa1001S0(mgjwa1001S0DTOin input) {
        Map<String, Object> criteria = buildCriteria(input);
        List<Map<String, Object>> rows = dao.searchJwtTokens(criteria);
        int totalCount = dao.countJwtTokens(criteria);

        mgjwa1001S0DTOout output = new mgjwa1001S0DTOout();
        output.setBusinessCode("JW");
        output.setScreen("토큰 현황");
        output.setPageNo((Integer) criteria.get("pageNo"));
        output.setPageSize((Integer) criteria.get("pageSize"));
        output.setTotalCount((long) totalCount);
        output.setRows(rows);
        return output;
    }

    public mgjwa1001D0DTOout mgjwa1001D0(mgjwa1001D0DTOin input) {
        if (input == null || !StringUtils.hasText(input.getJti())) {
            throw new BizException("E-JWT-VAL-0001", "jti");
        }
        String jti = input.getJti();
        String reason = StringUtils.hasText(input.getReason()) ? input.getReason() : "ADMIN_REVOKE";
        Map<String, Object> stored = dao.selectJwtTokenByJti(
                Map.of("issuer", runtimePolicy.getIssuer(), "jti", jti));
        Instant expiresAt = Instant.now().plusSeconds(3600);
        String userId = null;
        if (stored != null) {
            Object exp = stored.get("expiresAt");
            if (exp instanceof Timestamp ts) {
                expiresAt = ts.toInstant();
            }
            userId = JwtSupport.stringValue(stored, "userId");
        }
        tokenStore.denylist(jti, userId, expiresAt, reason);

        mgjwa1001D0DTOout output = new mgjwa1001D0DTOout();
        output.setBusinessCode("JW");
        output.setScreen("토큰 강제폐기");
        output.setRevoked(true);
        output.setJti(jti);
        output.setReason(reason);
        return output;
    }

    private Map<String, Object> buildCriteria(mgjwa1001S0DTOin input) {
        Map<String, Object> criteria = new HashMap<>();
        int pageNo = input != null && input.getPageNo() != null && input.getPageNo() >= 1 ? input.getPageNo() : 1;
        int pageSize = input != null && input.getPageSize() != null && input.getPageSize() >= 1
                ? input.getPageSize() : 20;
        if (pageSize > 100) {
            pageSize = 100;
        }
        criteria.put("pageNo", pageNo);
        criteria.put("pageSize", pageSize);
        criteria.put("offset", (pageNo - 1) * pageSize);
        if (input != null) {
            putIfHasText(criteria, "userId", input.getUserId());
            putIfHasText(criteria, "jti", input.getJti());
            putIfHasText(criteria, "revokedYn", input.getRevokedYn());
            putIfHasText(criteria, "activeOnly", input.getActiveOnly());
        }
        return criteria;
    }

    private void putIfHasText(Map<String, Object> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value);
        }
    }
}
