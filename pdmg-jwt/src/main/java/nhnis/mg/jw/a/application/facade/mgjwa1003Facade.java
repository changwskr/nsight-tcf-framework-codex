package nhnis.mg.jw.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.jw.a.application.service.mgjwa1003Service;
import nhnis.mg.jw.a.dto.mgjwa1003S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1003S0DTOout;

/** Refresh Token 조회 Facade. */
@Service
public class mgjwa1003Facade {

    private final mgjwa1003Service service;
    private final ObjectMapper objectMapper;

    public mgjwa1003Facade(mgjwa1003Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgjwa1003S0DTOout mgjwa1003S0(Object dtoBody) {
        return service.mgjwa1003S0(convert(dtoBody, mgjwa1003S0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
