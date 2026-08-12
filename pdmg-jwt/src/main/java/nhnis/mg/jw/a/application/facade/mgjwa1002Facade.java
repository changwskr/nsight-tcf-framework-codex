package nhnis.mg.jw.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.jw.a.application.service.mgjwa1002Service;
import nhnis.mg.jw.a.dto.mgjwa1002S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1002S0DTOout;

/** 로그인 이력 Facade. */
@Service
public class mgjwa1002Facade {

    private final mgjwa1002Service service;
    private final ObjectMapper objectMapper;

    public mgjwa1002Facade(mgjwa1002Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgjwa1002S0DTOout mgjwa1002S0(Object dtoBody) {
        return service.mgjwa1002S0(convert(dtoBody, mgjwa1002S0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
