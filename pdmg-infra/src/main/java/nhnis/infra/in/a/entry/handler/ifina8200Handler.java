package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina8200Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina8200Handler implements TransactionHandler {
    private final ifina8200Facade facade;
    public ifina8200Handler(ifina8200Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina8200S0", "ifina8200U0", "ifina8200V0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina8200S0" -> facade.ifina8200S0(dtoBody);
            case "ifina8200U0" -> facade.ifina8200U0(dtoBody);
            case "ifina8200V0" -> facade.ifina8200V0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina8200 미지원: " + context.getServiceId());
        };
    }
}
