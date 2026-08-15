package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina1600Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina1600Handler implements TransactionHandler {
    private final ifina1600Facade facade;

    public ifina1600Handler(ifina1600Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina1600S0", "ifina1600C0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina1600S0" -> facade.ifina1600S0(dtoBody);
            case "ifina1600C0" -> facade.ifina1600C0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina1600 미지원: " + context.getServiceId());
        };
    }
}
