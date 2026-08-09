package nhnis.mk.co.a.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mk.co.a.dto.mkcoa7777D0DTOin;
import nhnis.mk.co.a.dto.mkcoa7777D0DTOout;
import nhnis.mk.co.a.dto.mkcoa7777S0DTOin;
import nhnis.mk.co.a.dto.mkcoa7777S0DTOout;
import nhnis.mk.co.a.service.mkcoa7777Service;

/**
 * 이미지로그 조회/삭제 Controller.
 *
 * @since 2026.08.07
 */
@Slf4j
@RestController
public class mkcoa7777Controller {

    @Autowired
    private mkcoa7777Service mkcoa7777Service;

    /** 이미지로그 목록 조회. */
    @PostMapping("/mkcoa7777S0")
    public mkcoa7777S0DTOout mkcoa7777S0(@RequestBody mkcoa7777S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa7777 Controller Start!");

        mkcoa7777S0DTOout output = mkcoa7777Service.mkcoa7777S0(input);

        log.info("▷▷▷▷▷▷▷▷ mkcoa7777 Controller End!" + output.toString());
        return output;
    }

    /** 이미지로그 삭제. */
    @PostMapping("/mkcoa7777D0")
    public mkcoa7777D0DTOout mkcoa7777D0(@RequestBody mkcoa7777D0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa7777 Controller Start!");

        mkcoa7777D0DTOout output = mkcoa7777Service.mkcoa7777D0(input);

        log.info("▷▷▷▷▷▷▷▷ mkcoa7777 Controller End!" + output.toString());
        return output;
    }
}
