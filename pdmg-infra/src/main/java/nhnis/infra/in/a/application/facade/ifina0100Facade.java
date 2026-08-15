package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina0100Service;
import nhnis.infra.in.a.dto.ifina0100S0DTOin;
import nhnis.infra.in.a.dto.ifina0100S0DTOout;

@Service
public class ifina0100Facade {
    private final ifina0100Service service;
    private final ObjectMapper objectMapper;

    public ifina0100Facade(ifina0100Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina0100S0DTOout ifina0100S0(Object dtoBody) throws Exception {
        return service.ifina0100S0(convert(dtoBody, ifina0100S0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
