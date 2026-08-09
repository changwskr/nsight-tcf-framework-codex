package nhnis.mk.co.a.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhnis.mk.co.a.dao.mkcoa8888DAO;
import nhnis.mk.co.a.dto.mkcoa8888D0DTOin;
import nhnis.mk.co.a.dto.mkcoa8888D0DTOout;
import nhnis.mk.co.a.dto.mkcoa8888S0DTOSub0;
import nhnis.mk.co.a.dto.mkcoa8888S0DTOin;
import nhnis.mk.co.a.dto.mkcoa8888S0DTOout;

/**
 * 이미지로그 조회/삭제 Service.
 *
 * @since 2026.08.07
 */
@Service
public class mkcoa8888Service {

    private static final Logger log = LoggerFactory.getLogger(mkcoa8888Service.class);

    @Autowired
    private mkcoa8888DAO mkcoa8888DAO;

    /**
     * 이미지로그 목록 조회 (페이징).
     */
    public mkcoa8888S0DTOout mkcoa8888S0(mkcoa8888S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa8888S0 Service Start!");

        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            putIfHasText(param, "guid", input.getGuid());
            putIfHasText(param, "serviceId", input.getServiceId());
            putIfHasText(param, "screenId", input.getScreenId());
            putIfHasText(param, "optrEno", input.getOptrEno());
            if (Boolean.TRUE.equals(input.getExceptionOnly())) {
                param.put("exceptionOnly", true);
            }
        }

        int pageNo = input == null || input.getPageNo() == null || input.getPageNo() <= 0
                ? 1 : input.getPageNo();
        int pageSize = input == null || input.getPageSize() == null || input.getPageSize() <= 0
                ? 20 : input.getPageSize();
        if (pageSize > 100) {
            pageSize = 100;
        }
        int offset = (pageNo - 1) * pageSize;
        param.put("pageNo", pageNo);
        param.put("pageSize", pageSize);
        param.put("offset", offset);

        int totalCount = mkcoa8888DAO.mkcoa8888S0_S0_count(param);
        List<Map<String, Object>> rows = mkcoa8888DAO.mkcoa8888S0_S0(param);

        mkcoa8888S0DTOout output = new mkcoa8888S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                mkcoa8888S0DTOSub0 sub = new mkcoa8888S0DTOSub0();
                sub.setGuid(asString(row, "GUID", "guid"));
                sub.setServiceId(asString(row, "SERVICE_ID", "serviceId"));
                sub.setScreenId(asString(row, "SCREEN_ID", "screenId"));
                sub.setOptrEno(asString(row, "OPTR_ENO", "optrEno"));
                sub.setClientIp(asString(row, "CLIENT_IP", "clientIp"));
                sub.setRequestTime(asString(row, "REQUEST_TIME", "requestTime"));
                sub.setResponseTime(asString(row, "RESPONSE_TIME", "responseTime"));
                sub.setExceptionType(asString(row, "EXCEPTION_TYPE", "exceptionType"));
                sub.setExceptionCode(asString(row, "EXCEPTION_CODE", "exceptionCode"));
                sub.setExceptionMsg(asString(row, "EXCEPTION_MSG", "exceptionMsg"));
                output.addmkcoa8888S0DTOSub0(sub);
            }
        }
        output.setSize(output.sizemkcoa8888S0DTOSub0());
        output.setPageNo(pageNo);
        output.setPageSize(pageSize);
        output.setTotalCount(totalCount);
        output.setTotalPages(pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1L) / pageSize));

        log.info("▶▶▶▶▶▶▶▶ mkcoa8888S0 Service End! - Total: " + totalCount);
        return output;
    }

    /**
     * 이미지로그 삭제.
     */
    public mkcoa8888D0DTOout mkcoa8888D0(mkcoa8888D0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa8888D0 Service Start!");

        mkcoa8888D0DTOout output = new mkcoa8888D0DTOout();
        if (input == null || input.getGuidList() == null || input.getGuidList().isEmpty()) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("NO_DATA");
            log.info("▶▶▶▶▶▶▶▶ mkcoa8888D0 Service End! - Total: 0");
            return output;
        }

        List<String> guids = input.getGuidList().stream()
                .filter(g -> g != null && !g.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (guids.isEmpty()) {
            output.setPROC_CNT(0);
            output.setRSLT_CD("0001");
            output.setRSLT_MSG("NO_DATA");
            log.info("▶▶▶▶▶▶▶▶ mkcoa8888D0 Service End! - Total: 0 (blank guids)");
            return output;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("guidList", guids);
        int cnt = mkcoa8888DAO.mkcoa8888D0_D0(param);

        output.setPROC_CNT(cnt);
        output.setRSLT_CD("0000");
        output.setRSLT_MSG("OK");

        log.info("▶▶▶▶▶▶▶▶ mkcoa8888D0 Service End! - Total: " + cnt);
        return output;
    }

    private void putIfHasText(Map<String, Object> param, String key, String value) {
        if (value != null && !value.isBlank()) {
            param.put(key, value.trim());
        }
    }

    private String asString(Map<String, Object> row, String upperKey, String camelKey) {
        if (row == null) {
            return null;
        }
        Object value = row.get(upperKey);
        if (value == null) {
            value = row.get(camelKey);
        }
        if (value == null) {
            // H2/Oracle/MyBatis 환경별 키 대소문자 차이 대응
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey() != null
                        && (entry.getKey().equalsIgnoreCase(upperKey)
                        || entry.getKey().equalsIgnoreCase(camelKey))
                        && entry.getValue() != null) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value == null ? null : String.valueOf(value);
    }
}
