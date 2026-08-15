package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina3110Service;
import nhnis.infra.in.a.dto.ifina3110C0DTOin;
import nhnis.infra.in.a.dto.ifina3110C0DTOout;
import nhnis.infra.in.a.dto.ifina3110D0DTOin;
import nhnis.infra.in.a.dto.ifina3110D0DTOout;
import nhnis.infra.in.a.dto.ifina3110S0DTOin;
import nhnis.infra.in.a.dto.ifina3110S0DTOout;
import nhnis.infra.in.a.dto.ifina3110U0DTOin;
import nhnis.infra.in.a.dto.ifina3110U0DTOout;

@Service
public class ifina3110Facade {

    private final ifina3110Service service;
    private final ObjectMapper objectMapper;

    public ifina3110Facade(ifina3110Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina3110S0DTOout ifina3110S0(Object dtoBody) throws Exception {
        return service.ifina3110S0(convert(dtoBody, ifina3110S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina3110C0DTOout ifina3110C0(Object dtoBody) throws Exception {
        return service.ifina3110C0(convert(dtoBody, ifina3110C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina3110U0DTOout ifina3110U0(Object dtoBody) throws Exception {
        return service.ifina3110U0(convert(dtoBody, ifina3110U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina3110D0DTOout ifina3110D0(Object dtoBody) throws Exception {
        return service.ifina3110D0(convert(dtoBody, ifina3110D0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
