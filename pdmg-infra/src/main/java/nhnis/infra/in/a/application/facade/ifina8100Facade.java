package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina8100Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina8100Facade {
    private final ifina8100Service service;
    private final ObjectMapper objectMapper;
    public ifina8100Facade(ifina8100Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina8100S0DTOout ifina8100S0(Object dtoBody) throws Exception {
        return service.ifina8100S0(convert(dtoBody, ifina8100S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina8100C0DTOout ifina8100C0(Object dtoBody) throws Exception {
        return service.ifina8100C0(convert(dtoBody, ifina8100C0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina8100U0DTOout ifina8100U0(Object dtoBody) throws Exception {
        return service.ifina8100U0(convert(dtoBody, ifina8100U0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina8100D0DTOout ifina8100D0(Object dtoBody) throws Exception {
        return service.ifina8100D0(convert(dtoBody, ifina8100D0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
