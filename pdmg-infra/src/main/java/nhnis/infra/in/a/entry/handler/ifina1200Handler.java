package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina1200Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina1200Handler implements TransactionHandler {
    private final ifina1200Facade facade;

    public ifina1200Handler(ifina1200Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina1200S0", "ifina1200C0", "ifina1200U0", "ifina1200D0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina1200S0" -> facade.ifina1200S0(dtoBody);
            case "ifina1200C0" -> facade.ifina1200C0(dtoBody);
            case "ifina1200U0" -> facade.ifina1200U0(dtoBody);
            case "ifina1200D0" -> facade.ifina1200D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina1200 미지원: " + context.getServiceId());
        };
    }
}
