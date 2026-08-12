package nhnis.fw.tcf.core.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;

/**
 * 온라인 거래 1건의 처리 컨텍스트.
 *
 * <p>PDMG 시스템 선후처리는 {@link ServiceContext} / Interceptor 가 담당하므로,
 * 여기서는 핸들러·Facade 가 필요한 최소 정보만 담는다.
 */
public class TransactionContext {

    private static final Logger log = LoggerFactory.getLogger(TransactionContext.class);

    private final String serviceId;
    private final ServiceContext serviceContext;
    private final long startedAtNanos;

    public TransactionContext(String serviceId, ServiceContext serviceContext) {
        log.info("========================= [TransactionContext.<init>] 시작");
        try {
            this.serviceId = serviceId;
            this.serviceContext = serviceContext;
            this.startedAtNanos = System.nanoTime();
        } finally {
            log.info("========================= [TransactionContext.<init>] 종료");
        }
    }

    public static TransactionContext fromCurrent(String serviceId) {
        log.info("========================= [TransactionContext.fromCurrent] 시작");
        try {
            return new TransactionContext(serviceId, ServiceContextHolder.getInstance());
        } finally {
            log.info("========================= [TransactionContext.fromCurrent] 종료");
        }
    }

    public String getServiceId() {
        log.info("========================= [TransactionContext.getServiceId] 시작");
        try {
            return serviceId;
        } finally {
            log.info("========================= [TransactionContext.getServiceId] 종료");
        }
    }

    public ServiceContext getServiceContext() {
        log.info("========================= [TransactionContext.getServiceContext] 시작");
        try {
            return serviceContext;
        } finally {
            log.info("========================= [TransactionContext.getServiceContext] 종료");
        }
    }

    public hdr_nhnis getHeader() {
        log.info("========================= [TransactionContext.getHeader] 시작");
        try {
            return serviceContext == null ? null : serviceContext.getHeader();
        } finally {
            log.info("========================= [TransactionContext.getHeader] 종료");
        }
    }

    public String getGuid() {
        log.info("========================= [TransactionContext.getGuid] 시작");
        try {
            return serviceContext == null ? null : serviceContext.getGuid();
        } finally {
            log.info("========================= [TransactionContext.getGuid] 종료");
        }
    }

    public long elapsedMs() {
        log.info("========================= [TransactionContext.elapsedMs] 시작");
        try {
            return (System.nanoTime() - startedAtNanos) / 1_000_000L;
        } finally {
            log.info("========================= [TransactionContext.elapsedMs] 종료");
        }
    }
}
