package com.nh.nsight.tcf.core.support.processor;

import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.context.TransactionContextHolder;
import com.nh.nsight.tcf.core.support.control.TransactionControlService;
import com.nh.nsight.tcf.core.support.idempotency.IdempotencyChecker;
import com.nh.nsight.tcf.core.support.logging.TransactionLogService;
import com.nh.nsight.tcf.core.support.message.StandardHeader;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.security.AuthenticationContextValidator;
import com.nh.nsight.tcf.core.support.security.AuthorizationValidator;
import com.nh.nsight.tcf.core.support.security.SessionValidator;
import com.nh.nsight.tcf.core.support.TcfConsoleLog;
import com.nh.nsight.tcf.core.support.timeout.TimeoutPolicyService;
import com.nh.nsight.tcf.core.support.validation.StandardHeaderValidator;
import com.nh.nsight.tcf.util.GuidGenerator;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class STF {
    private final StandardHeaderValidator headerValidator;
    private final SessionValidator sessionValidator;
    private final AuthenticationContextValidator authenticationContextValidator;
    private final AuthorizationValidator authorizationValidator;
    private final IdempotencyChecker idempotencyChecker;
    private final TransactionControlService transactionControlService;
    private final TimeoutPolicyService timeoutPolicyService;
    private final TransactionLogService transactionLogService;

    public STF(StandardHeaderValidator headerValidator,
            SessionValidator sessionValidator,
            AuthenticationContextValidator authenticationContextValidator,
            AuthorizationValidator authorizationValidator,
            IdempotencyChecker idempotencyChecker,
            TransactionControlService transactionControlService,
            TimeoutPolicyService timeoutPolicyService,
            TransactionLogService transactionLogService) {
        this.headerValidator = headerValidator;
        this.sessionValidator = sessionValidator;
        this.authenticationContextValidator = authenticationContextValidator;
        this.authorizationValidator = authorizationValidator;
        this.idempotencyChecker = idempotencyChecker;
        this.transactionControlService = transactionControlService;
        this.timeoutPolicyService = timeoutPolicyService;
        this.transactionLogService = transactionLogService;
    }

    public TransactionContext preProcess(StandardRequest<Map<String, Object>> request, StandardHeader clientHeader) {
        System.out.println("=====================================================[STF.preProcess] start");
        StandardHeader header = request.getHeader();
        if (clientHeader == null) {
            clientHeader = StandardHeader.copyOf(header);
        }
        System.out.println(
                " =====================================================[STF.preProcess] headerValidator.validate");
        headerValidator.validate(request);
        if (!StringUtils.hasText(header.getGuid())) {
            header.setGuid(GuidGenerator.newGuid());
        }
        if (!StringUtils.hasText(header.getTraceId())) {
            header.setTraceId(GuidGenerator.newTraceId());
        }
        clientHeader.applyGeneratedCorrelationIdsFrom(header);
        System.out.println(
                " =====================================================[STF.preProcess] guid/traceId assigned");
        TransactionContext context = new TransactionContext(header, clientHeader);
        TransactionContextHolder.set(context);
        putMdc(header);
        System.out.println(
                " =====================================================[STF.preProcess] sessionValidator.validate");
        sessionValidator.validate(header);
        System.out.println(
                " =====================================================[STF.preProcess] authenticationContextValidator.validate");
        authenticationContextValidator.validate(header, context);
        System.out.println(
                " =====================================================[STF.preProcess] authorizationValidator.validate");
        authorizationValidator.validate(header);
        System.out.println(
                " =====================================================[STF.preProcess] transactionControlService.check");
        transactionControlService.check(header);
        System.out.println(
                " =====================================================[STF.preProcess] timeoutPolicyService.resolveAndApply");
        timeoutPolicyService.resolveAndApply(header, context);
        System.out.println(
                " =====================================================[STF.preProcess] idempotencyChecker.checkAndMarkProcessing");
        idempotencyChecker.checkAndMarkProcessing(header);
        System.out.println(
                " =====================================================[STF.preProcess] transactionLogService.start");
        transactionLogService.start(context);
        System.out.println(" =====================================================[STF.preProcess] end");
        return context;
    }

    private void putMdc(StandardHeader header) {
        MDC.put("guid", header.getGuid());
        MDC.put("traceId", header.getTraceId());
        MDC.put("serviceId", header.getServiceId());
        MDC.put("userId", header.getUserId());
        MDC.put("branchId", header.getBranchId());
    }
}
