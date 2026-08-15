package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina1500Service;
import nhnis.infra.in.a.dto.ifina1500C0DTOin;
import nhnis.infra.in.a.dto.ifina1500C0DTOout;
import nhnis.infra.in.a.dto.ifina1500E0DTOin;
import nhnis.infra.in.a.dto.ifina1500E0DTOout;
import nhnis.infra.in.a.dto.ifina1500S0DTOin;
import nhnis.infra.in.a.dto.ifina1500S0DTOout;
import nhnis.infra.in.a.dto.ifina1500U0DTOin;
import nhnis.infra.in.a.dto.ifina1500U0DTOout;

@Service
public class ifina1500Facade {
    private final ifina1500Service service;
    private final ObjectMapper objectMapper;

    public ifina1500Facade(ifina1500Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina1500S0DTOout ifina1500S0(Object dtoBody) throws Exception {
        return service.ifina1500S0(convert(dtoBody, ifina1500S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1500C0DTOout ifina1500C0(Object dtoBody) throws Exception {
        return service.ifina1500C0(convert(dtoBody, ifina1500C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1500U0DTOout ifina1500U0(Object dtoBody) throws Exception {
        return service.ifina1500U0(convert(dtoBody, ifina1500U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1500E0DTOout ifina1500E0(Object dtoBody) throws Exception {
        return service.ifina1500E0(convert(dtoBody, ifina1500E0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
