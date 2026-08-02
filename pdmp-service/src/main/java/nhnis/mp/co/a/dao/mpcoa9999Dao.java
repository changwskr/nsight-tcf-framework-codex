package nhnis.mp.co.a.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import nhnis.mp.co.a.dto.mpcoa9999DtoIn;
import nhnis.mp.co.a.dto.mpcoa9999DtoOut;

/**
 * 영업팁 실적(TB_CR_AH_SALES_TIP_RACT) 조회 DAO.
 *
 * <p>
 * 인터페이스 FQCN이 rdw.mp.co.a/mpcoa9999-ORA.xml의 namespace와 일치해야 하므로
 * 클래스명은 전문 ID 표기를 그대로 따른다.
 */
@Mapper
public interface mpcoa9999Dao {

    /** 영업팁 실적 목록 조회. */
    List<mpcoa9999DtoOut> mpcoa9999S0_S0(mpcoa9999DtoIn param);

    /** 영업팁 실적 목록 전체 건수 조회. */
    int mpcoa9999S0_S0_count(mpcoa9999DtoIn param);

    /** 영업팁 실적 단건 조회. */
    mpcoa9999DtoOut mpcoa9999S0_S1(mpcoa9999DtoIn param);
}
