package nhnis.mg.entry.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.application.dto.mgcoa8888D0DTOin;
import nhnis.mg.application.dto.mgcoa8888D0DTOout;
import nhnis.mg.application.dto.mgcoa8888S0DTOin;
import nhnis.mg.application.dto.mgcoa8888S0DTOout;
import nhnis.mg.application.service.mgcoa8888Service;

/**
 * 이미지로그 조회/삭제 Controller.
 *
 * @since 2026.08.07
 */
@Slf4j
@RestController
public class mgcoa8888Controller {

    @Autowired
    private mgcoa8888Service mgcoa8888Service;

    /** 이미지로그 목록 조회. */
    @PostMapping("/mgcoa8888S0")
    public mgcoa8888S0DTOout mgcoa8888S0(@RequestBody mgcoa8888S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mgcoa8888 Controller Start!");

        mgcoa8888S0DTOout output = mgcoa8888Service.mgcoa8888S0(input);

        log.info("▷▷▷▷▷▷▷▷ mgcoa8888 Controller End!" + output.toString());
        return output;
    }

    /** 이미지로그 삭제. */
    @PostMapping("/mgcoa8888D0")
    public mgcoa8888D0DTOout mgcoa8888D0(@RequestBody mgcoa8888D0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mgcoa8888 Controller Start!");

        mgcoa8888D0DTOout output = mgcoa8888Service.mgcoa8888D0(input);

        log.info("▷▷▷▷▷▷▷▷ mgcoa8888 Controller End!" + output.toString());
        return output;
    }
}
