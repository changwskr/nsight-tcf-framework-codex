package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina5100Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina5100Handler implements TransactionHandler {
    private final ifina5100Facade facade;

    public ifina5100Handler(ifina5100Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina5100S0", "ifina5100C0", "ifina5100U0", "ifina5100D0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina5100S0" -> facade.ifina5100S0(dtoBody);
            case "ifina5100C0" -> facade.ifina5100C0(dtoBody);
            case "ifina5100U0" -> facade.ifina5100U0(dtoBody);
            case "ifina5100D0" -> facade.ifina5100D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina5100 미지원: " + context.getServiceId());
        };
    }
}
