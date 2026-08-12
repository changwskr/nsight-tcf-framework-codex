package nhnis.mg.jw.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.jw.a.application.service.mgjwa1004Service;
import nhnis.mg.jw.a.dto.mgjwa1004S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1004S0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1004U0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1004U0DTOout;

/**
 * 보안정책 조회·수정 Controller (TCF OFF 호환).
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class mgjwa1004Controller {

    @Autowired
    private mgjwa1004Service mgjwa1004Service;

    @PostMapping("/mgjwa1004S0")
    public mgjwa1004S0DTOout mgjwa1004S0(@RequestBody mgjwa1004S0DTOin input) throws Throwable {
        return mgjwa1004Service.mgjwa1004S0(input);
    }

    @PostMapping("/mgjwa1004U0")
    public mgjwa1004U0DTOout mgjwa1004U0(@RequestBody mgjwa1004U0DTOin input) throws Throwable {
        return mgjwa1004Service.mgjwa1004U0(input);
    }
}
