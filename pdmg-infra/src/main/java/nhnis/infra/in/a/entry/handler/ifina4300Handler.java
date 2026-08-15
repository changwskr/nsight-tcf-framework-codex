package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina4300Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina4300Handler implements TransactionHandler {
    private final ifina4300Facade facade;
    public ifina4300Handler(ifina4300Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina4300S0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina4300S0" -> facade.ifina4300S0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina4300 미지원: " + context.getServiceId());
        };
    }
}
