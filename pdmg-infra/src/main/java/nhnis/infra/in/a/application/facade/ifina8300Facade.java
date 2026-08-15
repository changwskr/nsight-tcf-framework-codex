package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina8300Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina8300Facade {
    private final ifina8300Service service;
    private final ObjectMapper objectMapper;
    public ifina8300Facade(ifina8300Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina8300S0DTOout ifina8300S0(Object dtoBody) throws Exception {
        return service.ifina8300S0(convert(dtoBody, ifina8300S0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
