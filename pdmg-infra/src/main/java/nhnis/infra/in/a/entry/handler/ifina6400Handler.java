package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina6400Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina6400Handler implements TransactionHandler {
    private final ifina6400Facade facade;
    public ifina6400Handler(ifina6400Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina6400S0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina6400S0" -> facade.ifina6400S0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina6400 미지원: " + context.getServiceId());
        };
    }
}
