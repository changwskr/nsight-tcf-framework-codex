package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina1600Service;
import nhnis.infra.in.a.dto.ifina1600C0DTOin;
import nhnis.infra.in.a.dto.ifina1600C0DTOout;
import nhnis.infra.in.a.dto.ifina1600S0DTOin;
import nhnis.infra.in.a.dto.ifina1600S0DTOout;

@Service
public class ifina1600Facade {
    private final ifina1600Service service;
    private final ObjectMapper objectMapper;

    public ifina1600Facade(ifina1600Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina1600S0DTOout ifina1600S0(Object dtoBody) throws Exception {
        return service.ifina1600S0(convert(dtoBody, ifina1600S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1600C0DTOout ifina1600C0(Object dtoBody) throws Exception {
        return service.ifina1600C0(convert(dtoBody, ifina1600C0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
