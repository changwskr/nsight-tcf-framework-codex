package nhnis.mg.co.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.co.a.application.service.mgcoa9001Service;
import nhnis.mg.co.a.dto.mgcoa9001C0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001C0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001D0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001D0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001S0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001S0DTOout;
import nhnis.mg.co.a.dto.mgcoa9001U0DTOin;
import nhnis.mg.co.a.dto.mgcoa9001U0DTOout;

/**
 * 거래통제 Facade.
 */
@Service
public class mgcoa9001Facade {

    private final mgcoa9001Service service;
    private final ObjectMapper objectMapper;

    public mgcoa9001Facade(mgcoa9001Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgcoa9001S0DTOout mgcoa9001S0(Object dtoBody) throws Exception {
        return service.mgcoa9001S0(convert(dtoBody, mgcoa9001S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9001C0DTOout mgcoa9001C0(Object dtoBody) throws Exception {
        return service.mgcoa9001C0(convert(dtoBody, mgcoa9001C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9001U0DTOout mgcoa9001U0(Object dtoBody) throws Exception {
        return service.mgcoa9001U0(convert(dtoBody, mgcoa9001U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9001D0DTOout mgcoa9001D0(Object dtoBody) throws Exception {
        return service.mgcoa9001D0(convert(dtoBody, mgcoa9001D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
