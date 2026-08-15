package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina9100Facade;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina9100Handler implements TransactionHandler {

    private final ifina9100Facade facade;

    public ifina9100Handler(ifina9100Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("ifina9100S0", "ifina9100U0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina9100S0" -> facade.ifina9100S0(dtoBody);
            case "ifina9100U0" -> facade.ifina9100U0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina9100 미지원: " + context.getServiceId());
        };
    }
}
