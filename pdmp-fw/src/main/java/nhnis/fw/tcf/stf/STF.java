package nhnis.fw.tcf.stf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import nhnis.fw.tcf.TcfMdcKeys;
import nhnis.fw.tcf.TcfTransaction;
import nhnis.fw.tcf.context.TcfContext;
import nhnis.fw.tcf.context.TcfContextHolder;
import nhnis.fw.tcf.dto.StandardHeaderDto;
import nhnis.fw.tcf.dto.StandardRequestDto;
import nhnis.fw.tcf.web.TcfTraceFilter;

/**
 * 시스템 선처리. 전문 Header를 거래가 성립하는 상태로 만들어 놓는다.
 *
 * <p>
 * guid/traceId는 여기서 새로 채번하지 않는다. {@link TcfTraceFilter}가 이미 채번해 MDC에 심어
 * 두었으므로 그 값을 가져와야 HTTP 레벨 로그와 전문 로그가 같은 식별자를 공유한다.
 * 다만 클라이언트가 전문 Header에 guid를 실어 보냈다면 그쪽을 정본으로 삼고 MDC를 덮어쓴다.
 */
@Component
public class STF {

    private static final Logger log = LoggerFactory.getLogger(STF.class);

    private final StandardHeaderValidator headerValidator;

    public STF(StandardHeaderValidator headerValidator) {
        this.headerValidator = headerValidator;
    }

    /**
     * @param request 표준 전문 요청. 전문 봉투를 쓰지 않는 거래는 null이며,
     *                이때 Header는 {@link TcfTransaction} 선언만으로 합성한다.
     */
    public TcfContext preProcess(StandardRequestDto<?> request, TcfTransaction transaction) {
        System.out.println("=========[STF][preProcess][START] "
                + "transactionServiceId=" + transaction.serviceId()
                + " transactionCode=" + transaction.transactionCode()
                + " processingType=" + transaction.processingType());
        boolean clientSuppliedHeader = request != null && request.getHeader() != null;

        StandardHeaderDto header = clientSuppliedHeader ? request.getHeader() : new StandardHeaderDto();
        if (request != null && !clientSuppliedHeader) {
            request.setHeader(header);
        }
        StandardHeaderDto clientHeader = StandardHeaderDto.copyOf(header);

        applyTransactionDefinition(header, transaction);
        header.normalize();
        applyTraceContext(header);
        applyAuthenticatedUser(header);
        clientHeader.applyGeneratedCorrelationIdsFrom(header);

        headerValidator.validate(header, clientSuppliedHeader);
        enrichMdc(header);

        TcfContext context = new TcfContext(transaction, header, clientHeader);
        TcfContextHolder.set(context);

        System.out.println("=========[STF][preProcess][END] "
                + "serviceId=" + header.getServiceId()
                + " transactionCode=" + header.getTransactionCode()
                + " processingType=" + header.getProcessingType()
                + " channelId=" + orDash(header.getChannelId())
                + " userId=" + orDash(header.getUserId())
                + " guid=" + orDash(header.getGuid())
                + " traceId=" + orDash(header.getTraceId())
                + " clientIp=" + orDash(header.getClientIp()));
        log.info("[STF] 거래 시작 serviceId={} transactionCode={} processingType={} channelId={}",
                header.getServiceId(), header.getTransactionCode(), header.getProcessingType(),
                orDash(header.getChannelId()));
        return context;
    }

    /** 클라이언트가 보내지 않은 거래 식별 정보를 @TcfTransaction 선언값으로 채운다. */
    private void applyTransactionDefinition(StandardHeaderDto header, TcfTransaction transaction) {
        if (!StringUtils.hasText(header.getServiceId())) {
            header.setServiceId(transaction.serviceId());
        }
        if (!StringUtils.hasText(header.getTransactionCode())) {
            header.setTransactionCode(transaction.transactionCode());
        }
        if (!StringUtils.hasText(header.getProcessingType())) {
            header.setProcessingType(transaction.processingType().name());
        }
        if (!StringUtils.hasText(header.getServiceName())) {
            header.setServiceName(transaction.serviceName());
        }
        if (!StringUtils.hasText(header.getBusinessCode())) {
            header.setBusinessCode(StringUtils.hasText(transaction.businessCode())
                    ? transaction.businessCode()
                    : businessCodeOf(transaction.serviceId()));
        }
    }

    /** serviceId의 첫 토큰이 업무 코드이다. 예: MP.SalesTip.list → MP */
    private String businessCodeOf(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return null;
        }
        int separator = serviceId.indexOf('.');
        return separator < 0 ? serviceId : serviceId.substring(0, separator);
    }

    private void applyTraceContext(StandardHeaderDto header) {
        if (!StringUtils.hasText(header.getGuid())) {
            header.setGuid(MDC.get(TcfMdcKeys.GUID));
        }
        if (!StringUtils.hasText(header.getTraceId())) {
            header.setTraceId(MDC.get(TcfMdcKeys.TRACE_ID));
        }
        if (!StringUtils.hasText(header.getClientIp())) {
            header.setClientIp(MDC.get(TcfMdcKeys.IP));
        }
    }

    /** 인증은 Security 체인이 끝냈다. 여기서는 그 결과를 전문 Header에 옮기기만 한다. */
    private void applyAuthenticatedUser(StandardHeaderDto header) {
        if (StringUtils.hasText(header.getUserId())) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            header.setUserId(authentication.getName());
        }
    }

    /** 필터가 URI로 채워 둔 MDC를 전문 기준 값으로 덮어쓴다. */
    private void enrichMdc(StandardHeaderDto header) {
        putIfPresent(TcfMdcKeys.GUID, header.getGuid());
        putIfPresent(TcfMdcKeys.TRACE_ID, header.getTraceId());
        putIfPresent(TcfMdcKeys.SERVICE_ID, header.getServiceId());
        putIfPresent(TcfMdcKeys.USER_ID, header.getUserId());
        putIfPresent(TcfMdcKeys.IP, header.getClientIp());
    }

    private void putIfPresent(String key, String value) {
        if (StringUtils.hasText(value)) {
            MDC.put(key, value);
        }
    }

    private String orDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
