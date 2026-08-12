package nhnis.mg.jw.a.application.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import nhnis.mg.jw.a.dto.mgjwa1002S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1002S0DTOout;
import nhnis.mg.jw.a.persistence.dao.mgjwa1002DAO;
import nhnis.mg.jw.a.support.JwtClientContext;
import nhnis.mg.jw.a.support.JwtSupport;

/** 로그인 이력. */
@Service
public class mgjwa1002Service {

    private final mgjwa1002DAO dao;

    public mgjwa1002Service(mgjwa1002DAO dao) {
        this.dao = dao;
    }

    public mgjwa1002S0DTOout mgjwa1002S0(mgjwa1002S0DTOin input) {
        Map<String, Object> criteria = buildCriteria(input);
        List<Map<String, Object>> rows = dao.searchLoginHistories(criteria);
        int totalCount = dao.countLoginHistories(criteria);

        mgjwa1002S0DTOout output = new mgjwa1002S0DTOout();
        output.setBusinessCode("JW");
        output.setScreen("로그인 이력");
        output.setPageNo((Integer) criteria.get("pageNo"));
        output.setPageSize((Integer) criteria.get("pageSize"));
        output.setTotalCount((long) totalCount);
        output.setRows(rows);
        return output;
    }

    /**
     * 로그인 성공/실패 이력. 실패 시 상위 트랜잭션이 rollback 되어도 이력이 남도록
     * 별도 트랜잭션(REQUIRES_NEW)으로 커밋한다.
     */
    @Transactional(transactionManager = "rdwTransactionManager", propagation = Propagation.REQUIRES_NEW,
            rollbackFor = Exception.class)
    public void recordLoginHistory(String userId, String loginResult, String failReason) {
        Map<String, Object> row = new HashMap<>();
        row.put("logId", JwtSupport.newId());
        row.put("userId", userId == null ? "UNKNOWN" : userId);
        row.put("loginResult", loginResult);
        row.put("failReason", failReason);
        row.put("channelId", JwtClientContext.channelId());
        row.put("clientIp", JwtClientContext.clientIp());
        row.put("loginTime", Timestamp.from(Instant.now()));
        dao.insertLoginHistory(row);
    }

    private Map<String, Object> buildCriteria(mgjwa1002S0DTOin input) {
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
            putIfHasText(criteria, "loginResult", input.getLoginResult());
            putIfHasText(criteria, "fromDate", input.getFromDate());
            putIfHasText(criteria, "toDate", input.getToDate());
        }
        return criteria;
    }

    private void putIfHasText(Map<String, Object> map, String key, String value) {
        if (StringUtils.hasText(value)) {
            map.put(key, value);
        }
    }
}
