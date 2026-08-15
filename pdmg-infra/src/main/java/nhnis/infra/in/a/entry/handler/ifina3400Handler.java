package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina3400Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina3400Handler implements TransactionHandler {

    private final ifina3400Facade facade;

    public ifina3400Handler(ifina3400Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina3400V0", "ifina3400C0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina3400V0" -> facade.ifina3400V0(dtoBody);
            case "ifina3400C0" -> facade.ifina3400C0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina3400 미지원: " + context.getServiceId());
        };
    }
}
