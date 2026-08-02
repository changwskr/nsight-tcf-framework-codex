package nhnis.mp.co.a.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhnis.fw.exception.BizException;
import nhnis.mp.co.a.dao.mpcoa8888Dao;
import nhnis.mp.co.a.dto.mpcoa8888DtoIn;
import nhnis.mp.co.a.dto.mpcoa8888DtoOut;
import nhnis.mp.co.a.dto.mpcoa8888ListResponseDto;

/** 영업팁 실적 CRUD 서비스. */
@Service
@Transactional(readOnly = true)
public class mpcoa8888Service {

    private static final String CODE_REQUIRED = "FW0001";
    private static final String CODE_NOT_FOUND = "MP0404";
    private static final String CODE_DUPLICATE = "MP0409";

    private final mpcoa8888Dao dao;

    public mpcoa8888Service(mpcoa8888Dao dao) {
        this.dao = dao;
    }

    public mpcoa8888ListResponseDto selectSalesTipList(mpcoa8888DtoIn in) {
        mpcoa8888DtoIn param = new mpcoa8888DtoIn();
        param.setSalzTipKdc(in == null ? null : trimToNull(in.getSalzTipKdc()));
        int pageNo = in == null || in.getPageNo() == null || in.getPageNo() <= 0 ? 1 : in.getPageNo();
        int pageSize = in == null || in.getPageSize() == null || in.getPageSize() <= 0 ? 20 : in.getPageSize();
        pageSize = Math.min(pageSize, 100);
        param.setPageNo(pageNo);
        param.setPageSize(pageSize);
        param.setOffset((pageNo - 1) * pageSize);

        long totalCount = dao.mpcoa8888S0_S0_count(param);
        List<mpcoa8888DtoOut> records = dao.mpcoa8888S0_S0(param);
        mpcoa8888ListResponseDto response = new mpcoa8888ListResponseDto();
        response.setRecords(records);
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setTotalCount(totalCount);
        response.setTotalPages((int) ((totalCount + pageSize - 1) / pageSize));
        return response;
    }

    public mpcoa8888DtoOut selectSalesTipDetail(mpcoa8888DtoIn in) {
        mpcoa8888DtoOut result = dao.mpcoa8888S0_S1(normalizedKey(in));
        if (result == null) {
            throw new BizException(CODE_NOT_FOUND);
        }
        return result;
    }

    @Transactional(timeout = 4)
    public void createSalesTip(mpcoa8888DtoIn in) {
        mpcoa8888DtoIn param = normalizedWriteInput(in);
        if (dao.mpcoa8888S0_S1(param) != null) {
            throw new BizException(CODE_DUPLICATE);
        }
        dao.mpcoa8888I0_I0(param);
    }

    @Transactional(timeout = 4)
    public void updateSalesTip(mpcoa8888DtoIn in) {
        if (dao.mpcoa8888U0_U0(normalizedWriteInput(in)) == 0) {
            throw new BizException(CODE_NOT_FOUND);
        }
    }

    @Transactional(timeout = 4)
    public void deleteSalesTip(mpcoa8888DtoIn in) {
        if (dao.mpcoa8888D0_D0(normalizedKey(in)) == 0) {
            throw new BizException(CODE_NOT_FOUND);
        }
    }

    private mpcoa8888DtoIn normalizedWriteInput(mpcoa8888DtoIn in) {
        mpcoa8888DtoIn param = normalizedKey(in);
        param.setPrtoCn(trimToNull(in.getPrtoCn()));
        param.setInqCn(trimToNull(in.getInqCn()));
        param.setInpCn(trimToNull(in.getInpCn()));
        return param;
    }

    private mpcoa8888DtoIn normalizedKey(mpcoa8888DtoIn in) {
        if (in == null) {
            throw new BizException(CODE_REQUIRED, "요청 Body");
        }
        mpcoa8888DtoIn param = new mpcoa8888DtoIn();
        param.setTrtBrc(require(in.getTrtBrc(), "취급점코드"));
        param.setTrtmnEno(require(in.getTrtmnEno(), "취급자사번"));
        param.setSalzTipKdc(require(in.getSalzTipKdc(), "영업팁종류코드"));
        param.setBasDt(require(in.getBasDt(), "기준일자"));
        return param;
    }

    private String require(String value, String fieldName) {
        String result = trimToNull(value);
        if (result == null) {
            throw new BizException(CODE_REQUIRED, fieldName);
        }
        return result;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
