package com.nh.nsight.marketing.om.entry.handler;

import com.nh.nsight.marketing.om.entry.facade.OmAuthFacade;
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
 * OM 인증 도메인 핸들러. OM.Auth.* 거래를 한 핸들러가 처리한다(Service 도메인당 1개).
 */
@Component
public class OmAuthHandler implements TransactionHandler {

    private static final String LOGIN = "OM.Auth.login";
    private static final String SSO_LOGIN = "OM.Auth.ssoLogin";
    private static final String LOGOUT = "OM.Auth.logout";
    private static final String SESSION = "OM.Auth.session";

    private final OmAuthFacade facade;

    public OmAuthHandler(OmAuthFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(LOGIN, SSO_LOGIN, LOGOUT, SESSION);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case LOGIN -> facade.login(request.getBody(), context);
            case SSO_LOGIN -> facade.ssoLogin(request.getBody(), context);
            case LOGOUT -> facade.logout(request.getBody(), context);
            case SESSION -> facade.session(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "OmAuthHandler 미지원 serviceId: " + serviceId);
        };
    }
}
