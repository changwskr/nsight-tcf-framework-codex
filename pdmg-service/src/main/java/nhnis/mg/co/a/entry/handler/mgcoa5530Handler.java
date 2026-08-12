package nhnis.mg.co.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.mg.co.a.application.facade.mgcoa5530Facade;

/**
 * mgcoa5530 도메인 핸들러. serviceId → Facade 로 연결한다.
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class mgcoa5530Handler implements TransactionHandler {

    private static final String S0 = "mgcoa5530S0";

    private final mgcoa5530Facade facade;

    public mgcoa5530Handler(mgcoa5530Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(S0);
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        String serviceId = context.getServiceId();
        if (S0.equals(serviceId)) {
            return facade.mgcoa5530S0(dtoBody);
        }
        throw new ServiceHandlerNotFound("mgcoa5530Handler 미지원 serviceId: " + serviceId);
    }
}
