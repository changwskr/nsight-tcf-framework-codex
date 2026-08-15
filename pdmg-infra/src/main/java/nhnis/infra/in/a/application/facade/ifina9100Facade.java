package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina9100Service;
import nhnis.infra.in.a.dto.ifina9100S0DTOin;
import nhnis.infra.in.a.dto.ifina9100S0DTOout;
import nhnis.infra.in.a.dto.ifina9100U0DTOin;
import nhnis.infra.in.a.dto.ifina9100U0DTOout;

@Service
public class ifina9100Facade {

    private final ifina9100Service service;
    private final ObjectMapper objectMapper;

    public ifina9100Facade(ifina9100Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina9100S0DTOout ifina9100S0(Object dtoBody) throws Exception {
        return service.ifina9100S0(convert(dtoBody, ifina9100S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina9100U0DTOout ifina9100U0(Object dtoBody) throws Exception {
        return service.ifina9100U0(convert(dtoBody, ifina9100U0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
