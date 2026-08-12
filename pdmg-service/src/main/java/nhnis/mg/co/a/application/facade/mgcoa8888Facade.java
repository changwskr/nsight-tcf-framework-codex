package nhnis.mg.co.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.co.a.dto.mgcoa8888D0DTOin;
import nhnis.mg.co.a.dto.mgcoa8888D0DTOout;
import nhnis.mg.co.a.dto.mgcoa8888S0DTOin;
import nhnis.mg.co.a.dto.mgcoa8888S0DTOout;
import nhnis.mg.co.a.application.service.mgcoa8888Service;

/**
 * 이미지로그 조회/삭제 Facade.
 *
 * <p>{@code @Transactional} 은 Facade, 업무 선후처리({@code BizPrePostAspect})는 Service 전후다.
 */
@Service
public class mgcoa8888Facade {

    private final mgcoa8888Service service;
    private final ObjectMapper objectMapper;

    public mgcoa8888Facade(mgcoa8888Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgcoa8888S0DTOout mgcoa8888S0(Object dtoBody) throws Exception {
        mgcoa8888S0DTOin input = convert(dtoBody, mgcoa8888S0DTOin.class);
        return service.mgcoa8888S0(input);
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa8888D0DTOout mgcoa8888D0(Object dtoBody) throws Exception {
        mgcoa8888D0DTOin input = convert(dtoBody, mgcoa8888D0DTOin.class);
        return service.mgcoa8888D0(input);
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
