package nhnis.fw.commons.txcontrol;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import nhnis.fw.commons.dto.header.hdr_nhnis;
import nhnis.fw.commons.dto.header.sys_comm;
import nhnis.fw.exception.BizException;

/**
 * 시스템 선처리 거래통제 강제.
 *
 * <p>{@code TB_MG_TX_CONTROL} 의 BLOCK_YN=Y 규칙이 매칭되면 {@link BizException}(FW0410).
 * GLOBAL 허용(BLOCK_YN=N)이 켜져 있으면 개별 차단을 무시한다(관리 UI 안내와 동일).
 *
 * <p>호출 위치: TCF ON 시 {@code nhnis.fw.tcf.stf.stf#preProcess}.
 */
@Service
public class MgTxControlService {

    private static final Logger log = LoggerFactory.getLogger(MgTxControlService.class);

    private final MgTxControlRepository repository;

    @Value("${nhnis.fw.txcontrol.enabled:true}")
    private boolean enabled;

    public MgTxControlService(MgTxControlRepository repository) {
        this.repository = repository;
    }

    public void check(hdr_nhnis header) {
        if (!enabled) {
            return;
        }
        MgTxControlRequest req = fromHeader(header);
        if (req == null || MgTxControlExemptions.isExempt(req.getServiceId())) {
            return;
        }

        if (repository.isGlobalUnblockActive()) {
            log.info("[MgTxControl] GLOBAL 허용 활성 — 개별 차단 무시 serviceId={} businessCode={}",
                    req.getServiceId(), req.getBusinessCode());
            return;
        }

        Optional<MgTxControlRule> rule;
        try {
            rule = repository.findBlockingRule(req);
        } catch (Exception e) {
            log.error("[MgTxControl] 조회 실패 — 안전상 차단 serviceId={}", req.getServiceId(), e);
            throw new BizException("FW0411");
        }

        log.info("[MgTxControl] check serviceId={} businessCode={} user={} channel={} branch={} ip={} matched={} type={}",
                req.getServiceId(),
                req.getBusinessCode(),
                req.getUserId(),
                req.getChannelId(),
                req.getBranchId(),
                req.getClientIp(),
                rule.isPresent(),
                rule.map(MgTxControlRule::getControlType).orElse("-"));

        if (rule.isPresent() && rule.get().isBlocking()) {
            log.warn("[MgTxControl] BLOCKED serviceId={} controlType={} businessCode={}",
                    req.getServiceId(), rule.get().getControlType(), req.getBusinessCode());
            throw new BizException("FW0410", rule.get().getControlType(), req.getBusinessCode());
        }
    }

    static MgTxControlRequest fromHeader(hdr_nhnis header) {
        if (header == null || header.getSys_comm() == null) {
            return null;
        }
        sys_comm sys = header.getSys_comm();
        String serviceId = trim(sys.getRms_svc_c());
        String businessCode = MgTxControlRepository.resolveBusinessCode(serviceId);
        return new MgTxControlRequest(
                serviceId,
                "*",
                businessCode,
                "*",
                trim(sys.getOptr_eno()),
                firstNonBlank(sys.getTr_sysid(), sys.getScid()),
                trim(sys.getTr_brc()),
                trim(sys.getTr_trm_ipadr()));
    }

    private static String firstNonBlank(String a, String b) {
        String t = trim(a);
        return t.isEmpty() ? trim(b) : t;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
