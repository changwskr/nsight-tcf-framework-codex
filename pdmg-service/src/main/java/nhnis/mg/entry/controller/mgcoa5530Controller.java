package nhnis.mg.entry.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.application.dto.mgcoa5530S0DTOin;
import nhnis.mg.application.dto.mgcoa5530S0DTOout;
import nhnis.mg.application.service.mgcoa5530Service;

/**
 * 마케팅희망고객 조회 Controller.
 *
 * @since 2026.08.07
 */
@Slf4j
@RestController
public class mgcoa5530Controller {

    @Autowired
    private mgcoa5530Service mgcoa5530Service;

    /** 마케팅희망고객 목록 조회. */
    @PostMapping("/mgcoa5530S0")
    public mgcoa5530S0DTOout mgcoa5530S0(@RequestBody mgcoa5530S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mgcoa5530 Controller Start!");

        mgcoa5530S0DTOout output = mgcoa5530Service.mgcoa5530S0(input);

        log.info("▷▷▷▷▷▷▷▷ mgcoa5530 Controller End!" + output.toString());
        return output;
    }
}
