package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina9400Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina9400Facade {
    private final ifina9400Service service;
    private final ObjectMapper objectMapper;
    public ifina9400Facade(ifina9400Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina9400S0DTOout ifina9400S0(Object dtoBody) throws Exception {
        return service.ifina9400S0(convert(dtoBody, ifina9400S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina9400E0DTOout ifina9400E0(Object dtoBody) throws Exception {
        return service.ifina9400E0(convert(dtoBody, ifina9400E0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
