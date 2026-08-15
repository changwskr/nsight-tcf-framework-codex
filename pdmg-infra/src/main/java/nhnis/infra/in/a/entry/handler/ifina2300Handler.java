package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina2300Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina2300Handler implements TransactionHandler {
    private final ifina2300Facade facade;
    public ifina2300Handler(ifina2300Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina2300S0", "ifina2300C0", "ifina2300D0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina2300S0" -> facade.ifina2300S0(dtoBody);
            case "ifina2300C0" -> facade.ifina2300C0(dtoBody);
            case "ifina2300D0" -> facade.ifina2300D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina2300 미지원: " + context.getServiceId());
        };
    }
}
