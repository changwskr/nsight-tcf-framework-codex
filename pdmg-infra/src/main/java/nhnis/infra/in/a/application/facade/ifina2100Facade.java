package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina2100Service;
import nhnis.infra.in.a.dto.ifina2100C0DTOin;
import nhnis.infra.in.a.dto.ifina2100C0DTOout;
import nhnis.infra.in.a.dto.ifina2100D0DTOin;
import nhnis.infra.in.a.dto.ifina2100D0DTOout;
import nhnis.infra.in.a.dto.ifina2100S0DTOin;
import nhnis.infra.in.a.dto.ifina2100S0DTOout;
import nhnis.infra.in.a.dto.ifina2100U0DTOin;
import nhnis.infra.in.a.dto.ifina2100U0DTOout;

@Service
public class ifina2100Facade {

    private final ifina2100Service service;
    private final ObjectMapper objectMapper;

    public ifina2100Facade(ifina2100Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina2100S0DTOout ifina2100S0(Object dtoBody) throws Exception {
        return service.ifina2100S0(convert(dtoBody, ifina2100S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina2100C0DTOout ifina2100C0(Object dtoBody) throws Exception {
        return service.ifina2100C0(convert(dtoBody, ifina2100C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina2100U0DTOout ifina2100U0(Object dtoBody) throws Exception {
        return service.ifina2100U0(convert(dtoBody, ifina2100U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina2100D0DTOout ifina2100D0(Object dtoBody) throws Exception {
        return service.ifina2100D0(convert(dtoBody, ifina2100D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
