package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina4300Service;
import nhnis.infra.in.a.dto.ifina4300S0DTOin;
import nhnis.infra.in.a.dto.ifina4300S0DTOout;

@Service
public class ifina4300Facade {
    private final ifina4300Service service;
    private final ObjectMapper objectMapper;
    public ifina4300Facade(ifina4300Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina4300S0DTOout ifina4300S0(Object dtoBody) throws Exception {
        return service.ifina4300S0(convert(dtoBody, ifina4300S0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
