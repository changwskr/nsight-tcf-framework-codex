package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina6400Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina6400Facade {
    private final ifina6400Service service;
    private final ObjectMapper objectMapper;
    public ifina6400Facade(ifina6400Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina6400S0DTOout ifina6400S0(Object dtoBody) throws Exception {
        return service.ifina6400S0(convert(dtoBody, ifina6400S0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
