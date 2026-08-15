package nhnis.infra.in.a.application.facade;

import java.util.Collections;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.infra.in.a.application.service.ifina1999Service;
import nhnis.infra.in.a.dto.ifina1999C0DTOin;
import nhnis.infra.in.a.dto.ifina1999C0DTOout;
import nhnis.infra.in.a.dto.ifina1999D0DTOin;
import nhnis.infra.in.a.dto.ifina1999D0DTOout;
import nhnis.infra.in.a.dto.ifina1999E0DTOin;
import nhnis.infra.in.a.dto.ifina1999E0DTOout;
import nhnis.infra.in.a.dto.ifina1999S0DTOin;
import nhnis.infra.in.a.dto.ifina1999S0DTOout;
import nhnis.infra.in.a.dto.ifina1999U0DTOin;
import nhnis.infra.in.a.dto.ifina1999U0DTOout;

/**
 * 서버 인벤토리 파일럿 Facade.
 */
@Service
public class ifina1999Facade {

    private final ifina1999Service service;
    private final ObjectMapper objectMapper;

    public ifina1999Facade(ifina1999Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
    public ifina1999S0DTOout ifina1999S0(Object dtoBody) throws Exception {
        return service.ifina1999S0(convert(dtoBody, ifina1999S0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1999C0DTOout ifina1999C0(Object dtoBody) throws Exception {
        return service.ifina1999C0(convert(dtoBody, ifina1999C0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1999U0DTOout ifina1999U0(Object dtoBody) throws Exception {
        return service.ifina1999U0(convert(dtoBody, ifina1999U0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1999D0DTOout ifina1999D0(Object dtoBody) throws Exception {
        return service.ifina1999D0(convert(dtoBody, ifina1999D0DTOin.class));
    }

    @Transactional(transactionManager = "rdwTransactionManager", rollbackFor = Exception.class)
    public ifina1999E0DTOout ifina1999E0(Object dtoBody) throws Exception {
        return service.ifina1999E0(convert(dtoBody, ifina1999E0DTOin.class));
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
