package nhnis.infra.in.a.application.facade;
import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina4100Service;
import nhnis.infra.in.a.dto.*;
@Service
public class ifina4100Facade {
    private final ifina4100Service service; private final ObjectMapper objectMapper;
    public ifina4100Facade(ifina4100Service service, ObjectMapper objectMapper) { this.service = service; this.objectMapper = objectMapper; }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina4100S0DTOout ifina4100S0(Object dtoBody) throws Exception {
        return service.ifina4100S0(convert(dtoBody, ifina4100S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina4100C0DTOout ifina4100C0(Object dtoBody) throws Exception {
        return service.ifina4100C0(convert(dtoBody, ifina4100C0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina4100U0DTOout ifina4100U0(Object dtoBody) throws Exception {
        return service.ifina4100U0(convert(dtoBody, ifina4100U0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina4100D0DTOout ifina4100D0(Object dtoBody) throws Exception {
        return service.ifina4100D0(convert(dtoBody, ifina4100D0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
