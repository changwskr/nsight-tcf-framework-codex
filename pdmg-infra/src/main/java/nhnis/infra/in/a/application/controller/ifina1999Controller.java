package nhnis.infra.in.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.infra.in.a.application.service.ifina1999Service;
import nhnis.infra.in.a.dto.ifina1999C0DTOin;
import nhnis.infra.in.a.dto.ifina1999C0DTOout;
import nhnis.infra.in.a.dto.ifina1999D0DTOin;
import nhnis.infra.in.a.dto.ifina1999D0DTOout;
import nhnis.infra.in.a.dto.ifina1999S0DTOin;
import nhnis.infra.in.a.dto.ifina1999S0DTOout;
import nhnis.infra.in.a.dto.ifina1999U0DTOin;
import nhnis.infra.in.a.dto.ifina1999U0DTOout;

/**
 * 서버 인벤토리 파일럿 Controller (TCF OFF 호환).
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class ifina1999Controller {

    @Autowired
    private ifina1999Service ifina1999Service;

    @PostMapping("/ifina1999S0")
    public ifina1999S0DTOout ifina1999S0(@RequestBody ifina1999S0DTOin input) throws Throwable {
        return ifina1999Service.ifina1999S0(input);
    }

    @PostMapping("/ifina1999C0")
    public ifina1999C0DTOout ifina1999C0(@RequestBody ifina1999C0DTOin input) throws Throwable {
        return ifina1999Service.ifina1999C0(input);
    }

    @PostMapping("/ifina1999U0")
    public ifina1999U0DTOout ifina1999U0(@RequestBody ifina1999U0DTOin input) throws Throwable {
        return ifina1999Service.ifina1999U0(input);
    }

    @PostMapping("/ifina1999D0")
    public ifina1999D0DTOout ifina1999D0(@RequestBody ifina1999D0DTOin input) throws Throwable {
        return ifina1999Service.ifina1999D0(input);
    }
}
