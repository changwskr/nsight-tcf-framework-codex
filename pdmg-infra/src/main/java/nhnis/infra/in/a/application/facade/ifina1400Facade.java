package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina1400Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina1400Facade {
    private final ifina1400Service service;
    private final ObjectMapper objectMapper;
    public ifina1400Facade(ifina1400Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina1400S0DTOout ifina1400S0(Object dtoBody) throws Exception {
        return service.ifina1400S0(convert(dtoBody, ifina1400S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1400C0DTOout ifina1400C0(Object dtoBody) throws Exception {
        return service.ifina1400C0(convert(dtoBody, ifina1400C0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1400U0DTOout ifina1400U0(Object dtoBody) throws Exception {
        return service.ifina1400U0(convert(dtoBody, ifina1400U0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
