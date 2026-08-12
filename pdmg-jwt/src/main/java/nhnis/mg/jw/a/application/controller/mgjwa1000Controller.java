package nhnis.mg.jw.a.application.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nhnis.fw.commons.resolver.RequestBody;
import nhnis.mg.jw.a.application.service.mgjwa1000Service;
import nhnis.mg.jw.a.dto.mgjwa1000C0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000C0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1000C1DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000C1DTOout;
import nhnis.mg.jw.a.dto.mgjwa1000D0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000D0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1000D1DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000D1DTOout;
import nhnis.mg.jw.a.dto.mgjwa1000U0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1000U0DTOout;

/**
 * JWT 인증 Controller (TCF OFF 호환).
 *
 * <p>{@code nhnis.fw.tcf.enabled=true} 이면 단일 {@code OnlineTransactionController} 경로를 쓰고
 * 이 Controller 는 등록하지 않는다.
 */
@Slf4j
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "false", matchIfMissing = true)
public class mgjwa1000Controller {

    @Autowired
    private mgjwa1000Service mgjwa1000Service;

    @PostMapping("/mgjwa1000C0")
    public mgjwa1000C0DTOout mgjwa1000C0(@RequestBody mgjwa1000C0DTOin input) throws Throwable {
        return mgjwa1000Service.mgjwa1000C0(input);
    }

    @PostMapping("/mgjwa1000C1")
    public mgjwa1000C1DTOout mgjwa1000C1(@RequestBody mgjwa1000C1DTOin input) throws Throwable {
        return mgjwa1000Service.mgjwa1000C1(input);
    }

    @PostMapping("/mgjwa1000U0")
    public mgjwa1000U0DTOout mgjwa1000U0(@RequestBody mgjwa1000U0DTOin input) throws Throwable {
        return mgjwa1000Service.mgjwa1000U0(input);
    }

    @PostMapping("/mgjwa1000D0")
    public mgjwa1000D0DTOout mgjwa1000D0(@RequestBody mgjwa1000D0DTOin input) throws Throwable {
        return mgjwa1000Service.mgjwa1000D0(input);
    }

    @PostMapping("/mgjwa1000D1")
    public mgjwa1000D1DTOout mgjwa1000D1(@RequestBody mgjwa1000D1DTOin input) throws Throwable {
        return mgjwa1000Service.mgjwa1000D1(input);
    }
}
