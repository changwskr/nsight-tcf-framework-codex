package nhnis.mg.co.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.co.a.dto.mgcoa5530S0DTOin;
import nhnis.mg.co.a.dto.mgcoa5530S0DTOout;
import nhnis.mg.co.a.application.service.mgcoa5530Service;

/**
 * 마케팅희망고객 조회 Facade.
 *
 * <p>Handler → Facade → Service 경계. {@code @Transactional} 은 Facade,
 * 업무 선후처리({@code BizPrePostAspect})는 Service 호출 전후에 적용된다.
 */
@Service
public class mgcoa5530Facade {

    private final mgcoa5530Service service;
    private final ObjectMapper objectMapper;

    public mgcoa5530Facade(mgcoa5530Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgcoa5530S0DTOout mgcoa5530S0(Object dtoBody) throws Exception {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        mgcoa5530S0DTOin input = objectMapper.convertValue(source, mgcoa5530S0DTOin.class);
        return service.mgcoa5530S0(input);
    }
}
