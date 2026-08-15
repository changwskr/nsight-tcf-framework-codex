package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina2200Service;
import nhnis.infra.in.a.dto.ifina2200C0DTOin;
import nhnis.infra.in.a.dto.ifina2200C0DTOout;
import nhnis.infra.in.a.dto.ifina2200D0DTOin;
import nhnis.infra.in.a.dto.ifina2200D0DTOout;
import nhnis.infra.in.a.dto.ifina2200S0DTOin;
import nhnis.infra.in.a.dto.ifina2200S0DTOout;
import nhnis.infra.in.a.dto.ifina2200U0DTOin;
import nhnis.infra.in.a.dto.ifina2200U0DTOout;

@Service
public class ifina2200Facade {

    private final ifina2200Service service;
    private final ObjectMapper objectMapper;

    public ifina2200Facade(ifina2200Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina2200S0DTOout ifina2200S0(Object dtoBody) throws Exception {
        return service.ifina2200S0(convert(dtoBody, ifina2200S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina2200C0DTOout ifina2200C0(Object dtoBody) throws Exception {
        return service.ifina2200C0(convert(dtoBody, ifina2200C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina2200U0DTOout ifina2200U0(Object dtoBody) throws Exception {
        return service.ifina2200U0(convert(dtoBody, ifina2200U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina2200D0DTOout ifina2200D0(Object dtoBody) throws Exception {
        return service.ifina2200D0(convert(dtoBody, ifina2200D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
