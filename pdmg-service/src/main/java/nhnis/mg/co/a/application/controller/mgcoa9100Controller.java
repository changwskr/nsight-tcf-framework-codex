package nhnis.mg.co.a.application.controller;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.co.a.application.facade.mgcoa9100Facade;
import nhnis.mg.co.a.dto.mgcoa9100S0DTOin;

/**
 * 런타임 진단 Controller (TCF OFF 호환).
 */
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class mgcoa9100Controller {

    private final mgcoa9100Facade facade;

    public mgcoa9100Controller(mgcoa9100Facade facade) {
        this.facade = facade;
    }

    @PostMapping("/mgcoa9100S0")
    public Map<String, Object> mgcoa9100S0(@RequestBody mgcoa9100S0DTOin input) {
        return facade.mgcoa9100S0(input);
    }
}
