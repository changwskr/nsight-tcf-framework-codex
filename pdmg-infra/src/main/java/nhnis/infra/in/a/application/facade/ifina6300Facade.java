package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina6300Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina6300Facade {
    private final ifina6300Service service;
    private final ObjectMapper objectMapper;
    public ifina6300Facade(ifina6300Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina6300S0DTOout ifina6300S0(Object dtoBody) throws Exception {
        return service.ifina6300S0(convert(dtoBody, ifina6300S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifinaV0DTOout ifina6300V0(Object dtoBody) throws Exception {
        return service.ifina6300V0(convert(dtoBody, ifina6300S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina6300U0DTOout ifina6300U0(Object dtoBody) throws Exception {
        return service.ifina6300U0(convert(dtoBody, ifina6300U0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
