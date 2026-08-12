package nhnis.mg.jw.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.mg.jw.a.application.facade.mgjwa1004Facade;
import nhnis.mg.jw.a.support.JwtClientContext;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class mgjwa1004Handler implements TransactionHandler {
    private final mgjwa1004Facade facade;

    public mgjwa1004Handler(mgjwa1004Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("mgjwa1004S0", "mgjwa1004U0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        try {
            JwtClientContext.bind(context);
            return switch (context.getServiceId()) {
                case "mgjwa1004S0" -> facade.mgjwa1004S0(dtoBody);
                case "mgjwa1004U0" -> facade.mgjwa1004U0(dtoBody);
                default -> throw new ServiceHandlerNotFound(
                        "mgjwa1004Handler 미지원 serviceId: " + context.getServiceId());
            };
        } finally {
            JwtClientContext.clear();
        }
    }
}
