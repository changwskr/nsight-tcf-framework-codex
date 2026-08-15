package nhnis.infra.in.a.entry.handler;
import java.util.Collection; import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina4100Facade;
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina4100Handler implements TransactionHandler {
    private final ifina4100Facade facade;
    public ifina4100Handler(ifina4100Facade facade) { this.facade = facade; }
    @Override public Collection<String> serviceIds() { return List.of("ifina4100S0", "ifina4100C0", "ifina4100U0", "ifina4100D0"); }
    @Override public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case "ifina4100S0" -> facade.ifina4100S0(dtoBody);
            case "ifina4100C0" -> facade.ifina4100C0(dtoBody);
            case "ifina4100U0" -> facade.ifina4100U0(dtoBody);
            case "ifina4100D0" -> facade.ifina4100D0(dtoBody);
            default -> throw new ServiceHandlerNotFound("ifina4100 미지원: " + context.getServiceId());
        };
    }
}
