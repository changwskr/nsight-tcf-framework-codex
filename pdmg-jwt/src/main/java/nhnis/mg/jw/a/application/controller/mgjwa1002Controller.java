package nhnis.mg.jw.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.jw.a.application.service.mgjwa1002Service;
import nhnis.mg.jw.a.dto.mgjwa1002S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1002S0DTOout;

/**
 * 로그인 이력 Controller (TCF OFF 호환).
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class mgjwa1002Controller {

    @Autowired
    private mgjwa1002Service mgjwa1002Service;

    @PostMapping("/mgjwa1002S0")
    public mgjwa1002S0DTOout mgjwa1002S0(@RequestBody mgjwa1002S0DTOin input) throws Throwable {
        return mgjwa1002Service.mgjwa1002S0(input);
    }
}
