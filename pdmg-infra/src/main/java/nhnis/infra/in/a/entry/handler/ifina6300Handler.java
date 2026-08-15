package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina6300Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina6300Handler implements TransactionHandler {
    private final ifina6300Facade facade;
    public ifina6300Handler(ifina6300Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina6300S0", "ifina6300U0", "ifina6300V0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina6300S0" -> facade.ifina6300S0(dtoBody);
            case "ifina6300U0" -> facade.ifina6300U0(dtoBody);
            case "ifina6300V0" -> facade.ifina6300V0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina6300 미지원: " + context.getServiceId());
        };
    }
}
