package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina1300Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina1300Handler implements TransactionHandler {
    private final ifina1300Facade facade;
    public ifina1300Handler(ifina1300Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() {
        return List.of("ifina1300S0", "ifina1300C0", "ifina1300U0");
    }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina1300S0" -> facade.ifina1300S0(dtoBody);
            case "ifina1300C0" -> facade.ifina1300C0(dtoBody);
            case "ifina1300U0" -> facade.ifina1300U0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina1300 미지원: " + context.getServiceId());
        };
    }
}
