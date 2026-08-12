package nhnis.mg.co.a.persistence.dao;

import java.util.List;
import java.util.Map;

import nhnis.mg.co.a.config.RDWMapper;

/**
 * <PRE>
 * 설명
 * </PRE>
 *
 * @author 홍길동
 * @version 1.0, 2026. 7. 24.
 * @logicalName mgcoa9999DAO
 *
 * <pre>
 * 이력사항
 * 2026. 07. 24. 홍길동 최초작성
 * </pre>
 */
@RDWMapper
public interface mgcoa9999DAO {

    /**
     * 영업팁 실적 목록 조회.
     *
     * @param input 조회 조건 (salzTipKdc 등)
     * @return 조회 결과
     * @throws Exception 예외
     */
    public List<Map<String, Object>> mgcoa9999S0_S0(Map<String, Object> input) throws Exception;
}
