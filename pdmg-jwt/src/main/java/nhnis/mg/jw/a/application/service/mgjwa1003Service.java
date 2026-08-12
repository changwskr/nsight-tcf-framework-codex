package nhnis.mg.jw.a.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import nhnis.mg.jw.a.dto.mgjwa1003S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1003S0DTOout;
import nhnis.mg.jw.a.persistence.dao.mgjwa1003DAO;

/** Refresh Token 조회. */
@Service
public class mgjwa1003Service {

    private final mgjwa1003DAO dao;

    public mgjwa1003Service(mgjwa1003DAO dao) {
        this.dao = dao;
    }

    public mgjwa1003S0DTOout mgjwa1003S0(mgjwa1003S0DTOin input) {
        Map<String, Object> criteria = buildCriteria(input);
        List<Map<String, Object>> rows = dao.searchRefreshTokens(criteria);
        int totalCount = dao.countRefreshTokens(criteria);

        mgjwa1003S0DTOout output = new mgjwa1003S0DTOout();
        output.setBusinessCode("JW");
        output.setScreen("Refresh Token 관리");
        output.setPageNo((Integer) criteria.get("pageNo"));
        output.setPageSize((Integer) criteria.get("pageSize"));
        output.setTotalCount((long) totalCount);
        output.setRows(rows);
        return output;
    }

    private Map<String, Object> buildCriteria(mgjwa1003S0DTOin input) {
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
