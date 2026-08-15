package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina7100Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina7100Handler implements TransactionHandler {
    private final ifina7100Facade facade;
    public ifina7100Handler(ifina7100Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() {
        return List.of("ifina7100S0", "ifina7100C0", "ifina7100U0", "ifina7100D0");
    }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina7100S0" -> facade.ifina7100S0(dtoBody);
            case "ifina7100C0" -> facade.ifina7100C0(dtoBody);
            case "ifina7100U0" -> facade.ifina7100U0(dtoBody);
            case "ifina7100D0" -> facade.ifina7100D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina7100 미지원: " + context.getServiceId());
        };
    }
}
