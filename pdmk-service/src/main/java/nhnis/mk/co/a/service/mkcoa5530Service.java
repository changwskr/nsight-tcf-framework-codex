package nhnis.mk.co.a.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhnis.mk.co.a.dao.mkcoa5530DAO;
import nhnis.mk.co.a.dto.mkcoa5530S0DTOSub0;
import nhnis.mk.co.a.dto.mkcoa5530S0DTOin;
import nhnis.mk.co.a.dto.mkcoa5530S0DTOout;

/**
 * 마케팅희망고객 조회 Service.
 *
 * @since 2026.08.07
 */
@Service
public class mkcoa5530Service {

    private static final Logger log = LoggerFactory.getLogger(mkcoa5530Service.class);

    @Autowired
    private mkcoa5530DAO mkcoa5530DAO;

    /**
     * 마케팅희망고객 목록 조회 (페이징).
     */
    public mkcoa5530S0DTOout mkcoa5530S0(mkcoa5530S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mkcoa5530S0 Service Start!");

        Map<String, Object> param = new HashMap<>();
        if (input != null) {
            putIfHasText(param, "trtBrc", input.getTrtBrc());
            putIfHasText(param, "basDt", input.getBasDt());
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

        int totalCount = mkcoa5530DAO.mkcoa5530S0_S0_count(param);
        List<Map<String, Object>> rows = mkcoa5530DAO.mkcoa5530S0_S0(param);

        mkcoa5530S0DTOout output = new mkcoa5530S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                mkcoa5530S0DTOSub0 sub = new mkcoa5530S0DTOSub0();
                sub.setL5101(asString(row, "L5101", "l5101"));
                sub.setL5102(asString(row, "L5102", "l5102"));
                sub.setL5103(asString(row, "L5103", "l5103"));
                sub.setL5104(asString(row, "L5104", "l5104"));
                output.addmkcoa5530S0DTOSub0(sub);
            }
        }
        output.setSize(output.sizemkcoa5530S0DTOSub0());
        output.setPageNo(pageNo);
        output.setPageSize(pageSize);
        output.setTotalCount(totalCount);
        output.setTotalPages(pageSize <= 0 ? 0 : (int) ((totalCount + pageSize - 1L) / pageSize));

        log.info("▶▶▶▶▶▶▶▶ mkcoa5530S0 Service End! - Total: " + totalCount);
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
