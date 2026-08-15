package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina3400Service;
import nhnis.infra.in.a.dto.ifina3400V0DTOin;
import nhnis.infra.in.a.dto.ifina3400V0DTOout;

@Service
public class ifina3400Facade {

    private final ifina3400Service service;
    private final ObjectMapper objectMapper;

    public ifina3400Facade(ifina3400Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina3400V0DTOout ifina3400V0(Object dtoBody) throws Exception {
        return service.ifina3400V0(convert(dtoBody));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina3400V0DTOout ifina3400C0(Object dtoBody) throws Exception {
        return service.ifina3400C0(convert(dtoBody));
    }

    private ifina3400V0DTOin convert(Object dtoBody) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, ifina3400V0DTOin.class);
    }
}
