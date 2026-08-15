package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina9400Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina9400Handler implements TransactionHandler {
    private final ifina9400Facade facade;
    public ifina9400Handler(ifina9400Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina9400S0", "ifina9400E0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina9400S0" -> facade.ifina9400S0(dtoBody);
            case "ifina9400E0" -> facade.ifina9400E0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina9400 미지원: " + context.getServiceId());
        };
    }
}
