package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina4200Service;
import nhnis.infra.in.a.dto.ifina4200C0DTOin;
import nhnis.infra.in.a.dto.ifina4200C0DTOout;
import nhnis.infra.in.a.dto.ifina4200D0DTOin;
import nhnis.infra.in.a.dto.ifina4200D0DTOout;
import nhnis.infra.in.a.dto.ifina4200S0DTOin;
import nhnis.infra.in.a.dto.ifina4200S0DTOout;
import nhnis.infra.in.a.dto.ifina4200U0DTOin;
import nhnis.infra.in.a.dto.ifina4200U0DTOout;

@Service
public class ifina4200Facade {
    private final ifina4200Service service;
    private final ObjectMapper objectMapper;

    public ifina4200Facade(ifina4200Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina4200S0DTOout ifina4200S0(Object dtoBody) throws Exception {
        return service.ifina4200S0(convert(dtoBody, ifina4200S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina4200C0DTOout ifina4200C0(Object dtoBody) throws Exception {
        return service.ifina4200C0(convert(dtoBody, ifina4200C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina4200U0DTOout ifina4200U0(Object dtoBody) throws Exception {
        return service.ifina4200U0(convert(dtoBody, ifina4200U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina4200D0DTOout ifina4200D0(Object dtoBody) throws Exception {
        return service.ifina4200D0(convert(dtoBody, ifina4200D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
