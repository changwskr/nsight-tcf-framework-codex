package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina8100Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina8100Handler implements TransactionHandler {
    private final ifina8100Facade facade;
    public ifina8100Handler(ifina8100Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() {
        return List.of("ifina8100S0", "ifina8100C0", "ifina8100U0", "ifina8100D0");
    }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina8100S0" -> facade.ifina8100S0(dtoBody);
            case "ifina8100C0" -> facade.ifina8100C0(dtoBody);
            case "ifina8100U0" -> facade.ifina8100U0(dtoBody);
            case "ifina8100D0" -> facade.ifina8100D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina8100 미지원: " + context.getServiceId());
        };
    }
}
