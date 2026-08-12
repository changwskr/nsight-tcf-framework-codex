package nhnis.mg.jw.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.mg.jw.a.application.facade.mgjwa1000Facade;
import nhnis.mg.jw.a.support.JwtClientContext;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class mgjwa1000Handler implements TransactionHandler {
    private final mgjwa1000Facade facade;

    public mgjwa1000Handler(mgjwa1000Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("mgjwa1000C0", "mgjwa1000C1", "mgjwa1000U0", "mgjwa1000D0", "mgjwa1000D1");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        try {
            JwtClientContext.bind(context);
            return switch (context.getServiceId()) {
                case "mgjwa1000C0" -> facade.mgjwa1000C0(dtoBody);
                case "mgjwa1000C1" -> facade.mgjwa1000C1(dtoBody);
                case "mgjwa1000U0" -> facade.mgjwa1000U0(dtoBody);
                case "mgjwa1000D0" -> facade.mgjwa1000D0(dtoBody);
                case "mgjwa1000D1" -> facade.mgjwa1000D1(dtoBody);
                default -> throw new ServiceHandlerNotFound(
                        "mgjwa1000Handler 미지원 serviceId: " + context.getServiceId());
            };
        } finally {
            JwtClientContext.clear();
        }
    }
}
