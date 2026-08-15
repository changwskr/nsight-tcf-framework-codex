package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina5300Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina5300Facade {
    private final ifina5300Service service;
    private final ObjectMapper objectMapper;
    public ifina5300Facade(ifina5300Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina5300S0DTOout ifina5300S0(Object dtoBody) throws Exception {
        return service.ifina5300S0(convert(dtoBody, ifina5300S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina5300C0DTOout ifina5300C0(Object dtoBody) throws Exception {
        return service.ifina5300C0(convert(dtoBody, ifina5300C0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina5300D0DTOout ifina5300D0(Object dtoBody) throws Exception {
        return service.ifina5300D0(convert(dtoBody, ifina5300D0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
