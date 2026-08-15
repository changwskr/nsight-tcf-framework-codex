package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina3100Service;
import nhnis.infra.in.a.dto.ifina3100C0DTOin;
import nhnis.infra.in.a.dto.ifina3100C0DTOout;
import nhnis.infra.in.a.dto.ifina3100D0DTOin;
import nhnis.infra.in.a.dto.ifina3100D0DTOout;
import nhnis.infra.in.a.dto.ifina3100S0DTOin;
import nhnis.infra.in.a.dto.ifina3100S0DTOout;
import nhnis.infra.in.a.dto.ifina3100S1DTOin;
import nhnis.infra.in.a.dto.ifina3100S1DTOout;
import nhnis.infra.in.a.dto.ifina3100U0DTOin;
import nhnis.infra.in.a.dto.ifina3100U0DTOout;

@Service
public class ifina3100Facade {
    private final ifina3100Service service;
    private final ObjectMapper objectMapper;

    public ifina3100Facade(ifina3100Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina3100S0DTOout ifina3100S0(Object dtoBody) throws Exception {
        return service.ifina3100S0(convert(dtoBody, ifina3100S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina3100S1DTOout ifina3100S1(Object dtoBody) throws Exception {
        return service.ifina3100S1(convert(dtoBody, ifina3100S1DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina3100C0DTOout ifina3100C0(Object dtoBody) throws Exception {
        return service.ifina3100C0(convert(dtoBody, ifina3100C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina3100U0DTOout ifina3100U0(Object dtoBody) throws Exception {
        return service.ifina3100U0(convert(dtoBody, ifina3100U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina3100D0DTOout ifina3100D0(Object dtoBody) throws Exception {
        return service.ifina3100D0(convert(dtoBody, ifina3100D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
