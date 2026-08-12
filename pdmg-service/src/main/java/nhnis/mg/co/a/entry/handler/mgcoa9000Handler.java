package nhnis.mg.co.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.mg.co.a.application.facade.mgcoa9000Facade;

/**
 * mgcoa9000 도메인 핸들러 (거래 파라미터).
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class mgcoa9000Handler implements TransactionHandler {

    private static final String S0 = "mgcoa9000S0";
    private static final String C0 = "mgcoa9000C0";
    private static final String U0 = "mgcoa9000U0";
    private static final String D0 = "mgcoa9000D0";

    private final mgcoa9000Facade facade;

    public mgcoa9000Handler(mgcoa9000Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(S0, C0, U0, D0);
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case S0 -> facade.mgcoa9000S0(dtoBody);
            case C0 -> facade.mgcoa9000C0(dtoBody);
            case U0 -> facade.mgcoa9000U0(dtoBody);
            case D0 -> facade.mgcoa9000D0(dtoBody);
            default -> throw new ServiceHandlerNotFound(
                    "mgcoa9000Handler 미지원 serviceId: " + context.getServiceId());
        };
    }
}
