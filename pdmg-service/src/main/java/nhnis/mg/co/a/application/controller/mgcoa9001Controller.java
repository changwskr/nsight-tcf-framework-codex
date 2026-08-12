package nhnis.mg.co.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.co.a.application.service.mgcoa9001Service;
import nhnis.mg.co.a.dto.mgcoa9001C0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001C0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001D0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001D0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001S0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001S0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001U0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001U0DTOout;

/**
 * 거래통제 Controller (TCF OFF 호환).
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class mgcoa9001Controller {

    @Autowired
    private mgcoa9001Service mgcoa9001Service;

    @PostMapping("/mgcoa9001S0")
    public mgcoa9001S0DTOout mgcoa9001S0(@RequestBody mgcoa9001S0DTOin input) throws Throwable {
        return mgcoa9001Service.mgcoa9001S0(input);
    }

    @PostMapping("/mgcoa9001C0")
    public mgcoa9001C0DTOout mgcoa9001C0(@RequestBody mgcoa9001C0DTOin input) throws Throwable {
        return mgcoa9001Service.mgcoa9001C0(input);
    }

    @PostMapping("/mgcoa9001U0")
    public mgcoa9001U0DTOout mgcoa9001U0(@RequestBody mgcoa9001U0DTOin input) throws Throwable {
        return mgcoa9001Service.mgcoa9001U0(input);
    }

    @PostMapping("/mgcoa9001D0")
    public mgcoa9001D0DTOout mgcoa9001D0(@RequestBody mgcoa9001D0DTOin input) throws Throwable {
        return mgcoa9001Service.mgcoa9001D0(input);
    }
}
