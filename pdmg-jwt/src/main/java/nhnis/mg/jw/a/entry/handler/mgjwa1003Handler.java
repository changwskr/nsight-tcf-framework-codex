package nhnis.mg.jw.a.entry.handler;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;
import nhnis.mg.jw.a.application.facade.mgjwa1003Facade;
import nhnis.mg.jw.a.support.JwtClientContext;

@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class mgjwa1003Handler implements TransactionHandler {
    private final mgjwa1003Facade facade;

    public mgjwa1003Handler(mgjwa1003Facade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of("mgjwa1003S0");
    }

    @Override
    public Object handle(Object dtoBody, TransactionContext context) throws Exception {
        try {
            JwtClientContext.bind(context);
            if ("mgjwa1003S0".equals(context.getServiceId())) {
                return facade.mgjwa1003S0(dtoBody);
            }
            throw new ServiceHandlerNotFound("mgjwa1003Handler 미지원 serviceId: " + context.getServiceId());
        } finally {
            JwtClientContext.clear();
        }
    }
}
