package nhnis.mg.jw.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.jw.a.application.service.mgjwa1001Service;
import nhnis.mg.jw.a.dto.mgjwa1001D0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1001D0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1001S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1001S0DTOout;

/**
 * 토큰 현황 조회·강제폐기 Controller (TCF OFF 호환).
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class mgjwa1001Controller {

    @Autowired
    private mgjwa1001Service mgjwa1001Service;

    @PostMapping("/mgjwa1001S0")
    public mgjwa1001S0DTOout mgjwa1001S0(@RequestBody mgjwa1001S0DTOin input) throws Throwable {
        return mgjwa1001Service.mgjwa1001S0(input);
    }

    @PostMapping("/mgjwa1001D0")
    public mgjwa1001D0DTOout mgjwa1001D0(@RequestBody mgjwa1001D0DTOin input) throws Throwable {
        return mgjwa1001Service.mgjwa1001D0(input);
    }
}
