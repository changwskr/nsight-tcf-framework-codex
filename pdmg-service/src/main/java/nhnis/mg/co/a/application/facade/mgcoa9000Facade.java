package nhnis.mg.co.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.co.a.dto.mgcoa9000C0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000C0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000D0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000D0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000S0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000S0DTOout;
import nhnis.mg.co.a.dto.mgcoa9000U0DTOin;
import nhnis.mg.co.a.dto.mgcoa9000U0DTOout;
import nhnis.mg.co.a.application.service.mgcoa9000Service;

/**
 * 거래 파라미터 관리 Facade.
 */
@Service
public class mgcoa9000Facade {

    private final mgcoa9000Service service;
    private final ObjectMapper objectMapper;

    public mgcoa9000Facade(mgcoa9000Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public mgcoa9000S0DTOout mgcoa9000S0(Object dtoBody) throws Exception {
        return service.mgcoa9000S0(convert(dtoBody, mgcoa9000S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9000C0DTOout mgcoa9000C0(Object dtoBody) throws Exception {
        return service.mgcoa9000C0(convert(dtoBody, mgcoa9000C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9000U0DTOout mgcoa9000U0(Object dtoBody) throws Exception {
        return service.mgcoa9000U0(convert(dtoBody, mgcoa9000U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public mgcoa9000D0DTOout mgcoa9000D0(Object dtoBody) throws Exception {
        return service.mgcoa9000D0(convert(dtoBody, mgcoa9000D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
