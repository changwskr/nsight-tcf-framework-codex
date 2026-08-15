package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina1100Service;
import nhnis.infra.in.a.dto.ifina1100C0DTOin;
import nhnis.infra.in.a.dto.ifina1100C0DTOout;
import nhnis.infra.in.a.dto.ifina1100S0DTOin;
import nhnis.infra.in.a.dto.ifina1100S0DTOout;
import nhnis.infra.in.a.dto.ifina1100U0DTOin;
import nhnis.infra.in.a.dto.ifina1100U0DTOout;

@Service
public class ifina1100Facade {
    private final ifina1100Service service;
    private final ObjectMapper objectMapper;

    public ifina1100Facade(ifina1100Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina1100S0DTOout ifina1100S0(Object dtoBody) throws Exception {
        return service.ifina1100S0(convert(dtoBody, ifina1100S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1100C0DTOout ifina1100C0(Object dtoBody) throws Exception {
        return service.ifina1100C0(convert(dtoBody, ifina1100C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1100U0DTOout ifina1100U0(Object dtoBody) throws Exception {
        return service.ifina1100U0(convert(dtoBody, ifina1100U0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
