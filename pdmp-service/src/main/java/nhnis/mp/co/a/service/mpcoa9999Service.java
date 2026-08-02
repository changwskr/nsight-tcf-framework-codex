package nhnis.mp.co.a.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhnis.fw.exception.BizException;
import nhnis.mp.co.a.dao.mpcoa9999Dao;
import nhnis.mp.co.a.dto.mpcoa9999DtoIn;
import nhnis.mp.co.a.dto.mpcoa9999DtoOut;
import nhnis.mp.co.a.dto.mpcoa9999ListResponseDto;

@Service
@Transactional(readOnly = true)
public class mpcoa9999Service {

    private static final String CODE_REQUIRED = "FW0001";

    private final mpcoa9999Dao dao;

    public mpcoa9999Service(mpcoa9999Dao dao) {
        this.dao = dao;
    }

    public mpcoa9999ListResponseDto selectSalesTipList(mpcoa9999DtoIn in) {
        mpcoa9999DtoIn param = new mpcoa9999DtoIn();
        param.setSalzTipKdc(in == null ? null : trimToNull(in.getSalzTipKdc()));

        int pageNo = in == null || in.getPageNo() == null || in.getPageNo() <= 0 ? 1 : in.getPageNo();
        int pageSize = in == null || in.getPageSize() == null || in.getPageSize() <= 0 ? 20 : in.getPageSize();
        if (pageSize > 100) {
            pageSize = 100;
        }
        int offset = (pageNo - 1) * pageSize;
        param.setPageNo(pageNo);
        param.setPageSize(pageSize);
        param.setOffset(offset);

        long totalCount = dao.mpcoa9999S0_S0_count(param);
        List<mpcoa9999DtoOut> records = dao.mpcoa9999S0_S0(param);

        mpcoa9999ListResponseDto response = new mpcoa9999ListResponseDto();
        response.setRecords(records);
        response.setPageNo(pageNo);
        response.setPageSize(pageSize);
        response.setTotalCount(totalCount);
        response.setTotalPages((int) ((totalCount + pageSize - 1) / pageSize));
        return response;
    }

    public mpcoa9999DtoOut selectSalesTipDetail(mpcoa9999DtoIn in) {
        if (in == null) {
            throw new BizException(CODE_REQUIRED, "요청 Body");
        }
        mpcoa9999DtoIn param = new mpcoa9999DtoIn();
        param.setTrtBrc(require(in.getTrtBrc(), "취급점코드"));
        param.setTrtmnEno(require(in.getTrtmnEno(), "취급자사번"));
        param.setSalzTipKdc(require(in.getSalzTipKdc(), "영업팁종류코드"));
        param.setBasDt(require(in.getBasDt(), "기준일자"));

        mpcoa9999DtoOut row = dao.mpcoa9999S0_S1(param);
        if (row == null) {
            throw new BizException("FW0003", "mpcoa9999S0_S1", "조회 결과 없음");
        }
        return row;
    }

    private String require(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BizException(CODE_REQUIRED, fieldName);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
