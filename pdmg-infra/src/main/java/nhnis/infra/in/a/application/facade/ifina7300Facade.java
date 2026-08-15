package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina7300Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina7300Facade {
    private final ifina7300Service service;
    private final ObjectMapper objectMapper;
    public ifina7300Facade(ifina7300Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina7300S0DTOout ifina7300S0(Object dtoBody) throws Exception {
        return service.ifina7300S0(convert(dtoBody, ifina7300S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina7300C0DTOout ifina7300C0(Object dtoBody) throws Exception {
        return service.ifina7300C0(convert(dtoBody, ifina7300C0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
