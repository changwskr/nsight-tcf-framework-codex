package nhnis.mg.co.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.co.a.dto.mgcoa9999S0DTOin;
import nhnis.mg.co.a.dto.mgcoa9999S0DTOout;
import nhnis.mg.co.a.application.service.mgcoa9999Service;

/**
 * 샘플 조회 Controller.
 *
 * <p>{@code nhnis.fw.tcf.enabled=true} 이면 단일 {@code OnlineTransactionController} 경로를 쓰고
 * 이 Controller 는 등록하지 않는다.
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
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
