package nhnis.mk.co.a.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mk.co.a.dto.mkcoa9999S0DTOin;
import nhnis.mk.co.a.dto.mkcoa9999S0DTOout;
import nhnis.mk.co.a.service.mkcoa9999Service;

@Slf4j
@RestController
public class mkcoa9999Controller {

    @Autowired
    private mkcoa9999Service mkcoa9999Service;

    @PostMapping("/mkcoa9999S0")
    public mkcoa9999S0DTOout mkcoa9999S0(
            @RequestBody mkcoa9999S0DTOin input) throws Throwable {

        log.info("▷▷▷▷▷▷▷▷ mkcoa9999 Controller Start!");

        /**
         * 업무 개별 선처리 작성
         */

        mkcoa9999S0DTOout output =
                mkcoa9999Service.mkcoa9999S0(input);

        /**
         * 업무 개별 후처리 작성
         */

        log.info("▷▷▷▷▷▷▷▷ mkcoa9999 Controller End!");

        return output;
    }
}
