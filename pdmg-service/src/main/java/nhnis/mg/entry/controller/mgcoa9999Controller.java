package nhnis.mg.entry.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.application.dto.mgcoa9999S0DTOin;
import nhnis.mg.application.dto.mgcoa9999S0DTOout;
import nhnis.mg.application.service.mgcoa9999Service;

@Slf4j
@RestController
public class mgcoa9999Controller {

    @Autowired
    private mgcoa9999Service mgcoa9999Service;

    @PostMapping("/mgcoa9999S0")
    public mgcoa9999S0DTOout mgcoa9999S0(
            @RequestBody mgcoa9999S0DTOin input) throws Throwable {

        log.info("▷▷▷▷▷▷▷▷ mgcoa9999 Controller Start!");

        /**
         * 업무 개별 선처리 작성
         */

        mgcoa9999S0DTOout output =
                mgcoa9999Service.mgcoa9999S0(input);

        /**
         * 업무 개별 후처리 작성
         */

        log.info("▷▷▷▷▷▷▷▷ mgcoa9999 Controller End!");

        return output;
    }
}
