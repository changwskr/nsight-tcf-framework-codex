package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina8200Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina8200Facade {
    private final ifina8200Service service;
    private final ObjectMapper objectMapper;
    public ifina8200Facade(ifina8200Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina8200S0DTOout ifina8200S0(Object dtoBody) throws Exception {
        return service.ifina8200S0(convert(dtoBody, ifina8200S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifinaV0DTOout ifina8200V0(Object dtoBody) throws Exception {
        return service.ifina8200V0(convert(dtoBody, ifina8200S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina8200U0DTOout ifina8200U0(Object dtoBody) throws Exception {
        return service.ifina8200U0(convert(dtoBody, ifina8200U0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
