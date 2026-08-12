package nhnis.mg.co.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.co.a.dto.mgcoa9000C0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000C0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000D0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000D0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000S0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000S0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000U0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000U0DTOout;
import nhnis.mg.co.a.application.service.mgcoa9000Service;

/**
 * 거래 파라미터 관리 Controller (TCF OFF 호환).
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class mgcoa9000Controller {

    @Autowired
    private mgcoa9000Service mgcoa9000Service;

    @PostMapping("/mgcoa9000S0")
    public mgcoa9000S0DTOout mgcoa9000S0(@RequestBody mgcoa9000S0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mgcoa9000 Controller Start!");
        mgcoa9000S0DTOout output = mgcoa9000Service.mgcoa9000S0(input);
        log.info("▷▷▷▷▷▷▷▷ mgcoa9000 Controller End!" + output);
        return output;
    }

    @PostMapping("/mgcoa9000C0")
    public mgcoa9000C0DTOout mgcoa9000C0(@RequestBody mgcoa9000C0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mgcoa9000 Controller Start!");
        mgcoa9000C0DTOout output = mgcoa9000Service.mgcoa9000C0(input);
        log.info("▷▷▷▷▷▷▷▷ mgcoa9000 Controller End!" + output);
        return output;
    }

    @PostMapping("/mgcoa9000U0")
    public mgcoa9000U0DTOout mgcoa9000U0(@RequestBody mgcoa9000U0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mgcoa9000 Controller Start!");
        mgcoa9000U0DTOout output = mgcoa9000Service.mgcoa9000U0(input);
        log.info("▷▷▷▷▷▷▷▷ mgcoa9000 Controller End!" + output);
        return output;
    }

    @PostMapping("/mgcoa9000D0")
    public mgcoa9000D0DTOout mgcoa9000D0(@RequestBody mgcoa9000D0DTOin input) throws Throwable {
        log.info("▷▷▷▷▷▷▷▷ mgcoa9000 Controller Start!");
        mgcoa9000D0DTOout output = mgcoa9000Service.mgcoa9000D0(input);
        log.info("▷▷▷▷▷▷▷▷ mgcoa9000 Controller End!" + output);
        return output;
    }
}
