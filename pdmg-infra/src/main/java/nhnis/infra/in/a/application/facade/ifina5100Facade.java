package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina5100Service;
import nhnis.infra.in.a.dto.ifina5100C0DTOin;
import nhnis.infra.in.a.dto.ifina5100C0DTOout;
import nhnis.infra.in.a.dto.ifina5100D0DTOin;
import nhnis.infra.in.a.dto.ifina5100D0DTOout;
import nhnis.infra.in.a.dto.ifina5100S0DTOin;
import nhnis.infra.in.a.dto.ifina5100S0DTOout;
import nhnis.infra.in.a.dto.ifina5100U0DTOin;
import nhnis.infra.in.a.dto.ifina5100U0DTOout;

@Service
public class ifina5100Facade {
    private final ifina5100Service service;
    private final ObjectMapper objectMapper;

    public ifina5100Facade(ifina5100Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina5100S0DTOout ifina5100S0(Object dtoBody) throws Exception {
        return service.ifina5100S0(convert(dtoBody, ifina5100S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina5100C0DTOout ifina5100C0(Object dtoBody) throws Exception {
        return service.ifina5100C0(convert(dtoBody, ifina5100C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina5100U0DTOout ifina5100U0(Object dtoBody) throws Exception {
        return service.ifina5100U0(convert(dtoBody, ifina5100U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina5100D0DTOout ifina5100D0(Object dtoBody) throws Exception {
        return service.ifina5100D0(convert(dtoBody, ifina5100D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
