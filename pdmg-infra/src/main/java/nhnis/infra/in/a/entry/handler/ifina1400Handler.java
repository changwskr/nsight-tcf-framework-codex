package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina1400Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina1400Handler implements TransactionHandler {
    private final ifina1400Facade facade;
    public ifina1400Handler(ifina1400Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() {
        return List.of("ifina1400S0", "ifina1400C0", "ifina1400U0");
    }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina1400S0" -> facade.ifina1400S0(dtoBody);
            case "ifina1400C0" -> facade.ifina1400C0(dtoBody);
            case "ifina1400U0" -> facade.ifina1400U0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina1400 미지원: " + context.getServiceId());
        };
    }
}
