package nhnis.mk.co.a.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mk.co.a.dto.mkcoa5530S0DTOin;
import nhnis.mk.co.a.dto.mkcoa5530S0DTOout;
import nhnis.mk.co.a.service.mkcoa5530Service;

/**
 * 마케팅희망고객 조회 Controller.
 *
 * @since 2026.08.07
 */
@Slf4j
@RestController
public class mkcoa5530Controller {

    @Autowired
    private mkcoa5530Service mkcoa5530Service;

    /** 마케팅희망고객 목록 조회. */
    @PostMapping("/mkcoa5530S0")
    public mkcoa5530S0DTOout mkcoa5530S0(@RequestBody mkcoa5530S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mkcoa5530 Controller Start!");

        mkcoa5530S0DTOout output = mkcoa5530Service.mkcoa5530S0(input);

        log.info("▷▷▷▷▷▷▷▷ mkcoa5530 Controller End!" + output.toString());
        return output;
    }
}
