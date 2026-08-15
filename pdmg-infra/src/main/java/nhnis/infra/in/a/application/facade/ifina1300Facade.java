package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina1300Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina1300Facade {
    private final ifina1300Service service;
    private final ObjectMapper objectMapper;
    public ifina1300Facade(ifina1300Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina1300S0DTOout ifina1300S0(Object dtoBody) throws Exception {
        return service.ifina1300S0(convert(dtoBody, ifina1300S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1300C0DTOout ifina1300C0(Object dtoBody) throws Exception {
        return service.ifina1300C0(convert(dtoBody, ifina1300C0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1300U0DTOout ifina1300U0(Object dtoBody) throws Exception {
        return service.ifina1300U0(convert(dtoBody, ifina1300U0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
