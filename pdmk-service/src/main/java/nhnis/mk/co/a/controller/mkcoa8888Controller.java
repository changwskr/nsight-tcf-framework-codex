package nhnis.mk.co.a.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mk.co.a.dto.mkcoa8888D0DTOin;
import nhnis.mk.co.a.dto.mkcoa8888D0DTOout;
import nhnis.mk.co.a.dto.mkcoa8888S0DTOin;
import nhnis.mk.co.a.dto.mkcoa8888S0DTOout;
import nhnis.mk.co.a.service.mkcoa8888Service;

/**
 * 이미지로그 조회/삭제 Controller.
 *
 * @since 2026.08.07
 */
@Slf4j
@RestController
public class mkcoa8888Controller {

    @Autowired
    private mkcoa8888Service mkcoa8888Service;

    /** 이미지로그 목록 조회. */
    @PostMapping("/mkcoa8888S0")
    public mkcoa8888S0DTOout mkcoa8888S0(@RequestBody mkcoa8888S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa8888 Controller Start!");

        mkcoa8888S0DTOout output = mkcoa8888Service.mkcoa8888S0(input);

        log.info("▷▷▷▷▷▷▷▷ mkcoa8888 Controller End!" + output.toString());
        return output;
    }

    /** 이미지로그 삭제. */
    @PostMapping("/mkcoa8888D0")
    public mkcoa8888D0DTOout mkcoa8888D0(@RequestBody mkcoa8888D0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa8888 Controller Start!");

        mkcoa8888D0DTOout output = mkcoa8888Service.mkcoa8888D0(input);

        log.info("▷▷▷▷▷▷▷▷ mkcoa8888 Controller End!" + output.toString());
        return output;
    }
}
