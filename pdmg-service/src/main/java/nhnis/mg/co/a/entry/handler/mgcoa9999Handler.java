package nhnis.mg.co.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.mg.co.a.application.facade.mgcoa9999Facade;

/**
 * mgcoa9999 도메인 핸들러.
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class mgcoa9999Handler implements TransactionHandler {

    private static final String S0 = "mgcoa9999S0";

    private final mgcoa9999Facade facade;

    public mgcoa9999Handler(mgcoa9999Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(S0);
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        if (S0.equals(context.getServiceId())) {
            return facade.mgcoa9999S0(dtoBody);
        }
        throw new ServiceHandlerNotFound("mgcoa9999Handler 미지원 serviceId: " + context.getServiceId());
    }
}
