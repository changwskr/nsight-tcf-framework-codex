package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina2200Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina2200Handler implements TransactionHandler {

    private final ifina2200Facade facade;

    public ifina2200Handler(ifina2200Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina2200S0", "ifina2200C0", "ifina2200U0", "ifina2200D0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina2200S0" -> facade.ifina2200S0(dtoBody);
            case "ifina2200C0" -> facade.ifina2200C0(dtoBody);
            case "ifina2200U0" -> facade.ifina2200U0(dtoBody);
            case "ifina2200D0" -> facade.ifina2200D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina2200 미지원: " + context.getServiceId());
        };
    }
}
