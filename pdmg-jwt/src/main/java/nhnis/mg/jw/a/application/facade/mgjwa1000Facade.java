package nhnis.mg.jw.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

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
 * JWT 인증 Facade.
 *
 * <p>{@code @Transactional} 은 Facade, 업무 선후처리({@code BizPrePostAspect})는 Service 전후다.
 */
@Service
public class mgjwa1000Facade {

    private final mgjwa1000Service service;
    private final ObjectMapper objectMapper;

    public mgjwa1000Facade(mgjwa1000Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgjwa1000C0DTOout mgjwa1000C0(Object dtoBody) {
        return service.mgjwa1000C0(convert(dtoBody, mgjwa1000C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgjwa1000C1DTOout mgjwa1000C1(Object dtoBody) {
        return service.mgjwa1000C1(convert(dtoBody, mgjwa1000C1DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgjwa1000U0DTOout mgjwa1000U0(Object dtoBody) {
        return service.mgjwa1000U0(convert(dtoBody, mgjwa1000U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgjwa1000D0DTOout mgjwa1000D0(Object dtoBody) {
        return service.mgjwa1000D0(convert(dtoBody, mgjwa1000D0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgjwa1000D1DTOout mgjwa1000D1(Object dtoBody) {
        return service.mgjwa1000D1(convert(dtoBody, mgjwa1000D1DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
