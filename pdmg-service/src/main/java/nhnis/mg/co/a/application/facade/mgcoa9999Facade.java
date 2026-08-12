package nhnis.mg.co.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.co.a.dto.mgcoa9999S0DTOin;
import nhnis.mg.co.a.dto.mgcoa9999S0DTOout;
import nhnis.mg.co.a.application.service.mgcoa9999Service;

/**
 * 샘플 조회 Facade.
 *
 * <p>{@code @Transactional} 은 Facade, 업무 선후처리({@code BizPrePostAspect})는 Service 전후다.
 */
@Service
public class mgcoa9999Facade {

    private final mgcoa9999Service service;
    private final ObjectMapper objectMapper;

    public mgcoa9999Facade(mgcoa9999Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgcoa9999S0DTOout mgcoa9999S0(Object dtoBody) throws Exception {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        mgcoa9999S0DTOin input = objectMapper.convertValue(source, mgcoa9999S0DTOin.class);
        return service.mgcoa9999S0(input);
    }
}
