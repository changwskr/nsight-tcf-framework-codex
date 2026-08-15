package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina7200Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina7200Handler implements TransactionHandler {
    private final ifina7200Facade facade;
    public ifina7200Handler(ifina7200Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() {
        return List.of("ifina7200S0", "ifina7200U0");
    }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina7200S0" -> facade.ifina7200S0(dtoBody);
            case "ifina7200U0" -> facade.ifina7200U0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina7200 미지원: " + context.getServiceId());
        };
    }
}
