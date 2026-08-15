package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina6100Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina6100Handler implements TransactionHandler {
    private final ifina6100Facade facade;
    public ifina6100Handler(ifina6100Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina6100S0", "ifina6100U0", "ifina6100V0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina6100S0" -> facade.ifina6100S0(dtoBody);
            case "ifina6100U0" -> facade.ifina6100U0(dtoBody);
            case "ifina6100V0" -> facade.ifina6100V0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina6100 미지원: " + context.getServiceId());
        };
    }
}
