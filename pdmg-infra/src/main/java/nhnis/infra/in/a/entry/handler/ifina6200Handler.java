package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina6200Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina6200Handler implements TransactionHandler {
    private final ifina6200Facade facade;
    public ifina6200Handler(ifina6200Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina6200S0", "ifina6200U0", "ifina6200V0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina6200S0" -> facade.ifina6200S0(dtoBody);
            case "ifina6200U0" -> facade.ifina6200U0(dtoBody);
            case "ifina6200V0" -> facade.ifina6200V0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina6200 미지원: " + context.getServiceId());
        };
    }
}
