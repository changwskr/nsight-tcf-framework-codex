package nhnis.mg.jw.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.jw.a.application.service.mgjwa1004Service;
import nhnis.mg.jw.a.dto.mgjwa1004S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1004S0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1004U0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1004U0DTOout;

/** 보안정책 조회·수정 Facade. */
@Service
public class mgjwa1004Facade {

    private final mgjwa1004Service service;
    private final ObjectMapper objectMapper;

    public mgjwa1004Facade(mgjwa1004Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgjwa1004S0DTOout mgjwa1004S0(Object dtoBody) {
        return service.mgjwa1004S0(convert(dtoBody, mgjwa1004S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgjwa1004U0DTOout mgjwa1004U0(Object dtoBody) {
        return service.mgjwa1004U0(convert(dtoBody, mgjwa1004U0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
