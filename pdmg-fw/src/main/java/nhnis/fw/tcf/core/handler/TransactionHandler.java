package nhnis.fw.tcf.core.handler;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nhnis.fw.tcf.core.context.TransactionContext;

/**
 * 온라인 거래 핸들러.
 *
 * <p>serviceId 매핑 방식은 두 가지다.
 * <ul>
 *   <li>단일 거래: {@link #serviceId()} 재정의</li>
 *   <li>도메인 묶음: {@link #serviceIds()} 재정의 후 {@code context.getServiceId()} 로 분기</li>
 * </ul>
 *
 * <p>핸들러는 업무 {@code Facade} 를 호출하고, Service/DAO 는 Facade 뒤에서 수행한다.
 */
public interface TransactionHandler {

    Logger log = LoggerFactory.getLogger(TransactionHandler.class);

    /** 단일 거래 매핑용 serviceId. 도메인 묶음 핸들러는 재정의하지 않아도 된다. */
    default String serviceId() {
        log.info("========================= [TransactionHandler.serviceId] 시작");
        try {
            return null;
        } finally {
            log.info("========================= [TransactionHandler.serviceId] 종료");
        }
    }

    /**
     * 이 핸들러가 처리하는 serviceId 목록.
     * 기본은 {@link #serviceId()} 단일값을 감싼다.
     */
    default Collection<String> serviceIds() {
        log.info("========================= [TransactionHandler.serviceIds] 시작");
        try {
            String id = serviceId();
            return id == null ? List.of() : List.of(id);
        } finally {
            log.info("========================= [TransactionHandler.serviceIds] 종료");
        }
    }

    /**
     * @param dtoBody 요청 전문의 {@code dto} 노드(Map 또는 변환 전 Object)
     * @param context 거래 컨텍스트
     * @return 업무 응답 DTO (시스템 응답 봉투는 ResponseBodyAdvice 가 감싼다)
     */
    Object handle(Object dtoBody, TransactionContext context) throws Exception;
}
