package nhnis.mp.co.a.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import nhnis.mp.co.a.dto.mpcoa8888DtoIn;
import nhnis.mp.co.a.dto.mpcoa8888DtoOut;

/** 영업팁 실적 CRUD DAO. */
@Mapper
public interface mpcoa8888Dao {

    List<mpcoa8888DtoOut> mpcoa8888S0_S0(mpcoa8888DtoIn param);
    int mpcoa8888S0_S0_count(mpcoa8888DtoIn param);
    mpcoa8888DtoOut mpcoa8888S0_S1(mpcoa8888DtoIn param);
    int mpcoa8888I0_I0(mpcoa8888DtoIn param);
    int mpcoa8888U0_U0(mpcoa8888DtoIn param);
    int mpcoa8888D0_D0(mpcoa8888DtoIn param);
}
