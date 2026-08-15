package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina6100Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina6100Facade {
    private final ifina6100Service service;
    private final ObjectMapper objectMapper;
    public ifina6100Facade(ifina6100Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina6100S0DTOout ifina6100S0(Object dtoBody) throws Exception {
        return service.ifina6100S0(convert(dtoBody, ifina6100S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifinaV0DTOout ifina6100V0(Object dtoBody) throws Exception {
        return service.ifina6100V0(convert(dtoBody, ifina6100S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina6100U0DTOout ifina6100U0(Object dtoBody) throws Exception {
        return service.ifina6100U0(convert(dtoBody, ifina6100U0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
