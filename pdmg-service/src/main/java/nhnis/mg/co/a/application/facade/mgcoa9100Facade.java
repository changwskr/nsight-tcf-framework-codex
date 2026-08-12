package nhnis.mg.co.a.application.facade;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import nhnis.mg.co.a.application.service.mgcoa9100Service;
import nhnis.mg.co.a.dto.mgcoa9100S0DTOin;

/**
 * 런타임 진단 Facade.
 */
@Service
public class mgcoa9100Facade {

    private final mgcoa9100Service service;
    private final ObjectMapper objectMapper;

    public mgcoa9100Facade(mgcoa9100Service service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> mgcoa9100S0(Object dtoBody) {
        mgcoa9100S0DTOin in = convert(dtoBody, mgcoa9100S0DTOin.class);
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("includeDetails", in.getIncludeDetails() == null ? "Y" : in.getIncludeDetails());
        return service.inquiry(body);
    }

    private <T> T convert(Object dtoBody, Class<T> type) {
        Object source = dtoBody == null ? Collections.emptyMap() : dtoBody;
        return objectMapper.convertValue(source, type);
    }
}
