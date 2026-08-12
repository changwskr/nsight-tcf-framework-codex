package nhnis.mg.co.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.mg.co.a.application.facade.mgcoa8888Facade;

/**
 * mgcoa8888 도메인 핸들러.
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class mgcoa8888Handler implements TransactionHandler {

    private static final String S0 = "mgcoa8888S0";
    private static final String D0 = "mgcoa8888D0";

    private final mgcoa8888Facade facade;

    public mgcoa8888Handler(mgcoa8888Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(S0, D0);
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case S0 -> facade.mgcoa8888S0(dtoBody);
            case D0 -> facade.mgcoa8888D0(dtoBody);
            default -> throw new ServiceHandlerNotFound(
                    "mgcoa8888Handler 미지원 serviceId: " + context.getServiceId());
        };
    }
}
