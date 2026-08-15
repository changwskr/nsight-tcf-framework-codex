package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina5300Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina5300Handler implements TransactionHandler {
    private final ifina5300Facade facade;
    public ifina5300Handler(ifina5300Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina5300S0", "ifina5300C0", "ifina5300D0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina5300S0" -> facade.ifina5300S0(dtoBody);
            case "ifina5300C0" -> facade.ifina5300C0(dtoBody);
            case "ifina5300D0" -> facade.ifina5300D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina5300 미지원: " + context.getServiceId());
        };
    }
}
