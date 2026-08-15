package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina3100Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina3100Handler implements TransactionHandler {
    private final ifina3100Facade facade;

    public ifina3100Handler(ifina3100Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina3100S0", "ifina3100S1", "ifina3100C0", "ifina3100U0", "ifina3100D0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina3100S0" -> facade.ifina3100S0(dtoBody);
            case "ifina3100S1" -> facade.ifina3100S1(dtoBody);
            case "ifina3100C0" -> facade.ifina3100C0(dtoBody);
            case "ifina3100U0" -> facade.ifina3100U0(dtoBody);
            case "ifina3100D0" -> facade.ifina3100D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina3100 미지원: " + context.getServiceId());
        };
    }
}
