package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina4200Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina4200Handler implements TransactionHandler {
    private final ifina4200Facade facade;

    public ifina4200Handler(ifina4200Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina4200S0", "ifina4200C0", "ifina4200U0", "ifina4200D0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina4200S0" -> facade.ifina4200S0(dtoBody);
            case "ifina4200C0" -> facade.ifina4200C0(dtoBody);
            case "ifina4200U0" -> facade.ifina4200U0(dtoBody);
            case "ifina4200D0" -> facade.ifina4200D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina4200 미지원: " + context.getServiceId());
        };
    }
}
