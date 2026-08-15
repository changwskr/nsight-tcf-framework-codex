package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina5200Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina5200Facade {
    private final ifina5200Service service;
    private final ObjectMapper objectMapper;
    public ifina5200Facade(ifina5200Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina5200S0DTOout ifina5200S0(Object dtoBody) throws Exception {
        return service.ifina5200S0(convert(dtoBody, ifina5200S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina5200C0DTOout ifina5200C0(Object dtoBody) throws Exception {
        return service.ifina5200C0(convert(dtoBody, ifina5200C0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina5200U0DTOout ifina5200U0(Object dtoBody) throws Exception {
        return service.ifina5200U0(convert(dtoBody, ifina5200U0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina5200D0DTOout ifina5200D0(Object dtoBody) throws Exception {
        return service.ifina5200D0(convert(dtoBody, ifina5200D0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
