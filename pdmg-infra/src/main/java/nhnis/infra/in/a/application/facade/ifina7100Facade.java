package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina7100Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina7100Facade {
    private final ifina7100Service service;
    private final ObjectMapper objectMapper;
    public ifina7100Facade(ifina7100Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina7100S0DTOout ifina7100S0(Object dtoBody) throws Exception {
        return service.ifina7100S0(convert(dtoBody, ifina7100S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina7100C0DTOout ifina7100C0(Object dtoBody) throws Exception {
        return service.ifina7100C0(convert(dtoBody, ifina7100C0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina7100U0DTOout ifina7100U0(Object dtoBody) throws Exception {
        return service.ifina7100U0(convert(dtoBody, ifina7100U0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina7100D0DTOout ifina7100D0(Object dtoBody) throws Exception {
        return service.ifina7100D0(convert(dtoBody, ifina7100D0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
