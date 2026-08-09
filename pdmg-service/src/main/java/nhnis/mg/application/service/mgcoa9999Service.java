package nhnis.mg.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nhnis.mg.persistence.dao.mgcoa9999DAO;
import nhnis.mg.application.dto.mgcoa9999S0DTOout;
import nhnis.mg.application.dto.mgcoa9999S0DTOSub0;
import nhnis.mg.application.dto.mgcoa9999S0DTOin;

/**
 * 영업팁 실적 조회 Service.
 *
 * @author 홍길동
 * @since 2026.07.24
 */
@Service
public class mgcoa9999Service {

    private static final Logger log = LoggerFactory.getLogger(mgcoa9999Service.class);

    @Autowired
    private mgcoa9999DAO mgcoa9999DAO;

    /**
     * 영업팁 실적 목록 조회.
     *
     * @param input 입력 DTO
     * @return 출력 DTO (Sub0 리스트)
     * @throws Exception 예외
     */
    public mgcoa9999S0DTOout mgcoa9999S0(mgcoa9999S0DTOin input) throws Exception {
        log.info("▶▶▶▶▶▶▶▶ mgcoa9999S0 Service Start!");

        Map<String, Object> param = new HashMap<String, Object>();
        if (input != null && input.getSalzTipKdc() != null && !input.getSalzTipKdc().isEmpty()) {
            param.put("salzTipKdc", input.getSalzTipKdc());
        }

        List<Map<String, Object>> rows = mgcoa9999DAO.mgcoa9999S0_S0(param);

        mgcoa9999S0DTOout output = new mgcoa9999S0DTOout();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                mgcoa9999S0DTOSub0 sub = new mgcoa9999S0DTOSub0();
                sub.setTrtBrc(asString(row, "TRT_BRC", "trtBrc"));
                sub.setTrtmnEno(asString(row, "TRTMN_ENO", "trtmnEno"));
                sub.setSalzTipKdc(asString(row, "SALZ_TIP_KDC", "salzTipKdc"));
                sub.setBasDt(asString(row, "BAS_DT", "basDt"));
                sub.setPrtoCn(asString(row, "PRTO_CN", "prtoCn"));
                sub.setInqCn(asString(row, "INQ_CN", "inqCn"));
                sub.setInpCn(asString(row, "INP_CN", "inpCn"));
                output.addmgcoa9999S0DTOSub0(sub);
            }
            output.setSize(output.sizemgcoa9999S0DTOSub0());
        }

        log.info("▶▶▶▶▶▶▶▶ mgcoa9999S0 Service End!");
        return output;
    }

    private String asString(Map<String, Object> row, String upperKey, String camelKey) {
        Object value = row.get(upperKey);
        if (value == null) {
            value = row.get(camelKey);
        }
        return value == null ? null : String.valueOf(value);
    }
}
