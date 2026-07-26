package com.nh.nsight.marketing.om.entry.handler;

import com.nh.nsight.marketing.om.entry.facade.OmDeployFacade;
import com.nh.nsight.tcf.core.support.context.TransactionContext;
import com.nh.nsight.tcf.core.support.error.BusinessException;
import com.nh.nsight.tcf.core.support.error.ErrorCode;
import com.nh.nsight.tcf.core.support.message.StandardRequest;
import com.nh.nsight.tcf.core.support.transaction.TransactionHandler;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * OM 배포 도메인 핸들러. OM.Deploy.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class OmDeployHandler implements TransactionHandler {

    private static final String HISTORY = "OM.Deploy.history";
    private static final String BUILD_STATUS = "OM.Deploy.buildStatus";
    private static final String LOG_INQUIRY = "OM.Deploy.logInquiry";
    private static final String HEALTH_CHECK = "OM.Deploy.healthCheck";
    private static final String DELETE_ALL = "OM.Deploy.deleteAll";
    private static final String EXECUTE = "OM.Deploy.execute";
    private static final String BUILD_REQUEST = "OM.Deploy.buildRequest";
    private static final String DEPLOY_REQUEST = "OM.Deploy.deployRequest";
    private static final String ROLLBACK_REQUEST = "OM.Deploy.rollbackRequest";
    private static final String APPROVE = "OM.Deploy.approve";

    private final OmDeployFacade facade;

    public OmDeployHandler(OmDeployFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(HISTORY, BUILD_STATUS, LOG_INQUIRY, HEALTH_CHECK, DELETE_ALL,
                EXECUTE, BUILD_REQUEST, DEPLOY_REQUEST, ROLLBACK_REQUEST, APPROVE);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case HISTORY -> facade.history(request.getBody(), context);
            case BUILD_STATUS -> facade.buildStatus(request.getBody(), context);
            case LOG_INQUIRY -> facade.logInquiry(request.getBody(), context);
            case HEALTH_CHECK -> facade.healthCheck(request.getBody(), context);
            case DELETE_ALL -> facade.deleteAll(request.getBody(), context);
            case EXECUTE -> facade.execute(request.getBody(), context);
            case BUILD_REQUEST -> facade.buildRequest(request.getBody(), context);
            case DEPLOY_REQUEST -> facade.deployRequest(request.getBody(), context);
            case ROLLBACK_REQUEST -> facade.rollbackRequest(request.getBody(), context);
            case APPROVE -> facade.approve(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "OmDeployHandler 미지원 serviceId: " + serviceId);
        };
    }
}
