package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina6200Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina6200Facade {
    private final ifina6200Service service;
    private final ObjectMapper objectMapper;
    public ifina6200Facade(ifina6200Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina6200S0DTOout ifina6200S0(Object dtoBody) throws Exception {
        return service.ifina6200S0(convert(dtoBody, ifina6200S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifinaV0DTOout ifina6200V0(Object dtoBody) throws Exception {
        return service.ifina6200V0(convert(dtoBody, ifina6200S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina6200U0DTOout ifina6200U0(Object dtoBody) throws Exception {
        return service.ifina6200U0(convert(dtoBody, ifina6200U0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
