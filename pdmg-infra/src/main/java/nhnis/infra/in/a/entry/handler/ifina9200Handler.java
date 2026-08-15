package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina9200Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina9200Handler implements TransactionHandler {
    private final ifina9200Facade facade;
    public ifina9200Handler(ifina9200Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina9200S0", "ifina9200U0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina9200S0" -> facade.ifina9200S0(dtoBody);
            case "ifina9200U0" -> facade.ifina9200U0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina9200 미지원: " + context.getServiceId());
        };
    }
}
