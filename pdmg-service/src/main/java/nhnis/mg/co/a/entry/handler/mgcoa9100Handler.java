package nhnis.mg.co.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.mg.co.a.application.facade.mgcoa9100Facade;

/**
 * mgcoa9100 도메인 핸들러 (런타임 진단).
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class mgcoa9100Handler implements TransactionHandler {

    private static final String S0 = "mgcoa9100S0";

    private final mgcoa9100Facade facade;

    public mgcoa9100Handler(mgcoa9100Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(S0);
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) {
        return switch (context.getServiceId()) {
            case S0 -> facade.mgcoa9100S0(dtoBody);
            default -> throw new ServiceHandlerNotFound(
                    "mgcoa9100Handler 미지원 serviceId: " + context.getServiceId());
        };
    }
}
