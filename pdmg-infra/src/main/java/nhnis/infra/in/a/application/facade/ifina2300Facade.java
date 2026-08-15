package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina2300Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina2300Facade {
    private final ifina2300Service service;
    private final ObjectMapper objectMapper;
    public ifina2300Facade(ifina2300Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina2300S0DTOout ifina2300S0(Object dtoBody) throws Exception {
        return service.ifina2300S0(convert(dtoBody, ifina2300S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina2300C0DTOout ifina2300C0(Object dtoBody) throws Exception {
        return service.ifina2300C0(convert(dtoBody, ifina2300C0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina2300D0DTOout ifina2300D0(Object dtoBody) throws Exception {
        return service.ifina2300D0(convert(dtoBody, ifina2300D0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
