package nhnis.fw.tcf.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import nhnis.fw.commons.context.ServiceContext;
import nhnis.fw.commons.context.ServiceContextHolder;
import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.fw.tcf.core.facade.TcfFacade;

/**
 * PDMG 단일 온라인 거래 Controller.
 *
 * <p>pdmg-service URL 스타일({@code /mgcoa5530S0})과 tcf-web 스타일({@code /online})을 모두 받는다.
 * 시스템 선후처리는 기존 Filter / Interceptor / ResponseBodyAdvice 가 유지한다.
 *
 * <p>활성 조건: {@code nhnis.fw.tcf.enabled=true}
 */
@RestController
@ConditionalOnProperty(name = "nhnis.fw.tcf.enabled", havingValue = "true")
public class OnlineTransactionController {

    private static final Logger log = LoggerFactory.getLogger(OnlineTransactionController.class);

    private static final String HEADER = "hdr_nhnis";
    private static final String DTO = "dto";
    private static final String SYS_COMM = "sys_comm";
    private static final String RMS_SVC_C = "rms_svc_c";

    private final TcfFacade tcfFacade;

    public OnlineTransactionController(TcfFacade tcfFacade) {
        log.info("========================= [OnlineTransactionController.<init>] 시작");
        try {
            this.tcfFacade = tcfFacade;
        } finally {
            log.info("========================= [OnlineTransactionController.<init>] 종료");
        }
    }

    /** tcf-web 호환: serviceId 는 Header {@code rms_svc_c} 에서 읽는다. */
    @PostMapping("/online")
    public Object handleRoot(@RequestBody Map<String, Object> request, HttpServletRequest servletRequest)
            throws Exception {
        log.info("========================= [OnlineTransactionController.handleRoot] 시작");
        try {
            return handle(null, request, servletRequest);
        } finally {
            log.info("========================= [OnlineTransactionController.handleRoot] 종료");
        }
    }

    /** tcf-web 호환: path businessCode + Header serviceId. */
    @PostMapping("/{businessCode}/online")
    public Object handleWithBusinessCode(
            @PathVariable("businessCode") String businessCode,
            @RequestBody Map<String, Object> request,
            HttpServletRequest servletRequest) throws Exception {
        log.info("========================= [OnlineTransactionController.handleWithBusinessCode] 시작");
        try {
            return handle(businessCode, request, servletRequest);
        } finally {
            log.info("========================= [OnlineTransactionController.handleWithBusinessCode] 종료");
        }
    }

    /**
     * pdmg-service URL 스타일.
     * 예: {@code POST /mgcoa5530S0}
     */
    @PostMapping("/{serviceId}")
    public Object handleByServiceId(
            @PathVariable("serviceId") String serviceId,
            @RequestBody Map<String, Object> request,
            HttpServletRequest servletRequest) throws Exception {
        log.info("========================= [OnlineTransactionController.handleByServiceId] 시작");
        try {
            return handle(serviceId, request, servletRequest);
        } finally {
            log.info("========================= [OnlineTransactionController.handleByServiceId] 종료");
        }
    }

    private Object handle(String pathServiceId, Map<String, Object> request, HttpServletRequest servletRequest)
            throws Exception {
        log.info("========================= [OnlineTransactionController.handle] 시작");
        try {
            enrichClientIp(servletRequest);
            String serviceId = resolveServiceId(pathServiceId, request);
            ensureRmsSvcC(serviceId);
            Object dtoBody = request == null ? null : request.get(DTO);

            log.info("[OnlineTransactionController] serviceId={}", serviceId);
            return tcfFacade.process(serviceId, dtoBody);
        } finally {
            log.info("========================= [OnlineTransactionController.handle] 종료");
        }
    }

    /**
     * serviceId 우선순위: Header(rms_svc_c) → 요청 JSON → path.
     */
    private String resolveServiceId(String pathServiceId, Map<String, Object> request) {
        log.info("========================= [OnlineTransactionController.resolveServiceId] 시작");
        try {
            ServiceContext ctx = ServiceContextHolder.getInstance();
            if (ctx != null && ctx.getHeader() != null && ctx.getHeader().getSys_comm() != null) {
                String fromCtx = ctx.getHeader().getSys_comm().getRms_svc_c();
                if (StringUtils.hasText(fromCtx)) {
                    return fromCtx.trim();
                }
            }

            if (request != null) {
                Object headerObj = request.get(HEADER);
                if (headerObj instanceof Map<?, ?> headerMap) {
                    Object sysObj = headerMap.get(SYS_COMM);
                    if (sysObj instanceof Map<?, ?> sysMap) {
                        Object rms = sysMap.get(RMS_SVC_C);
                        if (rms != null && StringUtils.hasText(String.valueOf(rms))) {
                            return String.valueOf(rms).trim();
                        }
                    }
                }
            }

            if (StringUtils.hasText(pathServiceId)) {
                return pathServiceId.trim();
            }
            return null;
        } finally {
            log.info("========================= [OnlineTransactionController.resolveServiceId] 종료");
        }
    }

    /** path 로 들어온 serviceId 를 Header 에 보정한다. */
    private void ensureRmsSvcC(String serviceId) {
        log.info("========================= [OnlineTransactionController.ensureRmsSvcC] 시작");
        try {
            if (!StringUtils.hasText(serviceId)) {
                return;
            }
            ServiceContext ctx = ServiceContextHolder.getInstance();
            if (ctx == null || ctx.getHeader() == null) {
                return;
            }
            sys_comm sys = ctx.getHeader().getSys_comm();
            if (sys == null) {
                return;
            }
            if (!StringUtils.hasText(sys.getRms_svc_c())) {
                sys.setRms_svc_c(serviceId);
            }
        } finally {
            log.info("========================= [OnlineTransactionController.ensureRmsSvcC] 종료");
        }
    }

    private void enrichClientIp(HttpServletRequest servletRequest) {
        log.info("========================= [OnlineTransactionController.enrichClientIp] 시작");
        try {
            ServiceContext ctx = ServiceContextHolder.getInstance();
            if (ctx == null || ctx.getHeader() == null) {
                return;
            }
            hdr_nhnis header = ctx.getHeader();
            sys_comm sys = header.getSys_comm();
            if (sys == null) {
                return;
            }
            if (StringUtils.hasText(sys.getTr_trm_ipadr())) {
                return;
            }
            sys.setTr_trm_ipadr(resolveClientIp(servletRequest));
        } finally {
            log.info("========================= [OnlineTransactionController.enrichClientIp] 종료");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        log.info("========================= [OnlineTransactionController.resolveClientIp] 시작");
        try {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwardedFor)) {
                return forwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } finally {
            log.info("========================= [OnlineTransactionController.resolveClientIp] 종료");
        }
    }
}
