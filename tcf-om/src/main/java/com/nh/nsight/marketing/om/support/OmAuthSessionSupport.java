package com.nh.nsight.marketing.om.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class OmAuthSessionSupport {
    public static final String SESSION_USER = "OM_SESSION_USER";

    public HttpSession createSession(Map<String, Object> user) {
        return createSession(user, null, null);
    }

    public HttpSession createSession(Map<String, Object> user, String loginType, String channelId) {
        HttpServletRequest request = currentRequest()
                .orElseThrow(() -> new IllegalStateException("HTTP 요청 컨텍스트가 없습니다."));
        HttpSession session = request.getSession(true);
        String userId = stringValue(user, "userId");
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, userId);
        session.setAttribute(SESSION_USER, toSessionUser(user, loginType, channelId));
        return session;
    }

    public void invalidateSession() {
        currentRequest().ifPresent(request -> {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        });
    }

    public Optional<OmSessionUser> currentUser() {
        return currentRequest()
                .map(request -> request.getSession(false))
                .map(session -> session.getAttribute(SESSION_USER))
                .filter(OmSessionUser.class::isInstance)
                .map(OmSessionUser.class::cast);
    }

    public Optional<String> currentSessionId() {
        return currentRequest()
                .map(request -> request.getSession(false))
                .map(HttpSession::getId);
    }

    public Map<String, Object> toSessionBody(OmSessionUser user, boolean loggedIn) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("businessCode", "OM");
        body.put("screen", "OM 세션");
        body.put("loggedIn", loggedIn);
        if (loggedIn && user != null) {
            body.put("sessionId", currentSessionId().orElse(null));
            body.put("userId", user.userId());
            body.put("userName", user.userName());
            body.put("branchId", user.branchId());
            body.put("authGroupId", user.authGroupId());
            body.put("authGroupName", user.authGroupName());
            if (user.loginType() != null) {
                body.put("loginType", user.loginType());
            }
            if (user.channelId() != null) {
                body.put("channelId", user.channelId());
            }
        }
        return body;
    }

    private OmSessionUser toSessionUser(Map<String, Object> user, String loginType, String channelId) {
        return new OmSessionUser(
                stringValue(user, "userId"),
                stringValue(user, "userName"),
                stringValue(user, "branchId"),
                stringValue(user, "authGroupId"),
                stringValue(user, "authGroupName"),
                loginType,
                channelId
        );
    }

    private String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Optional<HttpServletRequest> currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return Optional.empty();
        }
        return Optional.ofNullable(attrs.getRequest());
    }

    public record OmSessionUser(
            String userId,
            String userName,
            String branchId,
            String authGroupId,
            String authGroupName,
            String loginType,
            String channelId
    ) implements Serializable {
    }
}
