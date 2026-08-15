package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina2100Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina2100Handler implements TransactionHandler {

    private static final String S0 = "ifina2100S0";
    private static final String C0 = "ifina2100C0";
    private static final String U0 = "ifina2100U0";
    private static final String D0 = "ifina2100D0";

    private final ifina2100Facade facade;

    public ifina2100Handler(ifina2100Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(S0, C0, U0, D0);
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case S0 -> facade.ifina2100S0(dtoBody);
            case C0 -> facade.ifina2100C0(dtoBody);
            case U0 -> facade.ifina2100U0(dtoBody);
            case D0 -> facade.ifina2100D0(dtoBody);
            default -> throw new ServiceHandlerNotFound(
                    "ifina2100Handler 미지원 serviceId: " + context.getServiceId());
        };
    }
}
