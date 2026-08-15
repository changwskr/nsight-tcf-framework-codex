package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina1200Service;
import nhnis.infra.in.a.dto.ifina1200C0DTOin;
import nhnis.infra.in.a.dto.ifina1200C0DTOout;
import nhnis.infra.in.a.dto.ifina1200D0DTOin;
import nhnis.infra.in.a.dto.ifina1200D0DTOout;
import nhnis.infra.in.a.dto.ifina1200S0DTOin;
import nhnis.infra.in.a.dto.ifina1200S0DTOout;
import nhnis.infra.in.a.dto.ifina1200U0DTOin;
import nhnis.infra.in.a.dto.ifina1200U0DTOout;

@Service
public class ifina1200Facade {
    private final ifina1200Service service;
    private final ObjectMapper objectMapper;

    public ifina1200Facade(ifina1200Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina1200S0DTOout ifina1200S0(Object dtoBody) throws Exception {
        return service.ifina1200S0(convert(dtoBody, ifina1200S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1200C0DTOout ifina1200C0(Object dtoBody) throws Exception {
        return service.ifina1200C0(convert(dtoBody, ifina1200C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1200U0DTOout ifina1200U0(Object dtoBody) throws Exception {
        return service.ifina1200U0(convert(dtoBody, ifina1200U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1200D0DTOout ifina1200D0(Object dtoBody) throws Exception {
        return service.ifina1200D0(convert(dtoBody, ifina1200D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
