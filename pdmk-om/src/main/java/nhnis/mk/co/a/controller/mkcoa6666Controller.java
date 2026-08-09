package nhnis.mk.co.a.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mk.co.a.dto.mkcoa6666D0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666E0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666E0DTOout;
import nhnis.mk.co.a.dto.mkcoa6666I0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666ProcDTOout;
import nhnis.mk.co.a.dto.mkcoa6666S0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666S0DTOout;
import nhnis.mk.co.a.dto.mkcoa6666S1DTOout;
import nhnis.mk.co.a.dto.mkcoa6666S2DTOout;
import nhnis.mk.co.a.dto.mkcoa6666S3DTOin;
import nhnis.mk.co.a.dto.mkcoa6666S3DTOout;
import nhnis.mk.co.a.dto.mkcoa6666U0DTOin;
import nhnis.mk.co.a.dto.mkcoa6666U1DTOin;
import nhnis.mk.co.a.service.mkcoa6666Service;

/**
 * 거래통제 Controller (mkcoa6666).
 * Service Catalog CRUD + sys_comm 평가(E0).
 * 요건: docs/요건정의/01.비기능요건-거래통제.md
 */
@Slf4j
@RestController
public class mkcoa6666Controller {

    @Autowired
    private mkcoa6666Service mkcoa6666Service;

    /** Service Catalog 목록 조회. */
    @PostMapping("/mkcoa6666S0")
    public mkcoa6666S0DTOout mkcoa6666S0(@RequestBody mkcoa6666S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666S0 Controller Start!");
        mkcoa6666S0DTOout output = mkcoa6666Service.mkcoa6666S0(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666S0 Controller End! " + output);
        return output;
    }

    /** 서비스별 거래통제 상세. */
    @PostMapping("/mkcoa6666S1")
    public mkcoa6666S1DTOout mkcoa6666S1(@RequestBody mkcoa6666S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666S1 Controller Start!");
        mkcoa6666S1DTOout output = mkcoa6666Service.mkcoa6666S1(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666S1 Controller End! " + output);
        return output;
    }

    /** 최근 통제 결과 목록. */
    @PostMapping("/mkcoa6666S3")
    public mkcoa6666S3DTOout mkcoa6666S3(@RequestBody mkcoa6666S3DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666S3 Controller Start!");
        mkcoa6666S3DTOout output = mkcoa6666Service.mkcoa6666S3(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666S3 Controller End! " + output);
        return output;
    }

    /** Service Catalog 등록. */
    @PostMapping("/mkcoa6666I0")
    public mkcoa6666ProcDTOout mkcoa6666I0(@RequestBody mkcoa6666I0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666I0 Controller Start!");
        mkcoa6666ProcDTOout output = mkcoa6666Service.mkcoa6666I0(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666I0 Controller End! " + output);
        return output;
    }

    /** Service Catalog 수정. */
    @PostMapping("/mkcoa6666U0")
    public mkcoa6666ProcDTOout mkcoa6666U0(@RequestBody mkcoa6666U0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666U0 Controller Start!");
        mkcoa6666ProcDTOout output = mkcoa6666Service.mkcoa6666U0(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666U0 Controller End! " + output);
        return output;
    }

    /** Service Catalog 삭제. */
    @PostMapping("/mkcoa6666D0")
    public mkcoa6666ProcDTOout mkcoa6666D0(@RequestBody mkcoa6666D0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666D0 Controller Start!");
        mkcoa6666ProcDTOout output = mkcoa6666Service.mkcoa6666D0(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666D0 Controller End! " + output);
        return output;
    }

    /** sys_comm 거래통제 평가 (ALLOW/REJECT/BLOCK). */
    @PostMapping("/mkcoa6666E0")
    public mkcoa6666E0DTOout mkcoa6666E0(@RequestBody mkcoa6666E0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666E0 Controller Start!");
        mkcoa6666E0DTOout output = mkcoa6666Service.mkcoa6666E0(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666E0 Controller End! " + output);
        return output;
    }

    /** Catalog 상태 집계 (대시보드). */
    @PostMapping("/mkcoa6666S2")
    public mkcoa6666S2DTOout mkcoa6666S2(@RequestBody mkcoa6666S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666S2 Controller Start!");
        mkcoa6666S2DTOout output = mkcoa6666Service.mkcoa6666S2(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666S2 Controller End! " + output);
        return output;
    }

    /** 중지/점검/재개. */
    @PostMapping("/mkcoa6666U1")
    public mkcoa6666ProcDTOout mkcoa6666U1(@RequestBody mkcoa6666U1DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666U1 Controller Start!");
        mkcoa6666ProcDTOout output = mkcoa6666Service.mkcoa6666U1(input);
        log.info("▷▷▷▷▷▷▷▷ mkcoa6666U1 Controller End! " + output);
        return output;
    }
}
