package nhnis.infra.in.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.infra.in.a.application.facade.ifina1999Facade;

/**
 * ifina1999 도메인 핸들러 (서버 인벤토리 파일럿 CRUD).
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class ifina1999Handler implements TransactionHandler {

    private static final String S0 = "ifina1999S0";
    private static final String C0 = "ifina1999C0";
    private static final String U0 = "ifina1999U0";
    private static final String D0 = "ifina1999D0";
    private static final String E0 = "ifina1999E0";

    private final ifina1999Facade facade;

    public ifina1999Handler(ifina1999Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(S0, C0, U0, D0, E0);
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        return switch (context.getServiceId()) {
            case S0 -> facade.ifina1999S0(dtoBody);
            case C0 -> facade.ifina1999C0(dtoBody);
            case U0 -> facade.ifina1999U0(dtoBody);
            case D0 -> facade.ifina1999D0(dtoBody);
            case E0 -> facade.ifina1999E0(dtoBody);
            default -> throw new ServiceHandlerNotFound(
                    "ifina1999Handler 미지원 serviceId: " + context.getServiceId());
        };
    }
}
