package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina9300Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina9300Facade {
    private final ifina9300Service service;
    private final ObjectMapper objectMapper;
    public ifina9300Facade(ifina9300Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina9300S0DTOout ifina9300S0(Object dtoBody) throws Exception {
        return service.ifina9300S0(convert(dtoBody, ifina9300S0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
