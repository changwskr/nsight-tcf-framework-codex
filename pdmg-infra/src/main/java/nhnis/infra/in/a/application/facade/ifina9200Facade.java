package nhnis.infra.in.a.application.facade;

import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import nhnis.infra.in.a.application.service.ifina9200Service;
import nhnis.infra.in.a.dto.*;

@Service
public class ifina9200Facade {
    private final ifina9200Service service;
    private final ObjectMapper objectMapper;
    public ifina9200Facade(ifina9200Service service, ObjectMapper objectMapper) {
        this.service = service; this.objectMapper = objectMapper;
    }
    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina9200S0DTOout ifina9200S0(Object dtoBody) throws Exception {
        return service.ifina9200S0(convert(dtoBody, ifina9200S0DTOin.class));
    }
    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina9200U0DTOout ifina9200U0(Object dtoBody) throws Exception {
        return service.ifina9200U0(convert(dtoBody, ifina9200U0DTOin.class));
    }
    private <T> T convert(Object dtoBody, Class<T> type) {
        return objectMapper.convertValue(dtoBody == null ? Collections.emptyMap() : dtoBody, type);
    }
}
