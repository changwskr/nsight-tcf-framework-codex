package nhnis.fw.tcf.core.dispatch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import nhnis.fw.commons.exception.ServiceHandlerNotFound;
import nhnis.fw.tcf.core.context.TransactionContext;
import nhnis.fw.tcf.core.handler.TransactionHandler;

/**
 * serviceId → {@link TransactionHandler} 라우팅.
 *
 * <p>tcf-core TransactionDispatcher 를 PDMG 에 맞게 단순화한 구현이다.
 */
@Component
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class TransactionDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TransactionDispatcher.class);

    private final Map<String, TransactionHandler> handlerMap = new ConcurrentHashMap<>();

    public TransactionDispatcher(List<TransactionHandler> handlers) {
        log.info("========================= [TransactionDispatcher.<init>] 시작");
        try {
            for (TransactionHandler handler : handlers) {
                Collection<String> serviceIds = handler.serviceIds();
                if (serviceIds == null || serviceIds.isEmpty()) {
                    log.warn("Handler declares no serviceId, skipped: {}", handler.getClass().getName());
                    continue;
                }
                for (String serviceId : serviceIds) {
                    TransactionHandler previous = handlerMap.put(serviceId, handler);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate serviceId detected: " + serviceId);
                    }
                    log.info("Registered TCF handler. serviceId={} handler={}",
                            serviceId, handler.getClass().getSimpleName());
                }
            }
            log.info("TCF dispatcher ready. handlers={}", handlerMap.size());
        } finally {
            log.info("========================= [TransactionDispatcher.<init>] 종료");
        }
    }

    public Object dispatch(String serviceId, Object dtoBody, TransactionContext context) throws Exception {
        log.info("========================= [TransactionDispatcher.dispatch] 시작");
        try {
            if (!StringUtils.hasText(serviceId)) {
                throw new ServiceHandlerNotFound("serviceId가 없습니다.");
            }
            TransactionHandler handler = handlerMap.get(serviceId);
            if (handler == null) {
                throw new ServiceHandlerNotFound("등록되지 않은 serviceId입니다: " + serviceId);
            }
            log.info("========================= [TransactionHandler.handle] 시작");
            try {
                return handler.handle(dtoBody, context);
            } finally {
                log.info("========================= [TransactionHandler.handle] 종료");
            }
        } finally {
            log.info("========================= [TransactionDispatcher.dispatch] 종료");
        }
    }

    public Map<String, TransactionHandler> handlers() {
        log.info("========================= [TransactionDispatcher.handlers] 시작");
        try {
            return Map.copyOf(handlerMap);
        } finally {
            log.info("========================= [TransactionDispatcher.handlers] 종료");
        }
    }
}
