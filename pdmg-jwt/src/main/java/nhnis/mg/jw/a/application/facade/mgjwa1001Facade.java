package nhnis.mg.jw.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.jw.a.application.service.mgjwa1001Service;
import nhnis.mg.jw.a.dto.mgjwa1001D0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1001D0DTOout;
import nhnis.mg.jw.a.dto.mgjwa1001S0DTOin;
import nhnis.mg.jw.a.dto.mgjwa1001S0DTOout;

/** 토큰 현황 조회·강제폐기 Facade. */
@Service
public class mgjwa1001Facade {

    private final mgjwa1001Service service;
    private final ObjectMapper objectMapper;

    public mgjwa1001Facade(mgjwa1001Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgjwa1001S0DTOout mgjwa1001S0(Object dtoBody) {
        return service.mgjwa1001S0(convert(dtoBody, mgjwa1001S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgjwa1001D0DTOout mgjwa1001D0(Object dtoBody) {
        return service.mgjwa1001D0(convert(dtoBody, mgjwa1001D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
