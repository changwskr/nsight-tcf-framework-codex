package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina1500Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina1500Handler implements TransactionHandler {
    private final ifina1500Facade facade;

    public ifina1500Handler(ifina1500Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina1500S0", "ifina1500C0", "ifina1500U0", "ifina1500E0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina1500S0" -> facade.ifina1500S0(dtoBody);
            case "ifina1500C0" -> facade.ifina1500C0(dtoBody);
            case "ifina1500U0" -> facade.ifina1500U0(dtoBody);
            case "ifina1500E0" -> facade.ifina1500E0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina1500 미지원: " + context.getServiceId());
        };
    }
}
