package nhnis.mg.jw.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.jw.a.application.service.mgjwa1003Service;
import nhnis.mg.jw.a.dto.mgjwa1003S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1003S0DTOout;

/**
 * Refresh Token 조회 Controller (TCF OFF 호환).
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class mgjwa1003Controller {

    @Autowired
    private mgjwa1003Service mgjwa1003Service;

    @PostMapping("/mgjwa1003S0")
    public mgjwa1003S0DTOout mgjwa1003S0(@RequestBody mgjwa1003S0DTOin input) throws Throwable {
        return mgjwa1003Service.mgjwa1003S0(input);
    }
}
