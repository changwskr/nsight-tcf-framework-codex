package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina7200Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina7200Facade {
    private final ifina7200Service service;
    private final ObjectMapper objectMapper;
    public ifina7200Facade(ifina7200Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina7200S0DTOout ifina7200S0(Object dtoBody) throws Exception {
        return service.ifina7200S0(convert(dtoBody, ifina7200S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina7200U0DTOout ifina7200U0(Object dtoBody) throws Exception {
        return service.ifina7200U0(convert(dtoBody, ifina7200U0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
